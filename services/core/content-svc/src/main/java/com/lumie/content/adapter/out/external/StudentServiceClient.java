package com.lumie.content.adapter.out.external;

import com.lumie.content.application.port.out.StudentServicePort;
import com.lumie.grpc.academy.AcademyServiceGrpc;
import com.lumie.grpc.academy.GetStudentRequest;
import com.lumie.grpc.academy.ValidateStudentRequest;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class StudentServiceClient implements StudentServicePort {

    @GrpcClient("academy-svc")
    private AcademyServiceGrpc.AcademyServiceBlockingStub academyServiceStub;

    @Override
    public boolean validateStudent(String tenantSlug, Long studentId) {
        try {
            var request = ValidateStudentRequest.newBuilder()
                    .setTenantSlug(tenantSlug)
                    .setStudentId(studentId)
                    .build();

            var response = academyServiceStub.validateStudent(request);
            return response.getValid();
        } catch (StatusRuntimeException e) {
            log.error("gRPC error validating student: {} in tenant: {}", studentId, tenantSlug, e);
            return false;
        }
    }

    @Override
    public Optional<StudentData> getStudent(String tenantSlug, Long studentId) {
        try {
            var request = GetStudentRequest.newBuilder()
                    .setTenantSlug(tenantSlug)
                    .setStudentId(studentId)
                    .build();

            var response = academyServiceStub.getStudent(request);

            return Optional.of(new StudentData(
                    response.getId(),
                    response.getUserId(),
                    response.getStudentName(),
                    response.getStudentPhone(),
                    response.getAcademyId(),
                    response.getAcademyName(),
                    response.getIsActive()
            ));
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting student: {} in tenant: {}", studentId, tenantSlug, e);
            return Optional.empty();
        }
    }
}
