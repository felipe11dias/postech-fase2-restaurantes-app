package com.postech.restaurantes;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

/**
 * Regra de dependência da Clean Architecture, verificada em build.
 *
 * <p>O código só pode apontar para dentro: {@code infrastructure} → {@code adapter} →
 * {@code application} → {@code domain}. Nenhum tipo fora de {@code infrastructure} pode
 * conhecer Spring, JPA ou Hibernate.
 *
 * <p>{@code allowEmptyShould(true)} permite que as regras existam antes das classes: os
 * pacotes nascem vazios na Etapa 1 e vão sendo preenchidos etapa a etapa.
 */
@AnalyzeClasses(
        packages = "com.postech.restaurantes",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String DOMAIN = "..domain..";
    private static final String APPLICATION = "..application..";
    private static final String ADAPTER = "..adapter..";
    private static final String INFRASTRUCTURE = "..infrastructure..";
    private static final String PACKAGE_INFO = "package-info";

    private static final String[] FRAMEWORK_PACKAGES = {
        "org.springframework..",
        "jakarta.persistence..",
        "org.hibernate..",
        "org.springdoc..",
        "io.jsonwebtoken..",
        "jakarta.validation..",
        "jakarta.mail..",
        "org.flywaydb.."
    };

    @ArchTest
    static final ArchRule camadas_concentricas_so_apontam_para_dentro =
            Architectures.onionArchitecture()
                    .domainModels("..domain..")
                    .domainServices("..domain..")
                    .applicationServices(APPLICATION)
                    .adapter("adapter", ADAPTER)
                    .adapter("infrastructure", INFRASTRUCTURE)
                    .withOptionalLayers(true)
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_nao_depende_de_nenhum_outro_pacote_do_projeto =
            noClasses().that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, ADAPTER, INFRASTRUCTURE)
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule application_depende_apenas_de_domain =
            noClasses().that().resideInAPackage(APPLICATION)
                    .should().dependOnClassesThat().resideInAnyPackage(ADAPTER, INFRASTRUCTURE)
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule adapter_nao_depende_de_infrastructure =
            noClasses().that().resideInAPackage(ADAPTER)
                    .should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE)
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule frameworks_existem_apenas_em_infrastructure =
            noClasses().that().resideOutsideOfPackages(INFRASTRUCTURE, "com.postech.restaurantes")
                    .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORK_PACKAGES)
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule casos_de_uso_terminam_em_UseCase =
            classes().that().resideInAPackage("..application.usecase..")
                    .and().areNotInterfaces()
                    .and().doNotHaveSimpleName(PACKAGE_INFO)
                    .should().haveSimpleNameEndingWith("UseCase")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule gateways_do_nucleo_sao_interfaces_com_prefixo_I =
            classes().that().resideInAPackage("..application.gateway..")
                    .and().doNotHaveSimpleName(PACKAGE_INFO)
                    .should().beInterfaces()
                    .andShould().haveSimpleNameStartingWith("I")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule origens_de_dados_sao_interfaces_com_prefixo_I =
            classes().that().resideInAPackage("..adapter.datasource")
                    .and().doNotHaveSimpleName(PACKAGE_INFO)
                    .should().beInterfaces()
                    .andShould().haveSimpleNameStartingWith("I")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule records_de_origem_de_dados_sao_records_com_sufixo_Data =
            classes().that().resideInAPackage("..adapter.datasource.data")
                    .and().doNotHaveSimpleName(PACKAGE_INFO)
                    .should().beRecords()
                    .andShould().haveSimpleNameEndingWith("Data")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule views_sao_records_com_sufixo_View =
            classes().that().resideInAPackage("..adapter.presenter.view")
                    .and().doNotHaveSimpleName(PACKAGE_INFO)
                    .should().beRecords()
                    .andShould().haveSimpleNameEndingWith("View")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule gateways_do_adapter_implementam_uma_porta_do_nucleo =
            classes().that().resideInAPackage("..adapter.gateway")
                    .and().doNotHaveSimpleName(PACKAGE_INFO)
                    .should().implement(com.tngtech.archunit.base.DescribedPredicate.describe(
                            "interface em application.gateway",
                            (com.tngtech.archunit.core.domain.JavaClass c) ->
                                    c.getPackageName().endsWith("application.gateway")))
                    .allowEmptyShould(true);
}
