package com.lumie.common.csv;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvWriterTest {

    @Test
    void writesBomAndHeaderAndRows() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CsvWriter csv = new CsvWriter(out)) {
            csv.writeHeader(List.of("이름", "점수"));
            csv.writeRow(List.of("홍길동", "95"));
        }
        byte[] bytes = out.toByteArray();
        assertThat(bytes).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        String body = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        assertThat(body).isEqualTo("이름,점수\r\n홍길동,95\r\n");
    }

    @Test
    void escapesCommasQuotesAndNewlines() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CsvWriter csv = new CsvWriter(out)) {
            csv.writeHeader(List.of("a", "b", "c"));
            csv.writeRow(List.of("hello, world", "say \"hi\"", "line1\nline2"));
        }
        byte[] bytes = out.toByteArray();
        String body = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        assertThat(body).contains("\"hello, world\"");
        assertThat(body).contains("\"say \"\"hi\"\"\"");
        assertThat(body).contains("\"line1\nline2\"");
    }

    @Test
    void emptyValueRendersAsBlank() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CsvWriter csv = new CsvWriter(out)) {
            csv.writeHeader(List.of("a", "b"));
            List<String> row = java.util.Arrays.asList(null, "x");
            csv.writeRow(row);
        }
        byte[] bytes = out.toByteArray();
        String body = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        assertThat(body).isEqualTo("a,b\r\n,x\r\n");
    }
}
