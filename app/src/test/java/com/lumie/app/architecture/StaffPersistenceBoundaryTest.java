package com.lumie.app.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.lumie.staff")
public class StaffPersistenceBoundaryTest {

    @ArchTest
    static final ArchRule applicationAndInboundAdaptersMustNotDependOnDomainRepositories = noClasses()
            .that().resideInAnyPackage("com.lumie.staff.application..", "com.lumie.staff.adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("com.lumie.staff.domain.repository..");
}
