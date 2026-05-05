package com.example.vouchersystem.service.alert;

public interface AlertService {
    void sendCriticalAlert(String subject, String detail);
}
