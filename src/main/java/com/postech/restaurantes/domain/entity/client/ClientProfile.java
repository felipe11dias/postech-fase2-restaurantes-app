package com.postech.restaurantes.domain.entity.client;

import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.vo.Cpf;
import com.postech.restaurantes.domain.vo.Phone;
import java.time.LocalDate;

/**
 * Perfil de cliente — a especialização {@code clients} do usuário. Não tem id próprio: a identidade
 * é a do usuário, e o perfil será parte do agregado {@code User}.
 *
 * <p>Invariantes: CPF e telefone válidos; data de nascimento opcional e, quando informada, não
 * futura. "Futura" depende do dia de hoje, que vem por parâmetro — a entidade não consulta o
 * relógio. Por isso a regra vale ao informar a data ({@link #create}, {@link #changeBirthDate}), e
 * não ao reconstruir o que já foi gravado ({@link #restore}).
 */
public final class ClientProfile {

    private Cpf cpf;
    private Phone phone;
    private LocalDate birthDate;

    private ClientProfile() {
    }

    /** Perfil novo; {@code today} é a referência para recusar nascimento no futuro. */
    public static ClientProfile create(String cpf, String phone, LocalDate birthDate, LocalDate today) {
        ClientProfile profile = fill(new ClientProfile(), cpf, phone);
        profile.changeBirthDate(birthDate, today);
        return profile;
    }

    /** Perfil reconstruído a partir da origem de dados. */
    public static ClientProfile restore(String cpf, String phone, LocalDate birthDate) {
        ClientProfile profile = fill(new ClientProfile(), cpf, phone);
        profile.birthDate = birthDate;
        return profile;
    }

    private static ClientProfile fill(ClientProfile profile, String cpf, String phone) {
        profile.setCpf(Cpf.of(cpf));
        profile.setPhone(Phone.of(phone));
        return profile;
    }

    public void setCpf(Cpf cpf) {
        this.cpf = Guard.requireNonNull(cpf, "CPF inválido");
    }

    public void setPhone(Phone phone) {
        this.phone = Guard.requireNonNull(phone, "Telefone inválido");
    }

    /** Data opcional: {@code null} remove. Informada, não pode ser posterior a {@code today}. */
    public void changeBirthDate(LocalDate birthDate, LocalDate today) {
        Guard.requireNonNull(today, "Data de referência inválida");
        Guard.require(birthDate == null || !birthDate.isAfter(today), "Data de nascimento não pode ser futura");
        this.birthDate = birthDate;
    }

    public Cpf getCpf() {
        return cpf;
    }

    public Phone getPhone() {
        return phone;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }
}
