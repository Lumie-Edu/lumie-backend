package com.lumie.content.application.port.out;

import com.lumie.content.domain.entity.QnaBoard;
import com.lumie.content.domain.entity.Reservation;

public interface ContentEventPublisherPort {

    void publishQnaReplied(QnaBoard qnaBoard, String tenantSlug);

    void publishReservationConfirmed(Reservation reservation, String tenantSlug);
}
