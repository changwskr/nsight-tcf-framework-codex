package com.nh.nsight.marketing.av.persistence.dao;

import com.nh.nsight.marketing.av.application.dto.customercontact.CustomerContactSearchCriteria;
import com.nh.nsight.marketing.av.persistence.dto.customercontact.CustomerContactRow;
import com.nh.nsight.marketing.av.persistence.mapper.AvCustomerContactMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class AvCustomerContactDao {
    private final AvCustomerContactMapper mapper;

    public AvCustomerContactDao(AvCustomerContactMapper mapper) {
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
