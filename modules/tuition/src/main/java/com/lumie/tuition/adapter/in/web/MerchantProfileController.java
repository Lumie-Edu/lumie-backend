package com.lumie.tuition.adapter.in.web;

import com.lumie.tuition.application.dto.request.SubmitKycRequest;
import com.lumie.tuition.application.dto.response.MerchantProfileResponse;
import com.lumie.tuition.application.service.MerchantProfileCommandService;
import com.lumie.tuition.application.service.MerchantProfileQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/merchant-profiles")
@RequiredArgsConstructor
public class MerchantProfileController {

    private final MerchantProfileCommandService commandService;
    private final MerchantProfileQueryService queryService;

    @GetMapping("/my")
    public ResponseEntity<MerchantProfileResponse> getMyProfile() {
        return ResponseEntity.ok(queryService.getMyProfile());
    }

    @PostMapping("/kyc")
    public ResponseEntity<MerchantProfileResponse> submitKyc(
            @Valid @RequestBody SubmitKycRequest request) {
        return ResponseEntity.ok(commandService.submitKyc(request));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<MerchantProfileResponse> getStatus(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.getById(id));
    }
}
