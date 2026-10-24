package com.lumie.common.exception;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Safe Korean fallbacks for non-business HTTP failures. */
final class UserFacingErrorMessages {

    private static final Pattern LATIN_WORD = Pattern.compile("\\b[A-Za-z]{2,}\\b");
    private static final Set<String> ACCEPTED_TERMS = Set.of(
            "AI", "API", "CSV", "ID", "JSON", "JWT", "OMR", "PG", "QNA", "SMS", "URL", "ZIP");

    private UserFacingErrorMessages() {}

    static String forStatus(int status) {
        return switch (status) {
            case 400, 422 -> "입력한 내용을 확인해 주세요.";
            case 401 -> "로그인이 필요합니다. 다시 로그인해 주세요.";
            case 403 -> "이 작업을 수행할 권한이 없습니다.";
            case 404 -> "요청한 항목을 찾을 수 없습니다.";
            case 409 -> "현재 상태에서는 요청을 처리할 수 없습니다.";
            case 410 -> "요청한 자료가 만료되었습니다.";
            case 429 -> "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.";
            case 502, 503, 504 -> "외부 서비스와 통신하지 못했습니다. 잠시 후 다시 시도해 주세요.";
            default -> status >= 500
                    ? "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."
                    : "요청을 처리할 수 없습니다.";
        };
    }

    static String preserveOrLocalize(String message, int status) {
        return isKoreanMessage(message) ? message : forStatus(status);
    }

    static String forBusinessError(String message, String codeMessage, int status) {
        if (isKoreanMessage(message)) return message;
        if (isKoreanMessage(codeMessage)) return codeMessage;
        return forStatus(status);
    }

    private static boolean isKoreanMessage(String message) {
        if (message == null || message.codePoints().noneMatch(cp -> cp >= 0xAC00 && cp <= 0xD7A3)) {
            return false;
        }
        Matcher matcher = LATIN_WORD.matcher(message);
        while (matcher.find()) {
            if (!ACCEPTED_TERMS.contains(matcher.group().toUpperCase(Locale.ROOT))) return false;
        }
        return true;
    }
}
