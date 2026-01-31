package com.lumie.exam.adapter.out.external;

import com.lumie.exam.application.port.out.StudentServicePort;
import com.lumie.exam.infrastructure.tenant.TenantContextHolder;
import com.lumie.grpc.academy.AcademyServiceGrpc;
import com.lumie.grpc.academy.GetStudentRequest;
import com.lumie.grpc.academy.ValidateStudentRequest;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StudentServiceClient implements StudentServicePort {

    @GrpcClient("academy-svc")
    private AcademyServiceGrpc.AcademyServiceBlockingStub academyServiceStub;

    @Override
    public boolean existsById(Long studentId) {
        try {
            var request = ValidateStudentRequest.newBuilder()
                    .setTenantSlug(TenantContextHolder.getRequiredTenant())
                    .setStudentId(studentId)
                    .build();

            var response = academyServiceStub.validateStudent(request);
            return response.getValid();
        } catch (StatusRuntimeException e) {
            log.debug("Student validation failed: {}", studentId);
            return false;
        }
    }

    @Override
    public StudentInfo getStudentInfo(Long studentId) {
        try {
            var request = GetStudentRequest.newBuilder()
                    .setTenantSlug(TenantContextHolder.getRequiredTenant())
                    .setStudentId(studentId)
                    .build();

            var response = academyServiceStub.getStudent(request);
            return new StudentInfo(
                    response.getId(),
                    response.getStudentName(),
                    "",  // email not in proto
                    response.getStudentPhone()
            );
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting student: {}", studentId, e);
            return null;
        }
    }
}
