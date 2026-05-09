package com.lumie.tenant.application.port.out;

public interface SchemaProvisioningPort {
    void createSchema(String schemaName);
    void migrateSchema(String schemaName);
    void dropSchema(String schemaName);
    boolean schemaExists(String schemaName);
}
