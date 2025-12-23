package controller;

import dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.WalletService;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Slf4j
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<ApiResponse<WalletResponse>> createWallet(
            @Valid @RequestBody CreateWalletRequest request) {
        log.info("Received request to create wallet for user: {}", request.getUserId());

        WalletResponse response = walletService.createWallet(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Wallet created successfully", response));
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<ApiResponse<WalletResponse>> getWalletBalance(
            @PathVariable Long walletId) {
        log.info("Received request to get wallet balance: walletId={}", walletId);

        WalletResponse response = walletService.getWalletBalance(walletId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{walletId}/credit")
    public ResponseEntity<ApiResponse<TransactionResponse>> creditWallet(
            @PathVariable Long walletId,
            @Valid @RequestBody TransactionRequest request) {
        log.info("Received request to credit wallet: walletId={}, referenceId={}",
                walletId, request.getReferenceId());

        TransactionResponse response = walletService.creditWallet(walletId, request);

        return ResponseEntity.ok(ApiResponse.success("Credit successful", response));
    }@PostMapping("/{walletId}/debit")
    public ResponseEntity<ApiResponse<TransactionResponse>> debitWallet(
            @PathVariable Long walletId,
            @Valid @RequestBody TransactionRequest request) {
        log.info("Received request to debit wallet: walletId={}, referenceId={}",
                walletId, request.getReferenceId());

        TransactionResponse response = walletService.debitWallet(walletId, request);

        return ResponseEntity.ok(ApiResponse.success("Debit successful", response));
    }

    @GetMapping("/{walletId}/transactions")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactionHistory(
            @PathVariable Long walletId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Received request to get transaction history: walletId={}, page={}, size={}",
                walletId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionResponse> response = walletService.getTransactionHistory(walletId, pageable);

        return ResponseEntity.ok(ApiResponse.success(response));
    }



}
