package com.wealthos.tax.repository;

import com.wealthos.tax.domain.TaxEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TaxEntryRepository extends JpaRepository<TaxEntry, Long> {

    List<TaxEntry> findByDateBetweenOrderByDateDescIdDesc(LocalDate from, LocalDate to);
}
