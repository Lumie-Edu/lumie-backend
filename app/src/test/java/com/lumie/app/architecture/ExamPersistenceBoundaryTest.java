package com.lumie.app.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** Guards the exam application's dependency direction at its persistence boundary. */
@AnalyzeClasses(packages = "com.lumie")
public class ExamPersistenceBoundaryTest {

    @ArchTest
    public static final ArchRule examApplicationAndInboundAdaptersUsePersistencePorts = noClasses()
            .that().resideInAnyPackage("com.lumie.exam.application..", "com.lumie.exam.adapter.in..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.lumie.exam.domain.repository..",
                    "com.lumie.exam.adapter.out.persistence..");
}
