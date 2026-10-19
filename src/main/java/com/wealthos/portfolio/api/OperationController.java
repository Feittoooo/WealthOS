package com.wealthos.portfolio.api;

import com.wealthos.portfolio.domain.Asset;
import com.wealthos.portfolio.dto.AssetDetail;
import com.wealthos.portfolio.dto.OperationUpdateRequest;
import com.wealthos.portfolio.service.AssetService;
import com.wealthos.portfolio.service.PortfolioQueryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// One buy, sale or transfer inside an asset. Responses are the updated asset detail.
@RestController
@RequestMapping("/api/operations")
public class OperationController {

    private final AssetService assets;
    private final PortfolioQueryService query;

    public OperationController(AssetService assets, PortfolioQueryService query) {
        this.assets = assets;
        this.query = query;
    }

    @PutMapping("/{id}")
    public AssetDetail update(@PathVariable Long id, @Valid @RequestBody OperationUpdateRequest request) {
        Asset asset = assets.updateOperation(id, request);
        return query.detail(asset.getId());
    }

    // 204 when that was the asset's last operation and the asset is gone too
    @DeleteMapping("/{id}")
    public ResponseEntity<AssetDetail> delete(@PathVariable Long id) {
        Asset asset = assets.deleteOperation(id);
        return asset == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(query.detail(asset.getId()));
    }
}
