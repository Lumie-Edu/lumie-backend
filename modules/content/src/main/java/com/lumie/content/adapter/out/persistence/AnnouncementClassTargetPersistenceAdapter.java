package com.lumie.content.adapter.out.persistence;
import com.lumie.content.application.port.out.AnnouncementClassTargetPersistencePort;
import com.lumie.content.domain.entity.AnnouncementClassTarget;
import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Repository; import java.util.*;
@Repository @RequiredArgsConstructor public class AnnouncementClassTargetPersistenceAdapter implements AnnouncementClassTargetPersistencePort {
 private final AnnouncementClassTargetJpaRepository repository;
 @Override public List<AnnouncementClassTarget> findByAnnouncementId(Long id){return repository.findByAnnouncementId(id);}
 @Override public List<AnnouncementClassTarget> findByAnnouncementIdIn(Collection<Long> ids){return repository.findByAnnouncementIdIn(ids);}
 @Override public void deleteByAnnouncementId(Long id){repository.deleteByAnnouncementId(id);}
 @Override public void flush(){repository.flush();}
 @Override public <S extends AnnouncementClassTarget> List<S> saveAll(Iterable<S> entities){return repository.saveAll(entities);}
}