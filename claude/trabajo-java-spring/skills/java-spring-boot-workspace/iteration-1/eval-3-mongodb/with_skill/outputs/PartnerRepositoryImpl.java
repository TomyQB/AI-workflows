package com.example.partner.repository;

import com.example.partner.entity.PartnerEntity;
import com.example.partner.entity.PartnerStatus;
import com.example.partner.exception.PartnerNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PartnerRepositoryImpl implements PartnerRepository {

    private final PartnerMongoRepository mongoRepository;

    @Override
    public PartnerEntity findByMerchantIdOrThrow(final String merchantId) {
        return mongoRepository.findByMerchantId(merchantId)
            .orElseThrow(() -> new PartnerNotFoundException(merchantId));
    }

    @Override
    public List<PartnerEntity> findByStatusCreatedAfter(final PartnerStatus status, final LocalDateTime since) {
        return mongoRepository.findByStatusAndCreatedDateAfter(status, since);
    }
}
