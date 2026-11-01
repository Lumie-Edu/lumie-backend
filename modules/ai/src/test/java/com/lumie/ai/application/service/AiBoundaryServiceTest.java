package com.lumie.ai.application.service;

import com.lumie.ai.application.port.out.SchemaMetadataPort;
import com.lumie.ai.application.port.out.SchemaMetadataPort.SchemaColumn;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiBoundaryServiceTest {

    @Test
    void schemaDiscoveryFormatsMetadataAndCachesItPerTenant() {
        SchemaMetadataPort metadataPort = mock(SchemaMetadataPort.class);
        when(metadataPort.discoverColumns()).thenReturn(List.of(
                new SchemaColumn("classes", "id", "int8", null),
                new SchemaColumn("classes", "name", "varchar", null),
                new SchemaColumn("preferences", "settings", "jsonb", "PRIMARY KEY")
        ));
        SchemaDiscoveryService service = new SchemaDiscoveryService(metadataPort);

        String result = service.getSchemaDescription("tenant-a");
        String cachedResult = service.getSchemaDescription("tenant-a");

        assertThat(result).isEqualTo("- classes (id, name)\n- preferences (settings JSONB PK)\n");
        assertThat(cachedResult).isEqualTo(result);
        org.mockito.Mockito.verify(metadataPort).discoverColumns();
    }

    @Test
    void schemaDiscoveryKeepsItsExistingEmptyResultOnStorageFailure() {
        SchemaMetadataPort metadataPort = mock(SchemaMetadataPort.class);
        when(metadataPort.discoverColumns()).thenThrow(new IllegalStateException("database unavailable"));

        assertThat(new SchemaDiscoveryService(metadataPort).getSchemaDescription("tenant-a")).isEmpty();
    }
}
