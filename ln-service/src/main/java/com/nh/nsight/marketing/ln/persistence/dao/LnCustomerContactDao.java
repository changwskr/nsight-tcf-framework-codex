package com.nh.nsight.marketing.ln.persistence.dao;

import com.nh.nsight.marketing.ln.application.dto.customercontact.CustomerContactSearchCriteria;
import com.nh.nsight.marketing.ln.persistence.dto.customercontact.CustomerContactRow;
import com.nh.nsight.marketing.ln.persistence.mapper.LnCustomerContactMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class LnCustomerContactDao {
    private final LnCustomerContactMapper mapper;

    public LnCustomerContactDao(LnCustomerContactMapper mapper) {
        this.mapper = mapper;
    }

    public List<CustomerContactRow> searchContacts(CustomerContactSearchCriteria criteria) {
        return mapper.searchContacts(criteria);
    }

    public int countContacts(CustomerContactSearchCriteria criteria) {
        return mapper.countContacts(criteria);
    }

    public CustomerContactRow selectByContactId(String contactId) {
        return mapper.selectByContactId(contactId);
    }
}
