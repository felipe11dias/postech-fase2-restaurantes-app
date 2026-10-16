package com.postech.restaurantes.adapter.datasource.data;

/** Perfil de administrador como a origem de dados o conhece. */
public record AdminData(String employeeCode, String department, boolean superAdmin) {
}
