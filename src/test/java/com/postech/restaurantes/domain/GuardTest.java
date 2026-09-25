package com.postech.restaurantes.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.postech.restaurantes.domain.entity.user.RoleName;
import com.postech.restaurantes.domain.exception.InvariantViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GuardTest {

    @Test
    @DisplayName("Devolve o próprio valor quando não é nulo")
    void deveDevolverValorQuandoNaoNulo() {
        Object value = new Object();

        Object result = Guard.requireNonNull(value, "msg");

        assertSame(value, result);
    }

    @Test
    @DisplayName("Recusa valor nulo com a mensagem informada")
    void deveRecusarQuandoNulo() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Guard.requireNonNull(null, "Campo obrigatório"));

        assertEquals("Campo obrigatório", ex.getMessage());
    }

    @Test
    @DisplayName("Devolve o texto sem espaços nas bordas quando não está em branco")
    void deveDevolverTextoAparadoQuandoNaoBranco() {
        String result = Guard.requireNonBlank("  valor  ", "msg");

        assertEquals("valor", result);
    }

    @Test
    @DisplayName("Recusa texto nulo")
    void deveRecusarTextoNulo() {
        assertThrows(IllegalArgumentException.class, () -> Guard.requireNonBlank(null, "msg"));
    }

    @Test
    @DisplayName("Recusa texto composto só de espaços")
    void deveRecusarTextoEmBranco() {
        assertThrows(IllegalArgumentException.class, () -> Guard.requireNonBlank("   ", "msg"));
    }

    @Test
    @DisplayName("Aceita condição verdadeira sem lançar")
    void deveAceitarCondicaoVerdadeira() {
        Guard.require(true, "msg");
    }

    @Test
    @DisplayName("Recusa condição falsa com a mensagem informada")
    void deveRecusarCondicaoFalsa() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Guard.require(false, "Condição violada"));

        assertEquals("Condição violada", ex.getMessage());
    }

    @Test
    @DisplayName("Normaliza opcional: nulo permanece nulo")
    void deveManterNuloQuandoOpcionalNulo() {
        assertNull(Guard.trimToNull(null));
    }

    @Test
    @DisplayName("Normaliza opcional: em branco vira nulo")
    void deveConverterBrancoEmNulo() {
        assertNull(Guard.trimToNull("   "));
    }

    @Test
    @DisplayName("Normaliza opcional: texto é aparado")
    void deveApararTextoOpcional() {
        assertEquals("abc", Guard.trimToNull(" abc "));
    }

    /**
     * O tipo importa fora do domínio: a mensagem de uma {@link InvariantViolationException} vai
     * para a resposta HTTP, e a de uma {@code IllegalArgumentException} qualquer, não.
     */
    @Test
    @DisplayName("Toda violação sai como InvariantViolationException, com a mensagem escrita para o usuário")
    void deveLancarViolacaoDeInvariante() {
        InvariantViolationException nulo = assertThrows(InvariantViolationException.class,
                () -> Guard.requireNonNull(null, "Id inválido"));
        InvariantViolationException branco = assertThrows(InvariantViolationException.class,
                () -> Guard.requireNonBlank("  ", "Nome inválido"));
        InvariantViolationException condicao = assertThrows(InvariantViolationException.class,
                () -> Guard.require(false, "CEP inválido"));
        InvariantViolationException papel = assertThrows(InvariantViolationException.class,
                () -> RoleName.from("ROLE_INEXISTENTE"));

        assertEquals("Id inválido", nulo.getMessage());
        assertEquals("Nome inválido", branco.getMessage());
        assertEquals("CEP inválido", condicao.getMessage());
        assertEquals("Papel inválido: ROLE_INEXISTENTE", papel.getMessage());
    }
}
