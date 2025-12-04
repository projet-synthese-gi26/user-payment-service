package com.yowyob.template.domain.ports.out;

import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.util.UUID;

public interface RechargePublisherPort {
    Mono<Void> publishRechargeEvent(UUID targetWalletId, BigDecimal amount);
}
