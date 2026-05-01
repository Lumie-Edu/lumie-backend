package com.lumie.exam.domain.service;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StatisticsCalculator - 변별도/Kelley 표본 단위 테스트")
class StatisticsCalculatorItemAnalysisTest {

    private final StatisticsCalculator calculator = new StatisticsCalculator();

    @Test
    @DisplayName("Kelley 표본 크기는 전체의 27% (올림), 최소 1을 보장한다")
    void kelleySampleSize_basicCases() {
        assertThat(calculator.kelleySampleSize(0)).isZero();
        assertThat(calculator.kelleySampleSize(1)).isEqualTo(1);
        assertThat(calculator.kelleySampleSize(2)).isEqualTo(1);
        assertThat(calculator.kelleySampleSize(10)).isEqualTo(3); // ceil(2.7)=3
        assertThat(calculator.kelleySampleSize(100)).isEqualTo(27);
    }

    @Test
    @DisplayName("표본 크기는 전체의 절반을 넘지 않는다 (top/bottom 겹침 방지)")
    void kelleySampleSize_capsAtHalf() {
        assertThat(calculator.kelleySampleSize(3)).isEqualTo(1); // half=1
    }

    @Test
    @DisplayName("변별도 = 상위군 정답률 - 하위군 정답률")
    void calculateDiscrimination_basic() {
        // top 10명 중 9명 정답, bottom 10명 중 3명 정답 → 0.9 - 0.3 = 0.6
        double d = calculator.calculateDiscrimination(9, 10, 3, 10);
        assertThat(d).isCloseTo(0.6, Offset.offset(1e-9));
    }

    @Test
    @DisplayName("두 그룹 모두 전원 정답이면 변별도는 0 (변별 불가)")
    void calculateDiscrimination_allCorrect() {
        double d = calculator.calculateDiscrimination(5, 5, 5, 5);
        assertThat(d).isZero();
    }

    @Test
    @DisplayName("표본 크기가 0이면 변별도는 0")
    void calculateDiscrimination_emptySample() {
        assertThat(calculator.calculateDiscrimination(0, 0, 0, 5)).isZero();
        assertThat(calculator.calculateDiscrimination(5, 5, 0, 0)).isZero();
    }

    @Test
    @DisplayName("음의 변별도 — 하위군이 상위군보다 잘 맞히면 폐기 후보")
    void calculateDiscrimination_negative() {
        double d = calculator.calculateDiscrimination(2, 10, 8, 10);
        assertThat(d).isCloseTo(-0.6, Offset.offset(1e-9));
    }
}
