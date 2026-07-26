package com.nh.nsight.aimethodology.store;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessModelRepository extends JpaRepository<BusinessModelEntity, String> {

    List<BusinessModelEntity> findAllByOrderByUpdatedAtDesc();

    List<BusinessModelEntity> findByServiceIdContainingIgnoreCaseOrServiceNameContainingIgnoreCaseOrDomainCodeContainingIgnoreCase(
            String serviceId, String serviceName, String domainCode);
}
