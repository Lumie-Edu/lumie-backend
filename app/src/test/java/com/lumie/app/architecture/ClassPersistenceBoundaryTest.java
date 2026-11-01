package com.lumie.app.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.lumie", importOptions = ImportOption.DoNotIncludeTests.class)
class ClassPersistenceBoundaryTest {

    @ArchTest
    static final ArchRule classApplicationAndInboundAdaptersMustUsePersistencePorts = noClasses()
            .that().resideInAnyPackage(
                    "com.lumie.classroom.application..",
                    "com.lumie.classroom.adapter.in..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.lumie.classroom.domain.repository..",
                    "com.lumie.classroom.adapter.out.persistence..")
            .as("Class application and inbound adapters depend on application ports, not persistence adapters");
}
