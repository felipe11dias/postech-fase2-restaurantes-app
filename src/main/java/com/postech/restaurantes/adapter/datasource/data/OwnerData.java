package com.postech.restaurantes.adapter.datasource.data;

/** Perfil de dono como a origem de dados o conhece: documentos sem máscara. */
public record OwnerData(String cnpj, String legalName, String businessPhone) {
}
