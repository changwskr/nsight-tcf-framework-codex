package com.nh.nsight.marketing.ep.application.service;

import com.nh.nsight.marketing.ep.application.dto.userevent.UserEventInquiryRequest;
import com.nh.nsight.marketing.ep.application.dto.userevent.UserEventInquiryResponse;
import com.nh.nsight.marketing.ep.application.dto.userevent.UserEventReceiveRequest;
import com.nh.nsight.marketing.ep.application.dto.userevent.UserEventReceiveResponse;
import com.nh.nsight.marketing.ep.application.dto.userevent.UserEventSearchCriteria;
import com.nh.nsight.marketing.ep.application.rule.EpUserEventRule;
import com.nh.nsight.marketing.ep.persistence.dao.EpUserEventDao;
import com.nh.nsight.marketing.ep.persistence.dto.userevent.UserEventInsertRow;
import com.nh.nsight.marketing.ep.persistence.dto.userevent.UserEventRow;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EpUserEventService {
    private static final Logger log = LoggerFactory.getLogger(EpUserEventService.class);

    private final EpUserEventRule rule;
    private final EpUserEventDao dao;

    public EpUserEventService(EpUserEventRule rule, EpUserEventDao dao) {
        this.rule = rule;
        this.dao = dao;
    }

    public UserEventInquiryResponse inquiry(UserEventInquiryRequest request, TransactionContext context) {
        rule.validateInquiry(request);
        UserEventSearchCriteria criteria = rule.buildSearchCriteria(request);
        List<UserEventRow> list = dao.searchReceivedEvents(criteria);
        int totalCount = dao.countReceivedEvents(criteria);
        return UserEventInquiryResponse.of(context, criteria, list, totalCount);
    }

    @Transactional(timeout = 5)
    public UserEventReceiveResponse receive(UserEventReceiveRequest request, TransactionContext context) {
        String eventId = request != null ? request.getEventId() : "";
        System.out.println("========== [EP-EVENT] START receive (EP.UserEvent.receive / EpUserEventService.receive) eventId="
                + eventId + " ==========");
        try {
            rule.validateReceive(request);
            eventId = request.getEventId();
            String userId = request.getUserId();
            String eventType = request.getEventType();
            System.out.println("[EP-EVENT] body (EpUserEventService.receive) eventId=" + eventId
                    + " userId=" + userId + " eventType=" + eventType);

            if (dao.existsByEventId(eventId)) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "이미 수신한 이벤트입니다: " + eventId);
            }

            log.info("EP user event received eventId={} userId={} eventType={}", eventId, userId, eventType);

            UserEventInsertRow row = new UserEventInsertRow();
            row.setEventId(eventId);
            row.setUserId(userId);
            row.setEventType(eventType);
            dao.insertReceivedEvent(row);

            System.out.println("[EP-EVENT] EP_USER_EVENT 저장 완료 (EpUserEventService.receive) eventId=" + eventId);
            return UserEventReceiveResponse.of(context, request);
        } catch (Exception e) {
            System.out.println("[EP-EVENT] ERROR receive (EpUserEventService.receive) eventId=" + eventId
                    + " message=" + e.getMessage());
            throw e;
        } finally {
            System.out.println("========== [EP-EVENT] END receive (EP.UserEvent.receive / EpUserEventService.receive) eventId="
                    + eventId + " ==========");
        }
    }
}
