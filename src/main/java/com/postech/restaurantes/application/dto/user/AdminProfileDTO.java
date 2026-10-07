package com.postech.restaurantes.application.dto.user;

import com.postech.restaurantes.domain.entity.admin.AdminProfile;

/** Perfil de administrador como chega aos casos de uso. {@code superAdmin} ausente vale {@code false}. */
public record AdminProfileDTO(String employeeCode, String department, Boolean superAdmin) implements UserProfileDTO {

    public AdminProfile toEntity() {
        return AdminProfile.create(employeeCode, department, Boolean.TRUE.equals(superAdmin));
    }
}
