package com.ias.flashreserve.domain.port.in;

import com.ias.flashreserve.domain.model.ProviderCommand;
import com.ias.flashreserve.domain.model.ProviderEventResult;
import reactor.core.publisher.Mono;

public interface ApplyProviderEventUseCase {
    Mono<ProviderEventResult> apply(ProviderCommand command);
}
