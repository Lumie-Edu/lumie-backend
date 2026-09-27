package com.lumie.file.api;

/** Browser-uploaded temporary MinIO object. It has no persistent DB row. */
public record TemporaryAttachment(String objectKey, String filename, String contentType, long fileSize, String uploadToken) {}
