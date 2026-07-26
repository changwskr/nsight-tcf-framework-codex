package com.nh.nsight.marketing.eb.persistence.mapper;

import com.nh.nsight.marketing.eb.application.dto.event.EventSearchCriteria;
import com.nh.nsight.marketing.eb.persistence.dto.event.EventInsertRow;
import com.nh.nsight.marketing.eb.persistence.dto.event.EventRow;
import com.nh.nsight.marketing.eb.persistence.dto.event.EventStatusCountRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EbEventMapper {

    int insertEvent(EventInsertRow row);

    int updateEventStatus(@Param("eventId") String eventId,
                          @Param("eventStatus") String eventStatus);

    List<EventRow> selectReadyEvents(@Param("limit") int limit);

    List<EventRow> searchEvents(EventSearchCriteria criteria);

    int countEvents(EventSearchCriteria criteria);

    List<EventStatusCountRow> countEventsByStatus();
}
