package com.nh.nsight.marketing.eb.persistence.dao;

import com.nh.nsight.marketing.eb.application.dto.user.UserSearchCriteria;
import com.nh.nsight.marketing.eb.persistence.dto.user.UserInsertRow;
import com.nh.nsight.marketing.eb.persistence.dto.user.UserRow;
import com.nh.nsight.marketing.eb.persistence.mapper.EbUserMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class EbUserDao {
    private final EbUserMapper mapper;

    public EbUserDao(EbUserMapper mapper) {
        this.mapper = mapper;
    }

    public int insertUser(UserInsertRow row) {
        return mapper.insertUser(row);
    }

    public boolean existsByUserId(String userId) {
        return mapper.countByUserId(userId) > 0;
    }

    public List<UserRow> searchUsers(UserSearchCriteria criteria) {
        return mapper.searchUsers(criteria);
    }

    public int countUsers(UserSearchCriteria criteria) {
        return mapper.countUsers(criteria);
    }
}
