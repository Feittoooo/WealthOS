package com.wealthos.portfolio.api;

import com.wealthos.portfolio.dto.PortfolioOverview;
import com.wealthos.portfolio.service.PortfolioQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioQueryService query;

    public PortfolioController(PortfolioQueryService query) {
        this.query = query;
    }

    @GetMapping
    public PortfolioOverview overview() {
        return query.overview();
    }
}
