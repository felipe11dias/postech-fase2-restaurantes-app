package com.postech.restaurantes.domain.entity.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AdminProfileTest {

    @Test
    @DisplayName("Cria perfil de administrador com código aparado e departamento opcional")
    void deveCriarQuandoValido() {
        AdminProfile profile = AdminProfile.create(" ADM-001 ", " Operações ", true);

        assertEquals("ADM-001", profile.getEmployeeCode());
        assertEquals("Operações", profile.getDepartment());
        assertTrue(profile.isSuperAdmin());
    }

    @Test
    @DisplayName("Restaura pela mesma validação; departamento em branco vira ausente")
    void deveRestaurarSemDepartamento() {
        AdminProfile profile = AdminProfile.restore("ADM-002", "  ", false);

        assertNull(profile.getDepartment());
        assertFalse(profile.isSuperAdmin());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Recusa código de funcionário em branco")
    void deveRecusarCodigoEmBranco(String codigo) {
        assertThrows(IllegalArgumentException.class, () -> AdminProfile.create(codigo, null, false));
    }

    @Test
    @DisplayName("Setters revalidam e alteram o estado")
    void deveRevalidarNosSetters() {
        AdminProfile profile = AdminProfile.create("ADM-001", null, false);

        profile.setEmployeeCode("ADM-009");
        profile.setDepartment("Financeiro");
        profile.setSuperAdmin(true);

        assertEquals("ADM-009", profile.getEmployeeCode());
        assertEquals("Financeiro", profile.getDepartment());
        assertTrue(profile.isSuperAdmin());
        assertThrows(IllegalArgumentException.class, () -> profile.setEmployeeCode(" "));
    }
}
