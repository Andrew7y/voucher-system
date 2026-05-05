package com.example.vouchersystem.domain.dto.slack;

import java.util.List;

public record SlackMessage(List<Attachment> attachments){
    public record Attachment(
            String fallback,
            String color,
            String title,
            String text,
            String footer
    ){}
}

