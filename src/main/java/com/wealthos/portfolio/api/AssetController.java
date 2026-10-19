package com.wealthos.portfolio.api;

import com.wealthos.portfolio.dto.AssetDetail;
import com.wealthos.portfolio.dto.AssetUpdateRequest;
import com.wealthos.portfolio.dto.BuyRequest;
import com.wealthos.portfolio.dto.BuyResult;
import com.wealthos.portfolio.dto.SellRequest;
import com.wealthos.portfolio.dto.SellResult;
import com.wealthos.portfolio.dto.TransferRequest;
import com.wealthos.portfolio.dto.TransferResult;
import com.wealthos.portfolio.service.AssetService;
import com.wealthos.portfolio.service.PortfolioQueryService;
import com.wealthos.portfolio.service.TradeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assets;
    private final TradeService trades;
    private final PortfolioQueryService query;

    public AssetController(AssetService assets, TradeService trades, PortfolioQueryService query) {
        this.assets = assets;
        this.trades = trades;
        this.query = query;
    }

    @GetMapping("/{id}")
    public AssetDetail detail(@PathVariable Long id) {
        return query.detail(id);
    }

    // a purchase: goes into the asset you already hold, or creates one
    @PostMapping("/buy")
    @ResponseStatus(HttpStatus.CREATED)
    public BuyResult buy(@Valid @RequestBody BuyRequest request) {
        return assets.buy(request);
    }

    @PutMapping("/{id}")
    public AssetDetail update(@PathVariable Long id, @Valid @RequestBody AssetUpdateRequest request) {
        assets.update(id, request);
        return query.detail(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        assets.delete(id);
    }

    @PostMapping("/{id}/sell")
    public SellResult sell(@PathVariable Long id, @Valid @RequestBody SellRequest request) {
        return trades.sell(id, request);
    }

    @PostMapping("/{id}/transfer")
    public TransferResult transfer(@PathVariable Long id, @Valid @RequestBody TransferRequest request) {
        return trades.transfer(id, request);
    }
}
