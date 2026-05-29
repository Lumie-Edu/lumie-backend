package com.lumie.exam.domain.vo;

public enum Priority {
    HIGH(1),    // 정확도 50% 미만 유형 (즉각 개선 필요)
    MEDIUM(2),  // 정확도 50~70% 유형 (집중 개선 권장)
    LOW(3);     // 정확도 70% 이상 유형 (유지 수준)

    // 정렬 우선순위. ordinal() 대신 명시적 값을 둬 상수 추가/재배치 시에도 정렬이 깨지지 않게 한다.
    private final int order;

    Priority(int order) {
        this.order = order;
    }

    public int order() {
        return order;
    }
}
