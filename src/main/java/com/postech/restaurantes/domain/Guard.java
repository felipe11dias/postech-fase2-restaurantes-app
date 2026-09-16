package com.postech.restaurantes.domain;

/**
 * Verificações de invariantes compartilhadas pelas entidades e VOs. Toda violação é uma
 * {@link IllegalArgumentException}: o dado nunca chegou a existir no domínio.
 */
public final class Guard {

    private Guard() {
    }

    public static <T> T requireNonNull(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /** Normaliza campos opcionais: espaços em branco viram ausência. */
    public static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
