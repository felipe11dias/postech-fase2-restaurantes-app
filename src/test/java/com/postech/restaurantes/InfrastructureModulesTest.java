package com.postech.restaurantes;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.postech.restaurantes.infrastructure.web.security.AuthenticatedUser;
import com.postech.restaurantes.infrastructure.web.security.IAccessTokenReader;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * A infraestrutura como conjunto de <em>módulos-plugin</em> substituíveis, verificada em build.
 *
 * <p>Trocar uma tecnologia deve ser apagar um subpacote e criar outro ao lado. Isso só é verdade
 * se (1) não há ciclos, (2) nenhum módulo conhece outro módulo-irmão — só {@code main} liga as
 * pontas — e (3) cada biblioteca aparece só dentro do módulo que a encapsula. Estas regras provam
 * as três coisas; sem elas, a estrutura de pastas seria só uma intenção.
 *
 * <p>Referências: Martin, Clean Architecture — ADP (cap. 14), fronteiras e plugins (caps. 17 e
 * 30-32), o componente Main (cap. 26); DIP em Agile PPP (a interface pertence ao cliente).
 */
@AnalyzeClasses(
        packages = "com.postech.restaurantes",
        importOptions = ImportOption.DoNotIncludeTests.class)
class InfrastructureModulesTest {

    private static final String MAIN = "..infrastructure.main..";
    private static final String WEB = "..infrastructure.web..";
    private static final String PERSISTENCE = "..infrastructure.persistence..";
    private static final String TOKEN = "..infrastructure.token..";
    private static final String CRYPTO = "..infrastructure.crypto..";
    private static final String MAIL = "..infrastructure.mail..";

    // ---- Princípio das Dependências Acíclicas ------------------------------------------------

    @ArchTest
    static final ArchRule nenhum_ciclo_entre_pacotes =
            slices().matching("com.postech.restaurantes.(**)")
                    .should().beFreeOfCycles()
                    .because("um ciclo junta os pacotes num bloco só: nenhum deles pode mais ser "
                            + "trocado, testado ou entendido separadamente (ADP)");

    // ---- Nenhum módulo conhece outro; só main liga as pontas -----------------------------------

    @ArchTest
    static final ArchRule persistencia_nao_conhece_outros_modulos =
            noClasses().that().resideInAPackage(PERSISTENCE)
                    .should().dependOnClassesThat().resideInAnyPackage(MAIN, WEB, TOKEN, CRYPTO, MAIL)
                    .because("trocar JPA não pode obrigar a mexer em segurança, token ou e-mail — o autor "
                            + "da auditoria chega pronto, ligado em main");

    @ArchTest
    static final ArchRule criptografia_nao_conhece_outros_modulos =
            noClasses().that().resideInAPackage(CRYPTO)
                    .should().dependOnClassesThat().resideInAnyPackage(MAIN, WEB, PERSISTENCE, TOKEN, MAIL);

    @ArchTest
    static final ArchRule email_nao_conhece_outros_modulos =
            noClasses().that().resideInAPackage(MAIL)
                    .should().dependOnClassesThat().resideInAnyPackage(MAIN, WEB, PERSISTENCE, TOKEN, CRYPTO)
                    .because("a validade do token chega pela porta IMailGateway, não por configuração alheia");

    /**
     * O módulo de token implementa a porta que a web declarou para ler o Bearer — e só isso ele
     * pode conhecer de lá: a interface e o tipo que ela devolve.
     */
    @ArchTest
    static final ArchRule token_so_conhece_a_porta_que_implementa =
            noClasses().that().resideInAPackage(TOKEN)
                    .should().dependOnClassesThat(resideInAnyPackage(MAIN, PERSISTENCE, CRYPTO, MAIL)
                            .or(resideInAPackage(WEB).and(not(belongToAnyOf(IAccessTokenReader.class,
                                    AuthenticatedUser.class)))))
                    .because("trocar o formato do token é criar outro subpacote de token que implemente "
                            + "ITokenIssuer e IAccessTokenReader");

    @ArchTest
    static final ArchRule web_nao_conhece_implementacoes =
            noClasses().that().resideInAPackage(WEB)
                    .should().dependOnClassesThat().resideInAnyPackage(MAIN, PERSISTENCE, TOKEN, CRYPTO, MAIL)
                    .because("a entrega HTTP fala com o núcleo pelos controllers de adaptação e com o "
                            + "formato do token pela porta que ela mesma declara");

    // ---- Cada biblioteca confinada ao módulo que a encapsula ----------------------------------

    @ArchTest
    static final ArchRule jpa_so_no_modulo_de_persistencia =
            noClasses().that().resideOutsideOfPackages(PERSISTENCE, MAIN)
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "jakarta.persistence..", "org.hibernate..",
                            "org.springframework.data..", "org.springframework.transaction..");

    @ArchTest
    static final ArchRule jjwt_so_no_modulo_de_token =
            noClasses().that().resideOutsideOfPackage("..infrastructure.token.jwt..")
                    .should().dependOnClassesThat().resideInAPackage("io.jsonwebtoken..");

    @ArchTest
    static final ArchRule spring_mail_so_no_modulo_de_email =
            noClasses().that().resideOutsideOfPackage("..infrastructure.mail.smtp..")
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework.mail..", "jakarta.mail..");

    @ArchTest
    static final ArchRule spring_security_so_na_web =
            noClasses().that().resideOutsideOfPackages(WEB, CRYPTO)
                    .should().dependOnClassesThat().resideInAPackage("org.springframework.security..");

    @ArchTest
    static final ArchRule criptografia_usa_so_o_modulo_crypto_do_spring_security =
            noClasses().that().resideInAPackage(CRYPTO)
                    .should().dependOnClassesThat(resideInAPackage("org.springframework.security..")
                            .and(not(resideInAPackage("org.springframework.security.crypto.."))));

    @ArchTest
    static final ArchRule http_e_documentacao_so_na_web =
            noClasses().that().resideOutsideOfPackage(WEB)
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework.web..", "org.springframework.hateoas..", "jakarta.servlet..",
                            "jakarta.validation..", "io.swagger..", "org.springdoc..");

    // ---- Configuração só declara ---------------------------------------------------------------

    /**
     * Classe {@code *Config} é {@code @Configuration}, e vice-versa: é o que sustenta a exclusão
     * {@code **}{@code /infrastructure/**}{@code /*Config.class} do JaCoCo sem esconder lógica.
     */
    @ArchTest
    static final ArchRule configuracao_tem_nome_e_anotacao =
            classes().that().haveSimpleNameEndingWith("Config")
                    .should().beAnnotatedWith(org.springframework.context.annotation.Configuration.class)
                    .andShould().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule toda_configuracao_termina_em_Config =
            classes().that().areAnnotatedWith(org.springframework.context.annotation.Configuration.class)
                    .should().haveSimpleNameEndingWith("Config");
}
