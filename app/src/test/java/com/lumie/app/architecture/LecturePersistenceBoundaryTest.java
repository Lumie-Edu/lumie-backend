package com.lumie.app.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class LecturePersistenceBoundaryTest {

    @Test
    void lectureApplicationAndInboundAdaptersMustNotDependOnDomainRepositories() {
        JavaClasses classes = new ClassFileImporter().importPackages("com.lumie.lecture");
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(
                        "com.lumie.lecture.application..",
                        "com.lumie.lecture.adapter.in.."
                )
                .should().dependOnClassesThat()
                .resideInAPackage("com.lumie.lecture.domain.repository..");

        rule.check(classes);
    }
}
