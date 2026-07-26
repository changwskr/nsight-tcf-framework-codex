package com.nh.nsight.marketing.ep.persistence.mapper;

import com.nh.nsight.marketing.ep.application.dto.userevent.UserEventSearchCriteria;
import com.nh.nsight.marketing.ep.persistence.dto.userevent.UserEventInsertRow;
import com.nh.nsight.marketing.ep.persistence.dto.userevent.UserEventRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EpUserEventMapper {

    int insertReceivedEvent(UserEventInsertRow row);

    int countByEventId(String eventId);

    List<UserEventRow> searchReceivedEvents(UserEventSearchCriteria criteria);

    int countReceivedEvents(UserEventSearchCriteria criteria);
}
