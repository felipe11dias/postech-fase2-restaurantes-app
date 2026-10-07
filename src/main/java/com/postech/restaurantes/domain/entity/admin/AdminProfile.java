package com.postech.restaurantes.domain.entity.admin;

import com.postech.restaurantes.domain.Guard;

/**
 * Perfil de administrador — a especialização {@code admins} do usuário. Não tem id próprio: a
 * identidade é a do usuário, e o perfil será parte do agregado {@code User}. Quem pode ganhar este
 * perfil é regra de entrada (nunca o autocadastro), e fica no caso de uso.
 *
 * <p>Invariantes: código de funcionário não vazio; departamento opcional (em branco vira ausente).
 */
public final class AdminProfile {

    private String employeeCode;
    private String department;
    private boolean superAdmin;

    private AdminProfile() {
    }

    /** Perfil novo. */
    public static AdminProfile create(String employeeCode, String department, boolean superAdmin) {
        return fill(new AdminProfile(), employeeCode, department, superAdmin);
    }

    /** Perfil reconstruído a partir da origem de dados; passa pela mesma validação. */
    public static AdminProfile restore(String employeeCode, String department, boolean superAdmin) {
        return fill(new AdminProfile(), employeeCode, department, superAdmin);
    }

    private static AdminProfile fill(AdminProfile profile, String employeeCode, String department,
                                     boolean superAdmin) {
        profile.setEmployeeCode(employeeCode);
        profile.setDepartment(department);
        profile.setSuperAdmin(superAdmin);
        return profile;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = Guard.requireNonBlank(employeeCode, "Código de funcionário inválido");
    }

    public void setDepartment(String department) {
        this.department = Guard.trimToNull(department);
    }

    public void setSuperAdmin(boolean superAdmin) {
        this.superAdmin = superAdmin;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getDepartment() {
        return department;
    }

    public boolean isSuperAdmin() {
        return superAdmin;
    }
}
