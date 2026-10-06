package com.postech.restaurantes.adapter.gateway;

import static com.postech.restaurantes.adapter.AdapterFixtures.HASH;
import static com.postech.restaurantes.adapter.AdapterFixtures.NOW;
import static com.postech.restaurantes.adapter.AdapterFixtures.USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.adapter.service.IMailSender;
import com.postech.restaurantes.adapter.service.ITokenEncoder;
import com.postech.restaurantes.adapter.service.data.TokenClaimsData;
import com.postech.restaurantes.application.dto.auth.IssuedToken;
import com.postech.restaurantes.domain.entity.client.ClientProfile;
import com.postech.restaurantes.domain.entity.owner.OwnerProfile;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.entity.user.UserAddress;
import com.postech.restaurantes.domain.entity.user.UserProfiles;
import com.postech.restaurantes.domain.vo.Email;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

/**
 * Gateways de serviço: o tradutor fica no adaptador (Aulas 05 e 06). Os serviços externos são
 * mockados pela interface; o que se verifica é a tradução — o texto do e-mail, os claims do token.
 */
class ServiceGatewaysTest {

    @Nested
    @DisplayName("E-mail de redefinição de senha")
    class Email_ {

        private final IMailSender mailSender = mock(IMailSender.class);
        private final PasswordResetMailGateway gateway = PasswordResetMailGateway.create(mailSender);

        @Test
        @DisplayName("Monta a mensagem com o token em claro e a validade informada e entrega ao serviço")
        void deveMontarAMensagemDeRedefinicao() {
            gateway.sendPasswordReset(Email.of("Joao.Silva@Email.com"), "token-em-claro", Duration.ofMinutes(45));

            ArgumentCaptor<String> corpo = ArgumentCaptor.forClass(String.class);
            verify(mailSender).send(eq("joao.silva@email.com"), eq("Redefinição de senha"), corpo.capture());
            assertTrue(corpo.getValue().contains("token-em-claro"));
            assertTrue(corpo.getValue().contains("45 minutos"), "a validade é a que o caso de uso informou");
        }

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({"PT1M, 1 minuto", "PT30M, 30 minutos", "PT90S, 90 segundos", "PT1S, 1 segundo",
                "PT0.5S, menos de um segundo"})
        @DisplayName("A validade sai como o usuário a lê: singular e plural, e em segundos quando não é minuto inteiro")
        void deveDescreverAValidade(Duration validade, String esperado) {
            assertEquals(esperado, PasswordResetMailGateway.describe(validade));
        }

        @Test
        @DisplayName("Sem serviço de e-mail o gateway não é criado")
        void naoDeveCriarSemServico() {
            assertThrows(IllegalArgumentException.class, () -> PasswordResetMailGateway.create(null));
        }
    }

    @Nested
    @DisplayName("Emissão de token")
    class Token {

        private final ITokenEncoder encoder = mock(ITokenEncoder.class);
        private final TokenGateway gateway = TokenGateway.create(encoder);

        @Test
        @DisplayName("Traduz o usuário em claims — id, login e nomes dos papéis derivados dos perfis — e devolve o token codificado")
        void deveTraduzirOUsuarioEmClaims() {
            IssuedToken emitido = new IssuedToken("token", NOW.plusHours(1));
            when(encoder.encode(any())).thenReturn(emitido);
            User usuario = User.restore(USER_ID, "João Silva", "joao.silva@email.com", "joao.silva", HASH,
                    new UserProfiles(OwnerProfile.restore("11222333000181", "Sabor Ltda", "1131234567"),
                            ClientProfile.restore("52998224725", "11912345678", null), null, null),
                    List.<UserAddress>of(), NOW, NOW);

            IssuedToken resultado = gateway.issue(usuario);

            ArgumentCaptor<TokenClaimsData> claims = ArgumentCaptor.forClass(TokenClaimsData.class);
            verify(encoder).encode(claims.capture());
            assertEquals(new TokenClaimsData(USER_ID, "joao.silva", Set.of("ROLE_OWNER", "ROLE_CLIENT")),
                    claims.getValue());
            assertSame(emitido, resultado);
        }

        @Test
        @DisplayName("Sem codificador o gateway não é criado")
        void naoDeveCriarSemCodificador() {
            assertThrows(IllegalArgumentException.class, () -> TokenGateway.create(null));
        }
    }
}
