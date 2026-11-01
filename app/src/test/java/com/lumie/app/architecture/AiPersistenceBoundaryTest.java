package com.lumie.app.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class AiPersistenceBoundaryTest {

    private static final String DOMAIN_REPOSITORY_PACKAGE = "com.lumie.ai.domain.repository";

    @Test
    void aiApplicationAndInboundAdaptersDoNotDependOnDomainRepositories() {
        JavaClasses classes = new ClassFileImporter().importPackages("com.lumie.ai");

        noClasses()
                .that().resideInAnyPackage("com.lumie.ai.application..", "com.lumie.ai.adapter.in..")
                .should().dependOnClassesThat().resideInAPackage(DOMAIN_REPOSITORY_PACKAGE)
                .check(classes);
    }
}
