package com.nh.nsight.marketing.ep.persistence.dao;

import com.nh.nsight.marketing.ep.application.dto.userevent.UserEventSearchCriteria;
import com.nh.nsight.marketing.ep.persistence.dto.userevent.UserEventInsertRow;
import com.nh.nsight.marketing.ep.persistence.dto.userevent.UserEventRow;
import com.nh.nsight.marketing.ep.persistence.mapper.EpUserEventMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class EpUserEventDao {
    private final EpUserEventMapper mapper;

    public EpUserEventDao(EpUserEventMapper mapper) {
        this.mapper = mapper;
    }

    public int insertReceivedEvent(UserEventInsertRow row) {
        return mapper.insertReceivedEvent(row);
    }

    public boolean existsByEventId(String eventId) {
        return mapper.countByEventId(eventId) > 0;
    }

    public List<UserEventRow> searchReceivedEvents(UserEventSearchCriteria criteria) {
        return mapper.searchReceivedEvents(criteria);
    }

    public int countReceivedEvents(UserEventSearchCriteria criteria) {
        return mapper.countReceivedEvents(criteria);
    }
}
