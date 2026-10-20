package com.lumie.file.adapter.out.storage;

import com.lumie.common.exception.BusinessException;
import com.lumie.file.domain.exception.FileErrorCode;
import io.minio.CopyObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.ObjectWriteResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import io.minio.http.Method;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MinioStorageAdapterTest {

    private final MinioClient minio = mock(MinioClient.class);
    private final MinioStorageAdapter adapter = new MinioStorageAdapter(minio, mock(MinioClient.class), "files");

    @Test
    void copyUsesTheExpectedSourceEtagAsAConditionalRead() throws Exception {
        adapter.copyObject("tmp/source.pdf", "files/target.pdf", "etag-1");

        AtomicReference<CopyObjectArgs> captured = new AtomicReference<>();
        verify(minio).copyObject(org.mockito.ArgumentMatchers.argThat(args -> {
            captured.set(args);
            return true;
        }));
        assertThat(captured.get().source().matchETag()).contains("etag-1");
    }

    @Test
    void storageOutageIsNotReportedAsAnAbsentObject() throws Exception {
        when(minio.statObject(any())).thenThrow(new RuntimeException("MinIO unavailable"));

        assertThatThrownBy(() -> adapter.objectExists("tmp/source.pdf"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.STORAGE_ERROR));
    }

    @Test
    void explicitNoSuchKeyResponseIsReportedAsAbsent() throws Exception {
        ErrorResponse error = new ErrorResponse(
                "NoSuchKey", "missing", "files", "source.pdf", "/files/source.pdf", "request", "host");
        when(minio.statObject(any())).thenThrow(new ErrorResponseException(
                error, minioResponse(), "missing object"));

        assertThat(adapter.objectExists("source.pdf")).isFalse();
    }

    @Test
    void deleteTreatsExplicitNoSuchKeyAsSuccessfulIdempotentRemoval() throws Exception {
        ErrorResponse error = new ErrorResponse(
                "NoSuchKey", "missing", "files", "source.pdf", "/files/source.pdf", "request", "host");
        org.mockito.BDDMockito.willThrow(new ErrorResponseException(
                error, minioResponse(), "missing object"))
                .given(minio).removeObject(any());

        adapter.deleteObject("source.pdf");
    }

    @Test
    void uploadPresignUsesPutAndMapsGenerationFailure() throws Exception {
        MinioClient presign = mock(MinioClient.class);
        MinioStorageAdapter presignAdapter = new MinioStorageAdapter(minio, presign, "files");
        when(presign.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn("signed-url");

        assertThat(presignAdapter.generatePresignedUploadUrl("tmp/a.pdf", "application/pdf", 900))
                .isEqualTo("signed-url");
        org.mockito.ArgumentCaptor<GetPresignedObjectUrlArgs> captured =
                org.mockito.ArgumentCaptor.forClass(GetPresignedObjectUrlArgs.class);
        verify(presign).getPresignedObjectUrl(captured.capture());
        assertThat(captured.getValue().method()).isEqualTo(Method.PUT);

        when(presign.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenThrow(new RuntimeException("presign unavailable"));
        assertThatThrownBy(() -> presignAdapter.generatePresignedUploadUrl(
                "tmp/a.pdf", "application/pdf", 900))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.PRESIGNED_URL_GENERATION_FAILED));

        when(minio.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenThrow(new RuntimeException("internal presign unavailable"));
        assertThatThrownBy(() -> presignAdapter.generateInternalPresignedUploadUrl(
                "files/a.pdf", "application/pdf", 60))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.PRESIGNED_URL_GENERATION_FAILED));
    }

    @Test
    void downloadPresignsUseExpectedClientAndMethod() throws Exception {
        MinioClient presign = mock(MinioClient.class);
        MinioStorageAdapter presignAdapter = new MinioStorageAdapter(minio, presign, "files");
        when(presign.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn("external-url");
        when(minio.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn("internal-url");

        assertThat(presignAdapter.generatePresignedDownloadUrl("files/a.pdf", 60)).isEqualTo("external-url");
        assertThat(presignAdapter.generateInternalPresignedDownloadUrl("files/a.pdf", 60)).isEqualTo("internal-url");
        org.mockito.ArgumentCaptor<GetPresignedObjectUrlArgs> args =
                org.mockito.ArgumentCaptor.forClass(GetPresignedObjectUrlArgs.class);
        verify(presign).getPresignedObjectUrl(args.capture());
        assertThat(args.getValue().method()).isEqualTo(Method.GET);
        verify(minio).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));

        when(presign.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenThrow(new RuntimeException("download presign unavailable"));
        assertThatThrownBy(() -> presignAdapter.generatePresignedDownloadUrl("files/a.pdf", 60))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.PRESIGNED_URL_GENERATION_FAILED));
    }

    @Test
    void internalUploadAndObjectUploadAndReadDelegateToMinio() throws Exception {
        when(minio.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn("internal-put-url");
        when(minio.putObject(any(PutObjectArgs.class))).thenReturn(mock(ObjectWriteResponse.class));
        io.minio.GetObjectResponse content = mock(io.minio.GetObjectResponse.class);
        when(minio.getObject(any(io.minio.GetObjectArgs.class))).thenReturn(content);

        assertThat(adapter.generateInternalPresignedUploadUrl("files/a.pdf", "application/pdf", 60))
                .isEqualTo("internal-put-url");
        adapter.uploadObject("files/a.pdf", content, 3L, "application/pdf");
        assertThat(adapter.getObject("files/a.pdf")).isSameAs(content);
        verify(minio).putObject(any(PutObjectArgs.class));
    }

    @Test
    void objectStatMethodsReturnSizeEtagTypeAndLastModified() throws Exception {
        StatObjectResponse response = mock(StatObjectResponse.class);
        when(minio.statObject(any(StatObjectArgs.class))).thenReturn(response);
        when(response.size()).thenReturn(123L);
        when(response.etag()).thenReturn("etag-1");
        when(response.contentType()).thenReturn("application/pdf");
        ZonedDateTime lastModified = ZonedDateTime.ofInstant(Instant.parse("2026-10-20T00:00:00Z"), ZoneOffset.UTC);
        when(response.lastModified()).thenReturn(lastModified);

        assertThat(adapter.objectSize("files/a.pdf")).isEqualTo(123L);
        assertThat(adapter.objectEtag("files/a.pdf")).isEqualTo("etag-1");
        assertThat(adapter.objectContentType("files/a.pdf")).isEqualTo("application/pdf");
        assertThat(adapter.objectLastModified("files/a.pdf")).isEqualTo(lastModified.toInstant());
    }

    @Test
    void objectExistsReturnsTrueForSuccessfulStat() throws Exception {
        when(minio.statObject(any(StatObjectArgs.class))).thenReturn(mock(StatObjectResponse.class));

        assertThat(adapter.objectExists("files/a.pdf")).isTrue();
    }

    @Test
    void objectStatAndCopyErrorsDistinguishMissingFromRetryableStorageFailure() throws Exception {
        when(minio.statObject(any(StatObjectArgs.class))).thenThrow(new RuntimeException("MinIO unavailable"));
        assertThatThrownBy(() -> adapter.objectSize("files/a.pdf"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.STORAGE_ERROR));

        when(minio.statObject(any(StatObjectArgs.class))).thenThrow(missingKey("NoSuchObject"));
        assertThatThrownBy(() -> adapter.objectEtag("files/a.pdf"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.FILE_NOT_FOUND));
        assertThatThrownBy(() -> adapter.objectContentType("files/a.pdf"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.FILE_NOT_FOUND));
        assertThatThrownBy(() -> adapter.objectLastModified("files/a.pdf"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.FILE_NOT_FOUND));

        when(minio.copyObject(any(CopyObjectArgs.class))).thenThrow(missingKey("NoSuchKey"));
        assertThatThrownBy(() -> adapter.copyObject("tmp/a.pdf", "files/a.pdf", "etag-1"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.FILE_NOT_FOUND));

        when(minio.statObject(any(StatObjectArgs.class))).thenThrow(new ErrorResponseException(
                new ErrorResponse(null, "missing code", "files", "a.pdf", "/files/a.pdf", "request", "host"),
                minioResponse(), "missing error details"));
        assertThatThrownBy(() -> adapter.objectExists("files/a.pdf"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.STORAGE_ERROR));
    }

    @Test
    void uploadReadAndDeleteFailuresMapToStorageErrorsExceptMissingDelete() throws Exception {
        when(minio.putObject(any(PutObjectArgs.class))).thenThrow(new RuntimeException("put failed"));
        assertThatThrownBy(() -> adapter.uploadObject("files/a.pdf", new ByteArrayInputStream(new byte[0]),
                0L, "application/pdf"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.STORAGE_ERROR));

        when(minio.getObject(any(io.minio.GetObjectArgs.class))).thenThrow(new RuntimeException("get failed"));
        assertThatThrownBy(() -> adapter.getObject("files/a.pdf"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.STORAGE_ERROR));

        org.mockito.BDDMockito.willThrow(new RuntimeException("delete failed"))
                .given(minio).removeObject(any(RemoveObjectArgs.class));
        assertThatThrownBy(() -> adapter.deleteObject("files/a.pdf"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.STORAGE_ERROR));
        org.mockito.BDDMockito.willThrow(missingKey("NoSuchKey"))
                .given(minio).removeObject(any(RemoveObjectArgs.class));
        adapter.deleteObject("files/a.pdf");
    }

    private static ErrorResponseException missingKey(String code) {
        return new ErrorResponseException(
                new ErrorResponse(code, "missing", "files", "a.pdf", "/files/a.pdf", "request", "host"),
                minioResponse(), "missing object");
    }

    private static okhttp3.Response minioResponse() {
        okhttp3.Request request = new okhttp3.Request.Builder().url("http://localhost/files/a.pdf").build();
        return new okhttp3.Response.Builder()
                .request(request)
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .code(404)
                .message("Not Found")
                .build();
    }
}
