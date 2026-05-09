package com.gc2026.portfolio.service;

import com.gc2026.portfolio.domain.entity.Transaction;
import com.gc2026.portfolio.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public String exportToCsv(Long userId) {
        List<Transaction> transactions = transactionRepository.findAllByUserIdAndIsDeletedFalseOrderByTxDateDesc(userId);

        StringBuilder csv = new StringBuilder();
        // Header
        csv.append("Date,Title,Type,Category,Amount(centimes),Description\n");

        // Rows
        for (Transaction t : transactions) {
            csv.append(escapeCsv(t.getTxDate() != null ? t.getTxDate().toString() : "")).append(",")
               .append(escapeCsv(t.getTitle())).append(",")
               .append(escapeCsv(t.getType() != null ? t.getType().name() : "")).append(",")
               .append(escapeCsv(t.getCategory() != null ? t.getCategory().getName() : "")).append(",")
               .append(t.getAmount()).append(",")
               .append(escapeCsv(t.getDescription() != null ? t.getDescription() : ""))
               .append("\n");
        }

        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
