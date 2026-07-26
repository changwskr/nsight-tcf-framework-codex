package com.nh.nsight.marketing.eb.application.scheduler;

import com.nh.nsight.marketing.eb.application.service.EbEventPublishService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EbEventPublishScheduler {
    private final EbEventPublishService publishService;

    public EbEventPublishScheduler(EbEventPublishService publishService) {
        this.publishService = publishService;
    }

    @Scheduled(fixedDelayString = "${nsight.eb.event-publish.fixed-delay-ms:60000}")
    public void publishUserEvents() {
        System.out.println("========== [EB-SCHEDULER] START 배치 tick (EbEventPublishScheduler.publishUserEvents) ==========");
        try {
            publishService.publishReadyEvents();
        } finally {
            System.out.println("========== [EB-SCHEDULER] END 배치 tick (EbEventPublishScheduler.publishUserEvents) ==========");
        }
    }
}
