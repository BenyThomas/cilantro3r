package com.config;

import com.entities.PensionTransaction;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.item.ItemProcessor;

public class TransactionItemProcessor implements ItemProcessor<PensionTransaction, PensionTransaction> {
    @Override
    public PensionTransaction process(final @NotNull PensionTransaction txn) throws Exception {
        return txn;
    }
}
