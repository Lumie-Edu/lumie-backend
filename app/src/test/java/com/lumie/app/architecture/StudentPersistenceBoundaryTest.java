package com.lumie.app.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class StudentPersistenceBoundaryTest {

    @Test
    void studentApplicationAndInboundAdaptersDoNotDependOnPersistenceTypes() {
        var studentClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.lumie.student");

        noClasses()
                .that().resideInAnyPackage(
                        "com.lumie.student.application..",
                        "com.lumie.student.adapter.in..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.lumie.student.domain.repository..",
                        "com.lumie.student.adapter.out.persistence..")
                .check(studentClasses);
    }
}
