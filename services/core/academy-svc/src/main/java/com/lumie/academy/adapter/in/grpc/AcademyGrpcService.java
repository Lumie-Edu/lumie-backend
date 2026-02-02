package com.lumie.academy.adapter.in.grpc;

import com.lumie.academy.application.service.AcademyQueryService;
import com.lumie.academy.application.service.StudentQueryService;
import com.lumie.academy.domain.exception.AcademyNotFoundException;
import com.lumie.academy.domain.exception.StudentNotFoundException;
import com.lumie.academy.infrastructure.tenant.TenantContextHolder;
import com.lumie.grpc.academy.AcademyResponse;
import com.lumie.grpc.academy.AcademyServiceGrpc;
import com.lumie.grpc.academy.GetAcademyRequest;
import com.lumie.grpc.academy.GetStudentRequest;
import com.lumie.grpc.academy.GetStudentsByAcademyRequest;
import com.lumie.grpc.academy.StudentListResponse;
import com.lumie.grpc.academy.StudentResponse;
import com.lumie.grpc.academy.ValidateStudentRequest;
import com.lumie.grpc.academy.ValidateStudentResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class AcademyGrpcService extends AcademyServiceGrpc.AcademyServiceImplBase {

    private final StudentQueryService studentQueryService;
    private final AcademyQueryService academyQueryService;

    @Override
    public void getStudent(GetStudentRequest request, StreamObserver<StudentResponse> responseObserver) {
        try {
            TenantContextHolder.setTenant(request.getTenantSlug());

            com.lumie.academy.application.dto.StudentResponse student =
                studentQueryService.getStudent(request.getStudentId());

            StudentResponse response = toGrpcStudentResponse(student);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (StudentNotFoundException e) {
            log.warn("Student not found: {}", request.getStudentId());
            responseObserver.onError(Status.NOT_FOUND
                .withDescription(e.getMessage())
                .asRuntimeException());
        } catch (Exception e) {
            log.error("Error getting student: {}", request.getStudentId(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription(e.getMessage())
                .asRuntimeException());
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public void getStudentsByAcademy(GetStudentsByAcademyRequest request,
                                     StreamObserver<StudentListResponse> responseObserver) {
        try {
            TenantContextHolder.setTenant(request.getTenantSlug());

            PageRequest pageable = PageRequest.of(request.getPage(), request.getSize());
            Page<com.lumie.academy.application.dto.StudentResponse> students =
                studentQueryService.getActiveStudentsByAcademy(request.getAcademyId(), pageable);

            StudentListResponse.Builder responseBuilder = StudentListResponse.newBuilder()
                .setTotalCount(students.getTotalElements())
                .setPage(request.getPage())
                .setSize(request.getSize());

            for (com.lumie.academy.application.dto.StudentResponse student : students.getContent()) {
                responseBuilder.addStudents(toGrpcStudentResponse(student));
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting students by academy: {}", request.getAcademyId(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription(e.getMessage())
                .asRuntimeException());
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public void validateStudent(ValidateStudentRequest request,
                                StreamObserver<ValidateStudentResponse> responseObserver) {
        try {
            TenantContextHolder.setTenant(request.getTenantSlug());

            try {
                com.lumie.academy.application.dto.StudentResponse student =
                    studentQueryService.getStudent(request.getStudentId());

                responseObserver.onNext(ValidateStudentResponse.newBuilder()
                    .setValid(true)
                    .setStudentId(student.id())
                    .setMessage("Student is valid")
                    .build());
            } catch (StudentNotFoundException e) {
                responseObserver.onNext(ValidateStudentResponse.newBuilder()
                    .setValid(false)
                    .setStudentId(request.getStudentId())
                    .setMessage("Student not found")
                    .build());
            }

            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error validating student: {}", request.getStudentId(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription(e.getMessage())
                .asRuntimeException());
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public void getAcademy(GetAcademyRequest request,
                           StreamObserver<AcademyResponse> responseObserver) {
        try {
            TenantContextHolder.setTenant(request.getTenantSlug());

            com.lumie.academy.application.dto.AcademyResponse academy =
                academyQueryService.getAcademy(request.getAcademyId());

            AcademyResponse response = AcademyResponse.newBuilder()
                .setId(academy.id())
                .setAcademyName(academy.name())
                .setAcademyPhone(academy.phone() != null ? academy.phone() : "")
                .setAcademyAddress(academy.address() != null ? academy.address() : "")
                .setIsActive(academy.isActive())
                .setStudentCount(academy.studentCount())
                .setCreatedAt(academy.createdAt().toString())
                .setUpdatedAt(academy.updatedAt().toString())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (AcademyNotFoundException e) {
            log.warn("Academy not found: {}", request.getAcademyId());
            responseObserver.onError(Status.NOT_FOUND
                .withDescription(e.getMessage())
                .asRuntimeException());
        } catch (Exception e) {
            log.error("Error getting academy: {}", request.getAcademyId(), e);
            responseObserver.onError(Status.INTERNAL
                .withDescription(e.getMessage())
                .asRuntimeException());
        } finally {
            TenantContextHolder.clear();
        }
    }

    private StudentResponse toGrpcStudentResponse(com.lumie.academy.application.dto.StudentResponse dto) {
        return StudentResponse.newBuilder()
            .setId(dto.id())
            .setUserId(dto.userId())
            .setStudentName(dto.name())
            .setStudentPhone(dto.phone() != null ? dto.phone() : "")
            .setAcademyId(dto.academyId())
            .setAcademyName(dto.academyName())
            .setIsActive(dto.isActive())
            .setCreatedAt(dto.createdAt().toString())
            .setUpdatedAt(dto.updatedAt().toString())
            .build();
    }
}
