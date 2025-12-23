package dto;

import entity.WalletTransaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private Long transactionId;
    private String referenceId;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String currency;
    private String description;
    private String status;
    private LocalDateTime createdAt;

    public static TransactionResponse fromEntity(WalletTransaction transaction) {
        return TransactionResponse.builder()
                .transactionId(transaction.getId())
                .referenceId(transaction.getReferenceId())
                .transactionType(transaction.getTransactionType().name())
                .amount(transaction.getAmount())
                .balanceBefore(transaction.getBalanceBefore())
                .balanceAfter(transaction.getBalanceAfter())
                .currency(transaction.getCurrency())
                .description(transaction.getDescription())
                .status(transaction.getStatus().name())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
