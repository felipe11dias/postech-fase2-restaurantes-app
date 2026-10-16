package com.postech.restaurantes.adapter.datasource.data;

import java.time.LocalDate;

/** Perfil de cliente como a origem de dados o conhece. */
public record ClientData(String cpf, String phone, LocalDate birthDate) {
}
