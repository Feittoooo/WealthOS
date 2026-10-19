package com.wealthos.tax.dto;

import java.time.YearMonth;

public record CuotaMonth(YearMonth month, boolean paid, boolean current, boolean future) {
}
