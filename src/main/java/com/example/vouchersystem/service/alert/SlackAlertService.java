package com.example.vouchersystem.service.alert;

import com.example.vouchersystem.domain.dto.slack.SlackMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackAlertService implements AlertService{
    private final WebClient slackWebClient;

    @Value("${app.env:}")
    private String environment;

    @Value("${alert.slack.enabled:}")
    private boolean isEnabled;

    @Value("${alert.slack.webhook-url:}")
    private String webhookUrl;

    @Value("${alert.slack.throttle-interval-ms:}")
    private long throttleIntervalMs;

    private final ConcurrentHashMap<String, Long> lastAlertTimeMap = new ConcurrentHashMap<>();

    @Override
    public void sendCriticalAlert(String subject, String details){
        if(!isEnabled || webhookUrl == null || webhookUrl.isBlank()){
            log.warn("Slack alert is disabled or webhook URL is not configured. Skipping alert: {}",
                subject);
            return;
        }

        long currentTime = System.currentTimeMillis();
        AtomicBoolean isAllowedToSend = new AtomicBoolean(false);

        lastAlertTimeMap.compute(subject, (key, lastSendTime) -> {
            if(lastSendTime == null || (currentTime - lastSendTime) >= throttleIntervalMs){
                isAllowedToSend.set(true);
                return currentTime;
            }
            return lastSendTime;
        });

        if(!isAllowedToSend.get()){
            log.debug("Skipping alert: {}. Throttle interval not reached yet.", subject);
            return;
        }

        try{
            String title = String.format("🚨 [%s] CRITICAL ALERT: %s",
                    environment.toUpperCase(), subject);
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            SlackMessage payload = new SlackMessage(List.of(
                    new SlackMessage.Attachment(
                            title,
                            "FF0000",
                            title,
                            details,
                            "Voucher System | " + timestamp
                    )
            ));

            slackWebClient.post()
                    .uri(webhookUrl)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(response ->
                            log.info("Critical alert sent to Slack successfully for: {}.",
                                    subject))
                    .doOnError(error -> {
                        log.error("Failed to send alert to Slack for subject: {}",
                            subject, error);
                        lastAlertTimeMap.remove(subject);
                    })
                    .subscribe();
        }catch (Exception e){
            log.error("Failed to compose or execute Slack alert request", e);
        }
    }
}
