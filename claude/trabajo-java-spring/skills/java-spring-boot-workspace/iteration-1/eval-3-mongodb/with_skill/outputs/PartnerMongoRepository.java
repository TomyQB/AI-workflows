package com.example.partner.repository;

import com.example.partner.entity.PartnerEntity;
import com.example.partner.entity.PartnerStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PartnerMongoRepository extends MongoRepository<PartnerEntity, String> {

    Optional<PartnerEntity> findByMerchantId(String merchantId);

    List<PartnerEntity> findByStatus(PartnerStatus status);

    boolean existsByMerchantId(String merchantId);

    @Query("{ 'status': ?0, 'createdDate': { '$gte': ?1 } }")
    List<PartnerEntity> findByStatusAndCreatedDateAfter(PartnerStatus status, LocalDateTime since);
}
