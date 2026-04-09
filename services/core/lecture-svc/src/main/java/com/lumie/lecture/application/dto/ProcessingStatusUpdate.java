package com.lumie.lecture.application.dto;

public record ProcessingStatusUpdate(
        String status,
        Integer progress
) {}
