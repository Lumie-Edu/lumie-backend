package com.lumie.file.adapter.out.persistence;

import com.lumie.file.domain.entity.TextbookFolder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TextbookFolderPersistenceAdapterTest {
    private final TextbookFolderJpaRepository repository = mock(TextbookFolderJpaRepository.class);
    private final TextbookFolderPersistenceAdapter adapter = new TextbookFolderPersistenceAdapter(repository);

    @Test
    void delegatesRootFolderQueriesAndEmptyResults() {
        UUID parent = UUID.randomUUID();
        when(repository.findByParentId(null)).thenReturn(List.of());
        when(repository.existsByParentIdAndName(null, "Root")).thenReturn(false);
        when(repository.existsByParentId(parent)).thenReturn(false);

        assertThat(adapter.findByParentId(null)).isEmpty();
        assertThat(adapter.existsByParentIdAndName(null, "Root")).isFalse();
        assertThat(adapter.existsByParentId(parent)).isFalse();
        verify(repository).findByParentId(null);
        verify(repository).existsByParentIdAndName(null, "Root");
        verify(repository).existsByParentId(parent);
    }

    @Test
    void delegatesCrudOperationsAndRetainsOptionalAbsence() {
        UUID id = UUID.randomUUID();
        TextbookFolder folder = mock(TextbookFolder.class);
        when(repository.findById(id)).thenReturn(Optional.empty());
        when(repository.save(folder)).thenReturn(folder);

        assertThat(adapter.findById(id)).isEmpty();
        assertThat(adapter.save(folder)).isSameAs(folder);
        adapter.deleteById(id);
        verify(repository).findById(id);
        verify(repository).save(folder);
        verify(repository).deleteById(id);
    }

    @Test
    void rootFolderQueryRetainsNullAwareFilterAndNameOrdering() throws NoSuchMethodException {
        var method = TextbookFolderJpaRepository.class.getMethod("findByParentId", UUID.class);

        assertThat(method.getAnnotation(Query.class).value())
                .contains(":parentId IS NULL AND f.parentId IS NULL")
                .contains("ORDER BY f.name ASC");
    }

    @Test
    void propagatesStoreFailure() {
        RuntimeException failure = new IllegalStateException("database unavailable");
        when(repository.findByParentId(null)).thenThrow(failure);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> adapter.findByParentId(null)).isSameAs(failure);
    }
}
