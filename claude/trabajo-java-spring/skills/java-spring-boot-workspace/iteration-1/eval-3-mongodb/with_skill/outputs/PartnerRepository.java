package com.example.partner.repository;

import com.example.partner.entity.PartnerEntity;
import com.example.partner.entity.PartnerStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface PartnerRepository {

    PartnerEntity findByMerchantIdOrThrow(String merchantId);

    List<PartnerEntity> findByStatusCreatedAfter(PartnerStatus status, LocalDateTime since);
}
