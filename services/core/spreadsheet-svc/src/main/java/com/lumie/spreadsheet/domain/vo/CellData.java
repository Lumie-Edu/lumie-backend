package com.lumie.spreadsheet.domain.vo;

import java.io.Serializable;

public record CellData(
        String value,
        String displayValue,
        String formula,
        CellStyle style
) implements Serializable {

    public static CellData empty() {
        return new CellData(null, null, null, null);
    }

    public static CellData withValue(String value) {
        return new CellData(value, value, null, null);
    }

    public static CellData withFormula(String formula, String displayValue) {
        return new CellData(null, displayValue, formula, null);
    }

    public CellData withStyle(CellStyle style) {
        return new CellData(this.value, this.displayValue, this.formula, style);
    }

    public boolean isEmpty() {
        return (value == null || value.isBlank()) &&
               (formula == null || formula.isBlank());
    }
}
