package com.postech.restaurantes;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.postech.restaurantes.application.gateway.IPasswordEncoder;
import com.postech.restaurantes.application.gateway.ISecureTokenGenerator;
import com.postech.restaurantes.application.gateway.IUnitOfWork;
import com.postech.restaurantes.infrastructure.api.rest.spring.security.AuthenticatedUser;
import com.postech.restaurantes.infrastructure.api.rest.spring.security.IAccessTokenReader;
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
    private static final String API = "..infrastructure.api..";
    private static final String API_SPRING = "..infrastructure.api.rest.spring";
    private static final String PERSISTENCE = "..infrastructure.persistence..";
    private static final String TOKEN = "..infrastructure.token..";
    private static final String CRYPTO = "..infrastructure.crypto..";
    private static final String MAIL = "..infrastructure.mail..";
    private static final String PROJECT_DOMAIN = "com.postech.restaurantes.domain..";
    private static final String PROJECT_DOMAIN_EXCEPTION = "com.postech.restaurantes.domain.exception..";

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
                    .should().dependOnClassesThat().resideInAnyPackage(MAIN, API, TOKEN, CRYPTO, MAIL)
                    .because("trocar JPA não pode obrigar a mexer em segurança, token ou e-mail — o autor "
                            + "da auditoria chega pronto, ligado em main");

    @ArchTest
    static final ArchRule criptografia_nao_conhece_outros_modulos =
            noClasses().that().resideInAPackage(CRYPTO)
                    .should().dependOnClassesThat().resideInAnyPackage(MAIN, API, PERSISTENCE, TOKEN, MAIL);

    @ArchTest
    static final ArchRule email_nao_conhece_outros_modulos =
            noClasses().that().resideInAPackage(MAIL)
                    .should().dependOnClassesThat().resideInAnyPackage(MAIN, API, PERSISTENCE, TOKEN, CRYPTO)
                    .because("a validade do token chega pela porta IMailGateway, não por configuração alheia");

    /**
     * O módulo de token implementa a porta que a API declarou para ler o Bearer — e só isso ele
     * pode conhecer de lá: a interface e o tipo que ela devolve.
     */
    @ArchTest
    static final ArchRule token_so_conhece_a_porta_que_implementa =
            noClasses().that().resideInAPackage(TOKEN)
                    .should().dependOnClassesThat(resideInAnyPackage(MAIN, PERSISTENCE, CRYPTO, MAIL)
                            .or(resideInAPackage(API).and(not(belongToAnyOf(IAccessTokenReader.class,
                                    AuthenticatedUser.class)))))
                    .because("trocar o formato do token é criar outro subpacote de token que implemente "
                            + "ITokenIssuer e IAccessTokenReader");

    @ArchTest
    static final ArchRule api_nao_conhece_implementacoes =
            noClasses().that().resideInAPackage(API)
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
    static final ArchRule spring_security_so_na_api =
            noClasses().that().resideOutsideOfPackages(API, CRYPTO)
                    .should().dependOnClassesThat().resideInAPackage("org.springframework.security..");

    @ArchTest
    static final ArchRule criptografia_usa_so_o_modulo_crypto_do_spring_security =
            noClasses().that().resideInAPackage(CRYPTO)
                    .should().dependOnClassesThat(resideInAPackage("org.springframework.security..")
                            .and(not(resideInAPackage("org.springframework.security.crypto.."))));

    @ArchTest
    static final ArchRule http_e_documentacao_so_na_api =
            noClasses().that().resideOutsideOfPackage(API)
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework.web..", "org.springframework.hateoas..", "jakarta.servlet..",
                            "jakarta.validation..", "io.swagger..", "org.springdoc..");

    // ---- Gateway é o tradutor, no adaptador (Aulas 02, 05, 06) --------------------------------

    /**
     * A infraestrutura só conhece, do núcleo, as portas técnicas — em que não há tradução: hash de
     * senha, geração de token seguro, unidade de trabalho. Toda outra porta passa por um gateway em
     * {@code adapter.gateway}, que traduz e consome a infraestrutura por interface
     * ({@code adapter.datasource} ou {@code adapter.service}) — é onde mora o texto do e-mail e a
     * escolha dos claims do token, e por isso trocar SMTP ou JWT não os reescreve.
     *
     * <p>A regra proíbe <em>depender</em>, e não só implementar: uma porta de um método só pode ser
     * implementada por uma lambda num {@code @Bean}, e lambda não é classe para o ArchUnit — mas o
     * tipo de retorno do método é uma dependência, e ela aparece.
     */
    @ArchTest
    static final ArchRule infraestrutura_so_conhece_portas_tecnicas =
            noClasses().that().resideInAPackage("..infrastructure..")
                    .should().dependOnClassesThat(resideInAPackage("..application.gateway..")
                            .and(not(belongToAnyOf(IPasswordEncoder.class, ISecureTokenGenerator.class,
                                    IUnitOfWork.class))))
                    .because("porta com tradução é implementada por um gateway no adaptador, que o controller "
                            + "instancia com o serviço externo recebido por interface");

    /**
     * E-mail e token só transportam e codificam: o que a mensagem diz e quem é o portador foram
     * decididos pelo gateway. Um módulo desses que voltasse a importar {@code User} ou {@code Email}
     * estaria traduzindo de novo — e trocar a tecnologia voltaria a reescrever a regra.
     *
     * <p>É mais estrita que {@code infraestrutura_so_conhece_do_dominio_as_excecoes}: transporte não conhece
     * nem as exceções do domínio, porque não as lança nem as traduz — quem as traduz é a API.
     */
    @ArchTest
    static final ArchRule transporte_nao_conhece_o_dominio =
            noClasses().that().resideInAnyPackage("..infrastructure.mail..", "..infrastructure.token..")
                    .should().dependOnClassesThat().resideInAPackage(PROJECT_DOMAIN)
                    .because("a tradução do domínio para a mensagem e para os claims é do gateway no adaptador");

    /**
     * Do domínio, a infraestrutura só conhece as exceções — que o handler da API traduz em status. Tipo do
     * domínio fora disso significa tradução fora do gateway: uma entidade JPA com {@code CourierVehicleType}
     * ou uma resposta HTTP montada de uma entidade. Por isso a coluna {@code ENUM} do PostgreSQL é texto na
     * JPA ({@code @ColumnTransformer(write = "?::tipo")}), e o gateway converte (Etapas 21 e 24).
     */
    @ArchTest
    static final ArchRule infraestrutura_so_conhece_do_dominio_as_excecoes =
            noClasses().that().resideInAPackage("..infrastructure..")
                    .should().dependOnClassesThat(resideInAPackage(PROJECT_DOMAIN)
                            .and(not(resideInAPackage(PROJECT_DOMAIN_EXCEPTION))))
                    .because("o domínio chega à infraestrutura traduzido pelo gateway (registros *Data) e pelo "
                            + "presenter (views); só a exceção atravessa, para virar ProblemDetail");

    // ---- A API REST em Spring organizada como MVC (Etapa 15) ----------------------------------

    /**
     * Dentro do módulo, o pacote diz o papel da classe: quem procura um controller, um corpo de
     * requisição ou o tratamento de erros sabe onde olhar, e restaurante e cardápio entram nos mesmos
     * pacotes. As regras valem nos dois sentidos — o pacote só tem classes daquele papel, e a classe
     * daquele papel só existe naquele pacote.
     */
    @ArchTest
    static final ArchRule controllers_rest_ficam_em_controller =
            classes().that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .should().resideInAPackage(API_SPRING + ".controller")
                    .andShould().haveSimpleNameEndingWith("RestController");

    @ArchTest
    static final ArchRule pacote_controller_so_tem_controllers_rest =
            classes().that().resideInAPackage(API_SPRING + ".controller")
                    .and().doNotHaveSimpleName("package-info")
                    .should().beAnnotatedWith("org.springframework.web.bind.annotation.RestController");

    @ArchTest
    static final ArchRule dto_request_so_tem_records_Request =
            classes().that().resideInAPackage(API_SPRING + ".dto.request")
                    .and().doNotHaveSimpleName("package-info")
                    .should().beRecords()
                    .andShould().haveSimpleNameEndingWith("Request");

    @ArchTest
    static final ArchRule dto_response_so_tem_records_Response =
            classes().that().resideInAPackage(API_SPRING + ".dto.response")
                    .and().doNotHaveSimpleName("package-info")
                    .should().beRecords()
                    .andShould().haveSimpleNameEndingWith("Response");

    @ArchTest
    static final ArchRule corpos_http_ficam_em_dto =
            classes().that().resideInAPackage(API)
                    .and().areRecords()
                    .and().haveNameMatching(".*(Request|Response)")
                    .should().resideInAnyPackage(API_SPRING + ".dto.request", API_SPRING + ".dto.response");

    @ArchTest
    static final ArchRule tratamento_de_erros_fica_em_exception =
            classes().that().areAnnotatedWith("org.springframework.web.bind.annotation.RestControllerAdvice")
                    .should().resideInAPackage(API_SPRING + ".exception");

    @ArchTest
    static final ArchRule pacote_assembler_so_tem_assemblers =
            classes().that().resideInAPackage(API_SPRING + ".assembler")
                    .and().doNotHaveSimpleName("package-info")
                    .should().haveSimpleNameEndingWith("Assembler");

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
