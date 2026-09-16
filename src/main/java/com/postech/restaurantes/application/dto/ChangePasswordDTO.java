package com.postech.restaurantes.application.dto;

/** Entrada da troca de senha pelo próprio usuário. */
public record ChangePasswordDTO(String currentPassword, String newPassword, String confirmPassword) {
}
