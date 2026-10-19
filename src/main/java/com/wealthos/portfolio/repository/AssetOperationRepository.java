package com.wealthos.portfolio.repository;

import com.wealthos.portfolio.domain.AssetOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetOperationRepository extends JpaRepository<AssetOperation, Long> {

    List<AssetOperation> findByTransferId(String transferId);
}
