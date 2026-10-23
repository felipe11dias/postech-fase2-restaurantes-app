package com.postech.restaurantes;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * As convenções da suíte, verificadas em build como a regra de dependência — escritas só no
 * {@code CLAUDE.md}, elas valeriam até o primeiro teste que as esquecesse.
 *
 * <p>As duas que mais importam são as da fronteira entre os dois compromissos da Etapa 11: teste
 * unitário nunca sobe contexto Spring nem toca banco (é o que o mantém rápido e o que dá sentido
 * aos 100% de cobertura); teste de integração nunca substitui um bean da aplicação, exceto o SMTP
 * (é o que o faz provar que os componentes <em>reais</em> se encaixam).
 */
@AnalyzeClasses(
        packages = "com.postech.restaurantes",
        importOptions = ImportOption.OnlyIncludeTests.class)
class TestConventionsTest {

    private static final String UNITARIO = ".*Test(\\$.*)?";

    // Por nome, e não por classe: citar a classe aqui faria este próprio arquivo depender do que
    // ele proíbe aos testes unitários.
    private static final String SPRING_BOOT_TEST = "org.springframework.boot.test.context.SpringBootTest";
    private static final String MOCKITO_BEAN = "org.springframework.test.context.bean.override.mockito.MockitoBean";
    private static final String MOCKITO_SPY_BEAN =
            "org.springframework.test.context.bean.override.mockito.MockitoSpyBean";

    @ArchTest
    static final ArchRule todo_teste_descreve_a_regra_em_linguagem_de_negocio =
            methods().that().areAnnotatedWith(Test.class)
                    .or().areAnnotatedWith(ParameterizedTest.class)
                    .should().beAnnotatedWith(DisplayName.class)
                    .because("o @DisplayName é o que aparece no relatório de testes e diz qual regra falhou");

    @ArchTest
    static final ArchRule todo_teste_se_chama_deve_ou_nao_deve =
            methods().that().areAnnotatedWith(Test.class)
                    .or().areAnnotatedWith(ParameterizedTest.class)
                    .should().haveNameMatching("(deve|naoDeve)[A-Z]\\w*")
                    .because("o nome do método diz o comportamento esperado: deve<Comportamento>[Quando<Condição>]");

    @ArchTest
    static final ArchRule unitario_nao_sobe_contexto_nem_toca_banco =
            noClasses().that().haveNameMatching(UNITARIO)
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.testcontainers..",
                            "org.springframework.boot.test..",
                            "org.springframework.test.context..",
                            "org.springframework.jdbc..")
                    .orShould().beAnnotatedWith(SPRING_BOOT_TEST)
                    .orShould().beAssignableTo(IntegrationTestSupport.class)
                    .orShould().beAssignableTo(WebIntegrationTestSupport.class)
                    .because("teste unitário roda em segundos, sem Docker; o que precisa de contexto é *IT");

    @ArchTest
    static final ArchRule integracao_estende_uma_das_bases =
            classes().that().haveSimpleNameEndingWith("IT")
                    .should().beAssignableTo(IntegrationTestSupport.class)
                    .orShould().beAssignableTo(WebIntegrationTestSupport.class)
                    .because("as bases apontam para o PostgreSQL compartilhado e fornecem o segredo do JWT");

    @ArchTest
    static final ArchRule integracao_so_substitui_o_smtp =
            fields().that().areAnnotatedWith(MOCKITO_BEAN)
                    .should().haveRawType(JavaMailSender.class)
                    .because("teste de integração prova os componentes reais juntos; só o envio de e-mail é trocado");

    @ArchTest
    static final ArchRule integracao_nao_espiona_bean_da_aplicacao =
            noFields().should().beAnnotatedWith(MOCKITO_SPY_BEAN)
                    .because("um espião altera o bean real, e o teste deixa de provar a aplicação como ela é");

    @ArchTest
    static final ArchRule container_do_banco_e_unico_e_compartilhado =
            noClasses().should().beAnnotatedWith("org.testcontainers.junit.jupiter.Testcontainers")
                    .because("a extensão encerraria o container ao fim da primeira classe, e as seguintes "
                            + "reaproveitariam o contexto apontando para um banco morto (ver SharedPostgres)");
}
