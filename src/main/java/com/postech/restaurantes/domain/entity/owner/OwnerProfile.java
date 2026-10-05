package com.postech.restaurantes.domain.entity.owner;

import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.vo.Cnpj;
import com.postech.restaurantes.domain.vo.Phone;

/**
 * Perfil de dono de restaurante — a especialização {@code owners} do usuário. Não tem id próprio: a
 * identidade é a do usuário (no banco, a chave primária é a do usuário), e o perfil será parte do
 * agregado {@code User}.
 *
 * <p>Invariantes: CNPJ válido, razão social não vazia, telefone comercial válido.
 */
public final class OwnerProfile {

    private Cnpj cnpj;
    private String legalName;
    private Phone businessPhone;

    private OwnerProfile() {
    }

    /** Perfil novo. */
    public static OwnerProfile create(String cnpj, String legalName, String businessPhone) {
        return fill(new OwnerProfile(), cnpj, legalName, businessPhone);
    }

    /** Perfil reconstruído a partir da origem de dados; passa pela mesma validação. */
    public static OwnerProfile restore(String cnpj, String legalName, String businessPhone) {
        return fill(new OwnerProfile(), cnpj, legalName, businessPhone);
    }

    private static OwnerProfile fill(OwnerProfile profile, String cnpj, String legalName, String businessPhone) {
        profile.setCnpj(Cnpj.of(cnpj));
        profile.setLegalName(legalName);
        profile.setBusinessPhone(Phone.of(businessPhone));
        return profile;
    }

    public void setCnpj(Cnpj cnpj) {
        this.cnpj = Guard.requireNonNull(cnpj, "CNPJ inválido");
    }

    public void setLegalName(String legalName) {
        this.legalName = Guard.requireNonBlank(legalName, "Razão social inválida");
    }

    public void setBusinessPhone(Phone businessPhone) {
        this.businessPhone = Guard.requireNonNull(businessPhone, "Telefone comercial inválido");
    }

    public Cnpj getCnpj() {
        return cnpj;
    }

    public String getLegalName() {
        return legalName;
    }

    public Phone getBusinessPhone() {
        return businessPhone;
    }
}
