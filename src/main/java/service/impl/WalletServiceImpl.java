package service.impl;

import dto.CreateWalletRequest;
import dto.TransactionRequest;
import dto.TransactionResponse;
import dto.WalletResponse;
import entity.Wallet;
import entity.WalletTransaction;
import exception.InsufficientBalanceException;
import exception.WalletNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import repository.WalletRepository;
import repository.WalletTransactionRepository;
import service.WalletService;

import java.math.BigDecimal;


@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    private static final int MAX_RETRY_ATTEMPTS = 3;

    @Override
    @Transactional
    public WalletResponse createWallet(CreateWalletRequest request) {
        log.info("Creating wallet for user: {}", request.getUserId());

        if (walletRepository.existsByUserId(request.getUserId())) {
            throw new DataIntegrityViolationException("Wallet already exists for user: " + request.getUserId());
        }

        Wallet wallet = Wallet.builder()
                .userId(request.getUserId())
                .balance(BigDecimal.ZERO)
                .currency(request.getCurrency())
                .status(Wallet.WalletStatus.ACTIVE)
                .version(0L)
                .build();

        Wallet savedWallet = walletRepository.save(wallet);
        log.info("Wallet created successfully: ID={}", savedWallet.getId());

        return WalletResponse.fromEntity(savedWallet);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWalletBalance(Long walletId) {
        log.debug("Fetching wallet balance: ID={}", walletId);

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        return WalletResponse.fromEntity(wallet);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse creditWallet(Long walletId, TransactionRequest request) {
        return executeWithRetry(() -> processCreditTransaction(walletId, request));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse debitWallet(Long walletId, TransactionRequest request) {
        return executeWithRetry(() -> processDebitTransaction(walletId, request));
    }

    private TransactionResponse processCreditTransaction(Long walletId, TransactionRequest request) {
        log.info("Processing credit transaction: walletId={}, referenceId={}", walletId, request.getReferenceId());

        // Idempotency check
        WalletTransaction existingTransaction = transactionRepository
                .findByReferenceId(request.getReferenceId())
                .orElse(null);

        if (existingTransaction != null) {
            log.warn("Duplicate transaction detected: referenceId={}", request.getReferenceId());
            return TransactionResponse.fromEntity(existingTransaction);
        }

        Wallet wallet = walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        if (wallet.getStatus() != Wallet.WalletStatus.ACTIVE) {
            throw new IllegalStateException("Wallet is not active");
        }

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.credit(request.getAmount());
        BigDecimal balanceAfter = wallet.getBalance();

        // Save wallet (version will be automatically incremented)
        walletRepository.save(wallet);

        // Create transaction record
        WalletTransaction transaction = WalletTransaction.builder()
                .walletId(walletId)
                .referenceId(request.getReferenceId())
                .transactionType(WalletTransaction.TransactionType.CREDIT)
                .amount(request.getAmount())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .currency(wallet.getCurrency())
                .description(request.getDescription())
                .status(WalletTransaction.TransactionStatus.SUCCESS)
                .build();

        WalletTransaction savedTransaction = transactionRepository.save(transaction);
        log.info("Credit transaction completed: transactionId={}, newBalance={}",
                savedTransaction.getId(), balanceAfter);

        return TransactionResponse.fromEntity(savedTransaction);
    }

    private TransactionResponse processDebitTransaction(Long walletId, TransactionRequest request) {
        log.info("Processing debit transaction: walletId={}, referenceId={}", walletId, request.getReferenceId());

        // Idempotency check
        WalletTransaction existingTransaction = transactionRepository
                .findByReferenceId(request.getReferenceId())
                .orElse(null);

        if (existingTransaction != null) {
            log.warn("Duplicate transaction detected: referenceId={}", request.getReferenceId());
            return TransactionResponse.fromEntity(existingTransaction);
        }

        // Fetch wallet with optimistic lock
        Wallet wallet = walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        if (wallet.getStatus() != Wallet.WalletStatus.ACTIVE) {
            throw new IllegalStateException("Wallet is not active");
        }

        BigDecimal balanceBefore = wallet.getBalance();

        // Check sufficient balance
        if (balanceBefore.compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance. Available: " + balanceBefore);
        }

        wallet.debit(request.getAmount());
        BigDecimal balanceAfter = wallet.getBalance();

        // Save wallet
        walletRepository.save(wallet);

        // Create transaction record
        WalletTransaction transaction = WalletTransaction.builder()
                .walletId(walletId)
                .referenceId(request.getReferenceId())
                .transactionType(WalletTransaction.TransactionType.DEBIT)
                .amount(request.getAmount())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .currency(wallet.getCurrency())
                .description(request.getDescription())
                .status(WalletTransaction.TransactionStatus.SUCCESS)
                .build();

        WalletTransaction savedTransaction = transactionRepository.save(transaction);
        log.info("Debit transaction completed: transactionId={}, newBalance={}",
                savedTransaction.getId(), balanceAfter);

        return TransactionResponse.fromEntity(savedTransaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistory(Long walletId, Pageable pageable) {
        log.debug("Fetching transaction history: walletId={}", walletId);

        // Verify wallet exists
        if (!walletRepository.existsById(walletId)) {
            throw new WalletNotFoundException("Wallet not found: " + walletId);
        }

        Page<WalletTransaction> transactions = transactionRepository
                .findByWalletIdOrderByCreatedAtDesc(walletId, pageable);

        return transactions.map(TransactionResponse::fromEntity);
    }

    private TransactionResponse executeWithRetry(TransactionExecutor executor) {
        int attempt = 0;
        while (attempt < MAX_RETRY_ATTEMPTS) {
            try {
                return executor.execute();
            } catch (ObjectOptimisticLockingFailureException e) {
                attempt++;
                if (attempt >= MAX_RETRY_ATTEMPTS) {
                    log.error("Transaction failed after {} attempts due to concurrent modification", attempt);
                    throw new RuntimeException("Transaction failed due to concurrent updates. Please retry.", e);
                }

                log.warn("Optimistic locking failure, retrying attempt {}/{}", attempt, MAX_RETRY_ATTEMPTS);

                // Exponential backoff: 50ms, 100ms, 200ms
                try {
                    Thread.sleep(50L * (1L << (attempt - 1)));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Transaction interrupted", ie);
                }
            }
        }
        throw new RuntimeException("Transaction failed");
    }

    @FunctionalInterface
    private interface TransactionExecutor {
        TransactionResponse execute();
    }
}