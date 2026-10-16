package com.postech.restaurantes.adapter.presenter.view;

import java.time.LocalDate;

public record ClientProfileView(String cpf, String phone, LocalDate birthDate) {
}
