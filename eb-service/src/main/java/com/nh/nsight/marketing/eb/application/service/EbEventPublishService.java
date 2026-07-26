package com.nh.nsight.marketing.eb.application.service;

import com.nh.nsight.marketing.eb.client.EpOnlineClient;
import com.nh.nsight.marketing.eb.client.dto.ep.EpUserEventPayload;
import com.nh.nsight.marketing.eb.config.EbEventPublishProperties;
import com.nh.nsight.marketing.eb.persistence.dao.EbEventDao;
import com.nh.nsight.marketing.eb.persistence.dto.event.EventRow;
import com.nh.nsight.marketing.eb.support.EbEventStatus;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EbEventPublishService {
    private static final Logger log = LoggerFactory.getLogger(EbEventPublishService.class);

    private final EbEventPublishProperties properties;
    private final EbEventDao eventDao;
    private final EpOnlineClient epClient;

    public EbEventPublishService(EbEventPublishProperties properties,
                                 EbEventDao eventDao,
                                 EpOnlineClient epClient) {
        this.properties = properties;
        this.eventDao = eventDao;
        this.epClient = epClient;
    }

    public void publishReadyEvents() {
        System.out.println("---------- [EB-BATCH] START publishReadyEvents (EbEventPublishService.publishReadyEvents) ----------");
        try {
            if (!properties.isEnabled()) {
                System.out.println("[EB-BATCH] SKIP enabled=false (EbEventPublishService.publishReadyEvents) 배치 비활성");
                return;
            }
            List<EventRow> events = eventDao.selectReadyEvents(properties.getBatchSize());
            if (events.isEmpty()) {
                System.out.println("[EB-BATCH] SKIP READY 이벤트 없음 (EbEventPublishService.publishReadyEvents)");
                return;
            }
            log.info("EB event publish batch start count={}", events.size());
            System.out.println("[EB-BATCH] READY 이벤트 " + events.size()
                    + "건 EP 전송 시작 (EbEventPublishService.publishReadyEvents)");
            for (EventRow event : events) {
                publishOne(event);
            }
            System.out.println("[EB-BATCH] READY 이벤트 " + events.size()
                    + "건 처리 완료 (EbEventPublishService.publishReadyEvents)");
        } catch (Exception e) {
            log.warn("EB event publish batch skipped: {}", e.getMessage());
            System.out.println("[EB-BATCH] ERROR publishReadyEvents: " + e.getMessage());
        } finally {
            System.out.println("---------- [EB-BATCH] END publishReadyEvents (EbEventPublishService.publishReadyEvents) ----------");
        }
    }

    private void publishOne(EventRow event) {
        String eventId = event.getEventId();
        String userId = event.getUserId();
        System.out.println("  >> [EB-EVENT] START publishOne (EbEventPublishService.publishOne) eventId="
                + eventId + " userId=" + userId);
        String status = EbEventStatus.FAIL;
        try {
            boolean success = epClient.sendUserEvent(
                    properties.getEpOnlineUrl(), EpUserEventPayload.fromEventRow(event));
            status = success ? EbEventStatus.SENT : EbEventStatus.FAIL;
            eventDao.updateEventStatus(eventId, status);
            log.info("EB event publish eventId={} status={}", eventId, status);
        } catch (Exception e) {
            eventDao.updateEventStatus(eventId, EbEventStatus.FAIL);
            log.warn("EB event publish failed eventId={} message={}", eventId, e.getMessage());
            System.out.println("  >> [EB-EVENT] ERROR publishOne (EbEventPublishService.publishOne) eventId="
                    + eventId + " message=" + e.getMessage());
        } finally {
            System.out.println("  >> [EB-EVENT] END publishOne (EbEventPublishService.publishOne) eventId="
                    + eventId + " status=" + status);
        }
    }
}
