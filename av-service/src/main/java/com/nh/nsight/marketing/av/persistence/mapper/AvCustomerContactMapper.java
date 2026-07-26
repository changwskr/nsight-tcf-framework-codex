package com.nh.nsight.marketing.av.persistence.mapper;

import com.nh.nsight.marketing.av.application.dto.customercontact.CustomerContactSearchCriteria;
import com.nh.nsight.marketing.av.persistence.dto.customercontact.CustomerContactRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AvCustomerContactMapper {
    List<CustomerContactRow> searchContacts(CustomerContactSearchCriteria criteria);

    int countContacts(CustomerContactSearchCriteria criteria);

    CustomerContactRow selectByContactId(@Param("contactId") String contactId);
}
