package com.nh.nsight.marketing.eb.application.service;

import com.nh.nsight.marketing.eb.application.dto.user.UserCreateRequest;
import com.nh.nsight.marketing.eb.application.dto.user.UserCreateResponse;
import com.nh.nsight.marketing.eb.application.dto.user.UserInquiryRequest;
import com.nh.nsight.marketing.eb.application.dto.user.UserInquiryResponse;
import com.nh.nsight.marketing.eb.application.dto.user.UserSearchCriteria;
import com.nh.nsight.marketing.eb.application.rule.EbUserRule;
import com.nh.nsight.marketing.eb.persistence.dao.EbEventDao;
import com.nh.nsight.marketing.eb.persistence.dao.EbUserDao;
import com.nh.nsight.marketing.eb.persistence.dto.event.EventInsertRow;
import com.nh.nsight.marketing.eb.persistence.dto.user.UserInsertRow;
import com.nh.nsight.marketing.eb.persistence.dto.user.UserRow;
import com.nh.nsight.marketing.eb.support.EbEventStatus;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import com.nh.nsight.tcf.util.GuidGenerator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EbUserService {
    private final EbUserRule rule;
    private final EbUserDao userDao;
    private final EbEventDao eventDao;

    public EbUserService(EbUserRule rule, EbUserDao userDao, EbEventDao eventDao) {
        this.rule = rule;
        this.userDao = userDao;
        this.eventDao = eventDao;
    }

    public UserInquiryResponse inquiry(UserInquiryRequest request, TransactionContext context) {
        rule.validateInquiry(request);
        UserSearchCriteria criteria = rule.buildSearchCriteria(request);
        List<UserRow> rows = userDao.searchUsers(criteria);
        int totalCount = userDao.countUsers(criteria);
        return UserInquiryResponse.of(context, criteria, rows, totalCount);
    }

    @Transactional(timeout = 5)
    public UserCreateResponse create(UserCreateRequest request, TransactionContext context) {
        rule.validateCreate(request);
        String userId = request.getUserId();
        String userName = request.getUserName();
        String branchId = request.getBranchId();

        if (userDao.existsByUserId(userId)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "이미 등록된 사용자입니다: " + userId);
        }

        UserInsertRow userRow = new UserInsertRow();
        userRow.setUserId(userId);
        userRow.setUserName(userName);
        userRow.setBranchId(branchId);
        userDao.insertUser(userRow);

        System.out.println("---------- [EB-EVENT] START Outbox 적재 (EB.User.create / EbUserService.create) userId="
                + userId + " ----------");
        String eventId = GuidGenerator.newGuid();
        EventInsertRow eventRow = new EventInsertRow();
        eventRow.setEventId(eventId);
        eventRow.setUserId(userId);
        eventRow.setEventType(EbEventStatus.USER_CREATED);
        eventRow.setEventStatus(EbEventStatus.READY);
        eventRow.setRetryCount(0);
        eventDao.insertEvent(eventRow);
        System.out.println("---------- [EB-EVENT] END Outbox 적재 (EbUserService.create) eventId=" + eventId
                + " eventType=" + EbEventStatus.USER_CREATED + " status=" + EbEventStatus.READY + " ----------");

        return UserCreateResponse.of(
                context, userId, userName, branchId, eventId, EbEventStatus.USER_CREATED, EbEventStatus.READY);
    }
}
