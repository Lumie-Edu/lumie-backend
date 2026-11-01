package com.lumie.app.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ModulePersistenceBoundaryTest {

    private static final Map<String, Integer> IN_SCOPE_REPOSITORIES = Map.of(
            "ai", 2,
            "assignment", 3,
            "classroom", 2,
            "content", 7,
            "exam", 7,
            "file", 4,
            "lecture", 2,
            "staff", 3,
            "student", 1
    );

    @Test
    void allThirtyOneRepositoryInterfacesBelongToOutboundPersistence() {
        var classes = new ClassFileImporter().importPackages("com.lumie");
        assertThat(IN_SCOPE_REPOSITORIES.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(31);

        IN_SCOPE_REPOSITORIES.forEach((module, expected) -> {
            String root = "com.lumie." + module + ".";
            assertThat(classes.stream()
                    .filter(type -> type.getPackageName().equals(root + "domain.repository"))
                    .map(JavaClass::getName))
                    .as("%s legacy domain repository types", module)
                    .isEmpty();
            assertThat(classes.stream()
                    .filter(type -> type.getPackageName().equals(root + "adapter.out.persistence"))
                    .filter(type -> type.getSimpleName().endsWith("JpaRepository"))
                    .count())
                    .as("%s outbound Spring Data interfaces", module)
                    .isGreaterThanOrEqualTo(expected);
        });
    }

    @Test
    void applicationAndInboundCodeDoNotDependOnMigratedDomainRepositories() {
        var classes = new ClassFileImporter().importPackages("com.lumie");
        Set<String> modules = IN_SCOPE_REPOSITORIES.keySet();

        assertThat(classes.stream()
                .filter(type -> modules.stream().anyMatch(module ->
                        type.getPackageName().startsWith("com.lumie." + module + ".application.")
                                || type.getPackageName().startsWith("com.lumie." + module + ".adapter.in.")))
                .flatMap(type -> type.getDirectDependenciesFromSelf().stream())
                .filter(dependency -> modules.stream().anyMatch(module -> dependency.getTargetClass()
                        .getPackageName().equals("com.lumie." + module + ".domain.repository")))
                .map(Object::toString))
                .isEmpty();
    }

    @Test
    void tenantCleanupDoesNotTouchDeferredNotificationRepositories() {
        var classes = new ClassFileImporter().importPackages("com.lumie");
        assertThat(classes.stream()
                .filter(type -> type.getPackageName().equals("com.lumie.tenant.domain.repository"))
                .map(JavaClass::getName))
                .isEmpty();
        assertThat(classes.stream()
                .filter(type -> type.getPackageName().equals("com.lumie.notification.domain.repository"))
                .map(JavaClass::getSimpleName))
                .containsExactlyInAnyOrder("SmsMessageRepository", "SmsTemplateRepository");
    }
}
