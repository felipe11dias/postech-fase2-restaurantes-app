package com.postech.restaurantes.application.dto.common;

/** Direção de ordenação, independente de framework. */
public enum SortDirection {
    ASC,
    DESC;

    /** Converte texto tolerante a caixa; qualquer valor que não seja "desc" vira ASC. */
    public static SortDirection from(String value) {
        if (value != null && value.trim().equalsIgnoreCase("desc")) {
            return DESC;
        }
        return ASC;
    }
}
