package com.nh.nsight.marketing.eb.persistence.dao;

import com.nh.nsight.marketing.eb.application.dto.event.EventSearchCriteria;
import com.nh.nsight.marketing.eb.persistence.dto.event.EventInsertRow;
import com.nh.nsight.marketing.eb.persistence.dto.event.EventRow;
import com.nh.nsight.marketing.eb.persistence.dto.event.EventStatusCountRow;
import com.nh.nsight.marketing.eb.persistence.mapper.EbEventMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class EbEventDao {
    private final EbEventMapper mapper;

    public EbEventDao(EbEventMapper mapper) {
        this.mapper = mapper;
    }

    public int insertEvent(EventInsertRow row) {
        return mapper.insertEvent(row);
    }

    public int updateEventStatus(String eventId, String eventStatus) {
        return mapper.updateEventStatus(eventId, eventStatus);
    }

    public List<EventRow> selectReadyEvents(int limit) {
        return mapper.selectReadyEvents(limit);
    }

    public List<EventRow> searchEvents(EventSearchCriteria criteria) {
        return mapper.searchEvents(criteria);
    }

    public int countEvents(EventSearchCriteria criteria) {
        return mapper.countEvents(criteria);
    }

    public List<EventStatusCountRow> countEventsByStatus() {
        return mapper.countEventsByStatus();
    }
}
