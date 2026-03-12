package com.example.partner.repository;

import com.example.partner.entity.PartnerEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PartnerRepository extends MongoRepository<PartnerEntity, String> {

    Optional<PartnerEntity> findByMerchantId(String merchantId);

    List<PartnerEntity> findByStatus(String status);

    @Query("{ 'status': ?0, 'createdDate': { $gt: ?1 } }")
    List<PartnerEntity> findByStatusAndCreatedDateAfter(String status, Instant createdDate);
}
