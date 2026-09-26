package com.lovebox.common.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Chạy job sau khi transaction hiện tại commit (rollback → không chạy); ngoài transaction → chạy ngay. */
public final class AfterCommit {
    private AfterCommit() {}

    public static void run(Runnable job) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) { job.run(); return; }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { job.run(); }
        });
    }
}
