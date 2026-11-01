package com.lumie.ai.application.port.out;

import java.util.List;

/** Application-owned access to database metadata used by schema discovery. */
public interface SchemaMetadataPort {

    List<SchemaColumn> discoverColumns();

    record SchemaColumn(String tableName, String columnName, String udtName, String constraintType) {
    }
}
