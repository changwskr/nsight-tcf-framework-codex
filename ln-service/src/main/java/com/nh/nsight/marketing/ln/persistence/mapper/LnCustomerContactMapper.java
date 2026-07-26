package com.nh.nsight.marketing.ln.persistence.mapper;

import com.nh.nsight.marketing.ln.application.dto.customercontact.CustomerContactSearchCriteria;
import com.nh.nsight.marketing.ln.persistence.dto.customercontact.CustomerContactRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LnCustomerContactMapper {
    List<CustomerContactRow> searchContacts(CustomerContactSearchCriteria criteria);

    int countContacts(CustomerContactSearchCriteria criteria);

    CustomerContactRow selectByContactId(@Param("contactId") String contactId);
}
