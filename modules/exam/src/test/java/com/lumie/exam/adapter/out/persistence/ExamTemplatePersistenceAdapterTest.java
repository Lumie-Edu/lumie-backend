package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.domain.entity.ExamTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamTemplatePersistenceAdapterTest {

    @Mock private ExamTemplateJpaRepository jpaRepository;
    @InjectMocks private ExamTemplatePersistenceAdapter adapter;

    @Test
    void everyPortOperationDelegates() throws Exception {
        PersistenceAdapterContractAssertions.everyPortOperationDelegates(
                com.lumie.exam.application.port.out.ExamTemplatePersistencePort.class,
                ExamTemplateJpaRepository.class, adapter, jpaRepository);
    }

    @Test
    void preservesAbsentAndEmptyReads() {
        when(jpaRepository.findById(1L)).thenReturn(Optional.empty());
        when(jpaRepository.findAll()).thenReturn(List.of());
        when(jpaRepository.existsById(1L)).thenReturn(false);
        var pageable = PageRequest.of(0, 10);
        when(jpaRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

        assertThat(adapter.findById(1L)).isEmpty();
        assertThat(adapter.findAll()).isEmpty();
        assertThat(adapter.existsById(1L)).isFalse();
        assertThat(adapter.findAll(pageable).getPageable()).isEqualTo(pageable);
    }

    @Test
    void delegatesSaveAndPropagatesDeleteFailure() {
        ExamTemplate template = org.mockito.Mockito.mock(ExamTemplate.class);
        when(jpaRepository.save(template)).thenReturn(template);
        doThrow(new IllegalStateException("database unavailable")).when(jpaRepository).deleteById(4L);

        assertThat(adapter.save(template)).isSameAs(template);
        assertThatThrownBy(() -> adapter.deleteById(4L)).isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");
        verify(jpaRepository).save(template);
    }
}
