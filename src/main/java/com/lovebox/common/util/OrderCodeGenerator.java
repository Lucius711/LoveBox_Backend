package com.lovebox.common.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class OrderCodeGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("ddMMyy");
    private final AtomicInteger sequence = new AtomicInteger(0);

    /**
     * Generates order code in format: LB{DDMMYY}-{SEQUENCE:04d}
     * Example: LB261018-0001
     */
    public String generate(long currentOrderCount) {
        String datePart = LocalDate.now().format(DATE_FORMAT);
        long nextSeq = currentOrderCount + 1;
        return String.format("LB%s-%04d", datePart, nextSeq);
    }
}
