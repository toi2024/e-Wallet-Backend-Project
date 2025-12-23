package service;



import dto.CreateWalletRequest;
import dto.TransactionRequest;
import dto.TransactionResponse;
import dto.WalletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WalletService {

    WalletResponse createWallet(CreateWalletRequest request);

    WalletResponse getWalletBalance(Long walletId);

    TransactionResponse creditWallet(Long walletId, TransactionRequest request);

    TransactionResponse debitWallet(Long debitId, TransactionRequest request);

    Page<TransactionResponse> getTransactionHistory(Long walletId, Pageable pageable);

}
