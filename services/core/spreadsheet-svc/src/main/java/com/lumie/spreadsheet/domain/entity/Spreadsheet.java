package com.lumie.spreadsheet.domain.entity;

import com.lumie.common.domain.BaseEntity;
import com.lumie.spreadsheet.domain.vo.CellData;
import com.lumie.spreadsheet.domain.vo.SpreadsheetPermission;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "spreadsheets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Spreadsheet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "row_count", nullable = false)
    private Integer rowCount = 100;

    @Column(name = "column_count", nullable = false)
    private Integer columnCount = 26;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "column_widths", columnDefinition = "jsonb")
    private Map<String, Integer> columnWidths = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "row_heights", columnDefinition = "jsonb")
    private Map<String, Integer> rowHeights = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cells", columnDefinition = "jsonb")
    private Map<String, CellData> cells = new HashMap<>();

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, length = 20)
    private SpreadsheetPermission permission = SpreadsheetPermission.PRIVATE;

    @Builder
    private Spreadsheet(String name, String description, Integer rowCount, Integer columnCount,
                        Long ownerId, SpreadsheetPermission permission) {
        this.name = name;
        this.description = description;
        this.rowCount = rowCount != null ? rowCount : 100;
        this.columnCount = columnCount != null ? columnCount : 26;
        this.ownerId = ownerId;
        this.permission = permission != null ? permission : SpreadsheetPermission.PRIVATE;
        this.cells = new HashMap<>();
        this.columnWidths = new HashMap<>();
        this.rowHeights = new HashMap<>();
    }

    public static Spreadsheet create(String name, Long ownerId) {
        return Spreadsheet.builder()
                .name(name)
                .ownerId(ownerId)
                .build();
    }

    public static Spreadsheet create(String name, String description, Long ownerId) {
        return Spreadsheet.builder()
                .name(name)
                .description(description)
                .ownerId(ownerId)
                .build();
    }

    public void update(String name, String description, SpreadsheetPermission permission) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
        if (permission != null) {
            this.permission = permission;
        }
    }

    public void updateCell(String address, CellData data) {
        if (data == null || data.isEmpty()) {
            this.cells.remove(address);
        } else {
            this.cells.put(address, data);
        }
    }

    public CellData getCell(String address) {
        return this.cells.getOrDefault(address, CellData.empty());
    }

    public void setColumnWidth(String column, Integer width) {
        if (width == null || width <= 0) {
            this.columnWidths.remove(column);
        } else {
            this.columnWidths.put(column, width);
        }
    }

    public Integer getColumnWidth(String column) {
        return this.columnWidths.getOrDefault(column, 100);
    }

    public void setRowHeight(String row, Integer height) {
        if (height == null || height <= 0) {
            this.rowHeights.remove(row);
        } else {
            this.rowHeights.put(row, height);
        }
    }

    public Integer getRowHeight(String row) {
        return this.rowHeights.getOrDefault(row, 24);
    }

    public void resize(Integer newRowCount, Integer newColumnCount) {
        if (newRowCount != null && newRowCount > 0) {
            this.rowCount = newRowCount;
        }
        if (newColumnCount != null && newColumnCount > 0) {
            this.columnCount = newColumnCount;
        }
    }

    public boolean isOwner(Long userId) {
        return this.ownerId.equals(userId);
    }

    public boolean canEdit(Long userId) {
        return isOwner(userId) || permission == SpreadsheetPermission.EDITABLE;
    }

    public boolean canView(Long userId) {
        return isOwner(userId) ||
               permission == SpreadsheetPermission.EDITABLE ||
               permission == SpreadsheetPermission.VIEW_ONLY;
    }
}
