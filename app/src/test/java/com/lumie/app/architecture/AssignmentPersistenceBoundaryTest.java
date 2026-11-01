package com.lumie.app.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.lumie.assignment", importOptions = ImportOption.DoNotIncludeTests.class)
class AssignmentPersistenceBoundaryTest {

    @ArchTest
    static final ArchRule assignmentApplicationAndInboundAdaptersMustNotDependOnDomainRepositories =
            noClasses()
                    .that().resideInAnyPackage("com.lumie.assignment.application..", "com.lumie.assignment.adapter.in..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("com.lumie.assignment.domain.repository..");
}
