package com.postech.restaurantes.application.usecase.location;

import com.postech.restaurantes.application.gateway.ILocationGateway;
import com.postech.restaurantes.domain.entity.location.state.State;
import java.util.List;

/** As 27 unidades federativas formam um catálogo pequeno e de tamanho fixo. */
public final class FindStatesUseCase {
    private final ILocationGateway gateway;

    private FindStatesUseCase(ILocationGateway gateway) { this.gateway = gateway; }

    public static FindStatesUseCase create(ILocationGateway gateway) {
        return new FindStatesUseCase(gateway);
    }

    public List<State> run() { return gateway.findStates(); }
}
