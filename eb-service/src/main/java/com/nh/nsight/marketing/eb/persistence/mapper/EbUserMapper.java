package com.nh.nsight.marketing.eb.persistence.mapper;

import com.nh.nsight.marketing.eb.application.dto.user.UserSearchCriteria;
import com.nh.nsight.marketing.eb.persistence.dto.user.UserInsertRow;
import com.nh.nsight.marketing.eb.persistence.dto.user.UserRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EbUserMapper {
    int insertUser(UserInsertRow row);

    int countByUserId(String userId);

    List<UserRow> searchUsers(UserSearchCriteria criteria);

    int countUsers(UserSearchCriteria criteria);
}
