package com.lumie.app.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ContentPersistenceBoundaryTest {

    @Test
    void contentApplicationAndInboundAdaptersDoNotDependOnDomainRepositoriesOrPersistenceAdapters() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.lumie.content");

        noClasses()
                .that().resideInAnyPackage("com.lumie.content.application..", "com.lumie.content.adapter.in..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.lumie.content.domain.repository..", "com.lumie.content.adapter.out.persistence..")
                .check(classes);
    }
}
