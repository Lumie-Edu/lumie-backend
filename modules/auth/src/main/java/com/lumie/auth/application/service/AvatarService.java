package com.lumie.auth.application.service;

import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvatarService {

    private final UserLookupPort userLookupPort;

    public void updateAvatar(Long userId, String avatarSeed) {
        boolean updated = userLookupPort.updateAvatarSeed(userId, avatarSeed);
        if (!updated) {
            throw new ResourceNotFoundException("User", String.valueOf(userId));
        }
        log.info("Avatar seed updated for user {}", userId);
    }
}
