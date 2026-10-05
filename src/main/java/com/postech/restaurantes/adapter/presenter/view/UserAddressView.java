package com.postech.restaurantes.adapter.presenter.view;

import java.util.UUID;

public record UserAddressView(UUID id, String label, boolean isDefault, AddressView address) {
}
