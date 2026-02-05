package com.lumie.spreadsheet.domain.vo;

import java.io.Serializable;

public record CellStyle(
        String backgroundColor,
        String textColor,
        String fontFamily,
        Integer fontSize,
        Boolean bold,
        Boolean italic,
        Boolean underline,
        String horizontalAlign,
        String verticalAlign,
        String numberFormat
) implements Serializable {

    public static CellStyle defaultStyle() {
        return new CellStyle(
                null,
                null,
                null,
                null,
                false,
                false,
                false,
                "left",
                "middle",
                null
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String backgroundColor;
        private String textColor;
        private String fontFamily;
        private Integer fontSize;
        private Boolean bold = false;
        private Boolean italic = false;
        private Boolean underline = false;
        private String horizontalAlign = "left";
        private String verticalAlign = "middle";
        private String numberFormat;

        public Builder backgroundColor(String backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public Builder textColor(String textColor) {
            this.textColor = textColor;
            return this;
        }

        public Builder fontFamily(String fontFamily) {
            this.fontFamily = fontFamily;
            return this;
        }

        public Builder fontSize(Integer fontSize) {
            this.fontSize = fontSize;
            return this;
        }

        public Builder bold(Boolean bold) {
            this.bold = bold;
            return this;
        }

        public Builder italic(Boolean italic) {
            this.italic = italic;
            return this;
        }

        public Builder underline(Boolean underline) {
            this.underline = underline;
            return this;
        }

        public Builder horizontalAlign(String horizontalAlign) {
            this.horizontalAlign = horizontalAlign;
            return this;
        }

        public Builder verticalAlign(String verticalAlign) {
            this.verticalAlign = verticalAlign;
            return this;
        }

        public Builder numberFormat(String numberFormat) {
            this.numberFormat = numberFormat;
            return this;
        }

        public CellStyle build() {
            return new CellStyle(
                    backgroundColor,
                    textColor,
                    fontFamily,
                    fontSize,
                    bold,
                    italic,
                    underline,
                    horizontalAlign,
                    verticalAlign,
                    numberFormat
            );
        }
    }
}
