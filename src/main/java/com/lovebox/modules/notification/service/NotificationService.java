package com.lovebox.modules.notification.service;

import java.util.UUID;

public interface NotificationService {
    void notifyNewOrder(UUID orderId);
    void notifyReceiptUploaded(UUID orderId);
}
