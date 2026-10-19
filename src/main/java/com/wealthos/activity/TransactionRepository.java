package com.wealthos.activity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("select t from Transaction t order by t.date desc, t.id desc")
    List<Transaction> findLatest(Pageable page);

    List<Transaction> findByTypeInOrderByDateAscIdAsc(Collection<TransactionType> types);

    @Modifying
    @Query("delete from Transaction t where t.type in :types")
    int deleteByTypeIn(@Param("types") Collection<TransactionType> types);
}
