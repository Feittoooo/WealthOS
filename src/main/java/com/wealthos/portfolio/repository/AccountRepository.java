package com.wealthos.portfolio.repository;

import com.wealthos.portfolio.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findAllByOrderByNameAsc();
}
