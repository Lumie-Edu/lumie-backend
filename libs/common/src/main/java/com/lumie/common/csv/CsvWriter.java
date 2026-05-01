package com.lumie.common.csv;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * UTF-8 BOM prefixed CSV writer (Excel-compatible Korean output).
 * Caller owns the OutputStream lifecycle; this writer flushes but does not close.
 */
public final class CsvWriter implements AutoCloseable {

    private static final char BOM = '﻿';
    private static final String LINE_ENDING = "\r\n";

    private final BufferedWriter writer;
    private boolean headerWritten;

    public CsvWriter(OutputStream out) {
        this.writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        try {
            writer.write(BOM);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeHeader(List<String> headers) {
        if (headerWritten) {
            throw new IllegalStateException("header already written");
        }
        writeRow(headers);
        headerWritten = true;
    }

    public void writeRow(List<String> values) {
        try {
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) writer.write(',');
                writer.write(escape(values.get(i)));
            }
            writer.write(LINE_ENDING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void flush() {
        try {
            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        flush();
    }

    private static String escape(String value) {
        if (value == null) return "";
        boolean needsQuote = value.indexOf(',') >= 0
            || value.indexOf('"') >= 0
            || value.indexOf('\n') >= 0
            || value.indexOf('\r') >= 0;
        if (!needsQuote) return value;
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
