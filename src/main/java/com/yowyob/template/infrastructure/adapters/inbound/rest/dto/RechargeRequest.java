package com.yowyob.template.infrastructure.adapters.inbound.rest.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RechargeRequest(UUID targetWalletId, BigDecimal amount) {}

