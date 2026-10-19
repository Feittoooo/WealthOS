package com.wealthos.portfolio.repository;

import com.wealthos.portfolio.domain.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    @Query("select distinct a from Asset a join fetch a.account left join fetch a.operations order by a.ticker")
    List<Asset> findAllWithOperations();

    List<Asset> findByAccountId(Long accountId);

    List<Asset> findByAutoPriceTrue();

    // how a new purchase finds the asset it belongs to
    @Query("select a from Asset a where a.account.id = :accountId and "
            + "((:isin is not null and a.isin = :isin) or upper(a.ticker) = upper(:ticker))")
    List<Asset> findSame(@Param("accountId") Long accountId, @Param("ticker") String ticker, @Param("isin") String isin);
}
