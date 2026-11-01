package com.lumie.app.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.lumie.file")
class FilePersistenceBoundaryTest {
    @ArchTest
    static final ArchRule applicationAndInboundAdaptersMustUseApplicationPersistencePorts = noClasses()
            .that().resideInAnyPackage("com.lumie.file.application..", "com.lumie.file.adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("com.lumie.file.domain.repository..");

    @ArchTest
    static final ArchRule applicationAndInboundAdaptersMustNotDependOnPersistenceAdapters = noClasses()
            .that().resideInAnyPackage("com.lumie.file.application..", "com.lumie.file.adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("com.lumie.file.adapter.out.persistence..");
}
