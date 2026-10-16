package com.postech.restaurantes.domain.entity.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.postech.restaurantes.domain.vo.Cpf;
import com.postech.restaurantes.domain.vo.Phone;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClientProfileTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 5);
    private static final String CPF = "529.982.247-25";
    private static final String PHONE = "(11) 91234-5678";

    @Test
    @DisplayName("Cria perfil de cliente com CPF e telefone normalizados e nascimento no passado")
    void deveCriarQuandoValido() {
        ClientProfile profile = ClientProfile.create(CPF, PHONE, LocalDate.of(1990, 5, 20), HOJE);

        assertEquals(Cpf.of(CPF), profile.getCpf());
        assertEquals(Phone.of(PHONE), profile.getPhone());
        assertEquals(LocalDate.of(1990, 5, 20), profile.getBirthDate());
    }

    @Test
    @DisplayName("Nascimento é opcional, e hoje é aceito")
    void deveAceitarNascimentoAusenteOuHoje() {
        assertNull(ClientProfile.create(CPF, PHONE, null, HOJE).getBirthDate());
        assertEquals(HOJE, ClientProfile.create(CPF, PHONE, HOJE, HOJE).getBirthDate());
    }

    @Test
    @DisplayName("Recusa nascimento no futuro e criação sem a data de referência")
    void deveRecusarNascimentoFuturo() {
        assertThrows(IllegalArgumentException.class,
                () -> ClientProfile.create(CPF, PHONE, HOJE.plusDays(1), HOJE));
        assertThrows(IllegalArgumentException.class, () -> ClientProfile.create(CPF, PHONE, null, null));
    }

    @Test
    @DisplayName("Restaura o que foi gravado sem precisar do dia de hoje")
    void deveRestaurarSemDataDeReferencia() {
        ClientProfile profile = ClientProfile.restore(CPF, PHONE, LocalDate.of(1990, 5, 20));

        assertEquals(LocalDate.of(1990, 5, 20), profile.getBirthDate());
    }

    @Test
    @DisplayName("Recusa CPF e telefone inválidos, na criação e nos setters")
    void deveRecusarDocumentosInvalidos() {
        ClientProfile profile = ClientProfile.create(CPF, PHONE, null, HOJE);

        assertThrows(IllegalArgumentException.class, () -> ClientProfile.create("123", PHONE, null, HOJE));
        assertThrows(IllegalArgumentException.class, () -> ClientProfile.restore(CPF, "1", null));
        assertThrows(IllegalArgumentException.class, () -> profile.setCpf(null));
        assertThrows(IllegalArgumentException.class, () -> profile.setPhone(null));
    }

    @Test
    @DisplayName("Troca de nascimento revalida contra o dia de hoje; recusada, não altera")
    void deveTrocarONascimento() {
        ClientProfile profile = ClientProfile.create(CPF, PHONE, LocalDate.of(1990, 5, 20), HOJE);

        assertThrows(IllegalArgumentException.class, () -> profile.changeBirthDate(HOJE.plusYears(1), HOJE));
        assertEquals(LocalDate.of(1990, 5, 20), profile.getBirthDate());

        profile.changeBirthDate(null, HOJE);
        profile.setCpf(Cpf.of("111.444.777-35"));
        profile.setPhone(Phone.of("1131234567"));

        assertNull(profile.getBirthDate());
        assertEquals("11144477735", profile.getCpf().value());
        assertEquals("1131234567", profile.getPhone().value());
    }
}
