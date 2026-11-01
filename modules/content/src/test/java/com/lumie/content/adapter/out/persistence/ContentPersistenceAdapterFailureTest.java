package com.lumie.content.adapter.out.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ContentPersistenceAdapterFailureTest {

    @Test
    void classTargetStorageFailurePropagates() {
        var repository = mock(AnnouncementClassTargetJpaRepository.class);
        var adapter = new AnnouncementClassTargetPersistenceAdapter(repository);
        given(repository.findByAnnouncementId(1L)).willThrow(storageFailure());

        assertThatThrownBy(() -> adapter.findByAnnouncementId(1L))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void announcementReadStorageFailurePropagates() {
        var repository = mock(AnnouncementReadJpaRepository.class);
        var adapter = new AnnouncementReadPersistenceAdapter(repository);
        given(repository.findUserIdsByAnnouncementId(1L)).willThrow(storageFailure());

        assertThatThrownBy(() -> adapter.findUserIdsByAnnouncementId(1L))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void announcementStorageFailurePropagates() {
        var repository = mock(AnnouncementJpaRepository.class);
        var adapter = new AnnouncementPersistenceAdapter(repository);
        given(repository.findAllOrderByImportantDescCreatedAtDesc(PageRequest.of(0, 10)))
                .willThrow(storageFailure());

        assertThatThrownBy(() -> adapter.findAllOrderByImportantDescCreatedAtDesc(PageRequest.of(0, 10)))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void qnaBoardStorageFailurePropagates() {
        var repository = mock(QnaBoardJpaRepository.class);
        var adapter = new QnaBoardPersistenceAdapter(repository);
        given(repository.search("keyword", List.of(5L), false, PageRequest.of(0, 10)))
                .willThrow(storageFailure());

        assertThatThrownBy(() -> adapter.search("keyword", List.of(5L), false, PageRequest.of(0, 10)))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void qnaCommentStorageFailurePropagates() {
        var repository = mock(QnaCommentJpaRepository.class);
        var adapter = new QnaCommentPersistenceAdapter(repository);
        given(repository.findByQnaBoardId(1L)).willThrow(storageFailure());

        assertThatThrownBy(() -> adapter.findByQnaBoardId(1L))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void popupSettingStorageFailurePropagates() {
        var repository = mock(ReviewPopupSettingJpaRepository.class);
        var adapter = new ReviewPopupSettingPersistenceAdapter(repository);
        given(repository.findFirst()).willThrow(storageFailure());

        assertThatThrownBy(adapter::findFirst).isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void reviewStorageFailurePropagates() {
        var repository = mock(ReviewJpaRepository.class);
        var adapter = new ReviewPersistenceAdapter(repository);
        given(repository.findAll(PageRequest.of(0, 10))).willThrow(storageFailure());

        assertThatThrownBy(() -> adapter.findAll(PageRequest.of(0, 10)))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    private static DataAccessResourceFailureException storageFailure() {
        return new DataAccessResourceFailureException("database unavailable");
    }
}
