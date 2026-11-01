package com.lumie.content.adapter.out.persistence;
import com.lumie.content.application.port.out.AnnouncementReadPersistencePort; import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Repository; import java.util.List;
@Repository @RequiredArgsConstructor public class AnnouncementReadPersistenceAdapter implements AnnouncementReadPersistencePort {
 private final AnnouncementReadJpaRepository repository;
 @Override public int insertIfAbsent(Long tenantId,Long announcementId,Long userId){return repository.insertIfAbsent(tenantId,announcementId,userId);}
 @Override public List<Long> findUserIdsByAnnouncementId(Long id){return repository.findUserIdsByAnnouncementId(id);}
}