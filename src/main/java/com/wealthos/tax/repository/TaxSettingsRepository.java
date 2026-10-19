package com.wealthos.tax.repository;

import com.wealthos.tax.domain.TaxSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxSettingsRepository extends JpaRepository<TaxSettings, Long> {
}
