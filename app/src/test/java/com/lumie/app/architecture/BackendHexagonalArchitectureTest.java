package com.lumie.app.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class BackendHexagonalArchitectureTest {

    private static final Set<String> MODULES = Set.of(
            "ai", "assignment", "auth", "billing", "classroom", "content", "exam",
            "file", "lecture", "notification", "staff", "student", "tenant");

    private static final Set<String> DEFERRED_REPOSITORIES = Set.of(
            "com.lumie.notification.domain.repository.SmsMessageRepository",
            "com.lumie.notification.domain.repository.SmsTemplateRepository");

    // Exact existing dependencies: new ones fail; removed ones must be deleted here.
    private static final Set<DependencyPair> EXISTING_LAYER_EXCEPTIONS = Set.of(
            pair("com.lumie.auth.application.service.LoginSessionHelper",
                    "com.lumie.auth.adapter.out.config.SessionConfig"),
            pair("com.lumie.auth.adapter.in.security.SameOriginCsrfFilter",
                    "com.lumie.auth.adapter.out.config.SessionConfig"),
            pair("com.lumie.auth.adapter.in.security.SessionAuthenticationFilter",
                    "com.lumie.auth.adapter.out.config.SessionConfig"),
            pair("com.lumie.auth.adapter.in.security.SameOriginPolicy",
                    "com.lumie.auth.adapter.out.config.SessionConfig"),
            pair("com.lumie.auth.adapter.in.web.CookieUtils",
                    "com.lumie.auth.adapter.out.config.SessionConfig"),
            pair("com.lumie.auth.adapter.in.web.AuthController",
                    "com.lumie.auth.adapter.out.config.SessionConfig"),
            pair("com.lumie.notification.application.service.SmsDispatchService",
                    "com.lumie.notification.adapter.out.config.SmsProperties"),
            pair("com.lumie.notification.application.service.SmsSenderSettingService",
                    "com.lumie.notification.adapter.out.config.SmsProperties"),
            pair("com.lumie.notification.application.service.SmsProviderRegistry",
                    "com.lumie.notification.adapter.out.config.SmsProperties"),
            pair("com.lumie.notification.application.service.SmsWebhookService",
                    "com.lumie.notification.adapter.out.config.SmsProperties"),
            pair("com.lumie.notification.application.service.SmsWebhookService",
                    "com.lumie.notification.adapter.out.config.SmsProperties$Solapi"),
            pair("com.lumie.ai.adapter.in.web.ChatController",
                    "com.lumie.ai.adapter.out.external.ChatbotClient"),
            pair("com.lumie.exam.adapter.in.web.JoossamengAiReportController",
                    "com.lumie.exam.adapter.out.external.JoossamengAiReportClient"),
            pair("com.lumie.notification.application.service.SmsQueryService",
                    "com.lumie.notification.domain.repository.SmsMessageRepository"),
            pair("com.lumie.notification.application.service.SmsQueryService",
                    "com.lumie.notification.domain.repository.SmsTemplateRepository"),
            pair("com.lumie.notification.application.service.SmsCommandService",
                    "com.lumie.notification.domain.repository.SmsMessageRepository"),
            pair("com.lumie.notification.application.service.SmsCommandService",
                    "com.lumie.notification.domain.repository.SmsTemplateRepository"),
            pair("com.lumie.notification.application.service.SmsDispatchService",
                    "com.lumie.notification.domain.repository.SmsMessageRepository"));

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.lumie");
    private static final Pattern PACKAGE_SEPARATOR = Pattern.compile("\\.");

    @Test
    void everyCurrentModuleIsInTheArchitectureScan() {
        Set<String> scanned = CLASSES.stream()
                .map(JavaClass::getPackageName)
                .map(BackendHexagonalArchitectureTest::moduleOf)
                .filter(module -> module != null)
                .collect(Collectors.toSet());

        assertThat(scanned).containsExactlyInAnyOrderElementsOf(MODULES);
    }

    @Test
    void modulesUseOnlySharedApiForCrossModuleDependencies() {
        Set<String> violations = CLASSES.stream()
                .filter(type -> moduleOf(type.getPackageName()) != null)
                .flatMap(type -> type.getDirectDependenciesFromSelf().stream())
                .filter(dependency -> {
                    String origin = moduleOf(dependency.getOriginClass().getPackageName());
                    String target = moduleOf(dependency.getTargetClass().getPackageName());
                    return target != null && !target.equals(origin);
                })
                .map(dependency -> dependency.getOriginClass().getName()
                        + " -> " + dependency.getTargetClass().getName())
                .collect(Collectors.toCollection(TreeSet::new));

        assertThat(violations)
                .as("Cross-module code must use contracts from libs/internal-api, not private implementation packages")
                .isEmpty();
    }

    @Test
    void domainNeverPointsUpAndOtherLayerExceptionsAreExact() {
        Set<DependencyPair> observed = CLASSES.stream()
                .filter(type -> moduleOf(type.getPackageName()) != null)
                .flatMap(type -> type.getDirectDependenciesFromSelf().stream())
                .filter(dependency -> violatesLayerDirection(
                        dependency.getOriginClass().getPackageName(),
                        dependency.getTargetClass().getPackageName()))
                .map(dependency -> pair(dependency.getOriginClass().getName(),
                        dependency.getTargetClass().getName()))
                .collect(Collectors.toSet());

        assertThat(observed)
                .as("Only the named baseline dependencies may cross the hexagonal layers")
                .containsExactlyInAnyOrderElementsOf(EXISTING_LAYER_EXCEPTIONS);
    }

    @Test
    void springDataRepositoriesStayInOutboundPersistenceExceptDeferredSmsTypes() {
        Set<String> domainRepositories = CLASSES.stream()
                .filter(type -> moduleOf(type.getPackageName()) != null)
                .filter(type -> isInPackage(type.getPackageName(), ".domain.repository"))
                .map(JavaClass::getName)
                .collect(Collectors.toSet());
        assertThat(domainRepositories).containsExactlyInAnyOrderElementsOf(DEFERRED_REPOSITORIES);

        Set<String> misplaced = CLASSES.stream()
                .filter(type -> moduleOf(type.getPackageName()) != null)
                .filter(type -> type.isInterface() && type.isAssignableTo(Repository.class))
                .filter(type -> !DEFERRED_REPOSITORIES.contains(type.getName()))
                .filter(type -> !isInPackage(type.getPackageName(), ".adapter.out.persistence"))
                .map(JavaClass::getName)
                .collect(Collectors.toCollection(TreeSet::new));

        assertThat(misplaced)
                .as("Spring Data repository interfaces belong to adapter/out/persistence")
                .isEmpty();
    }

    @Test
    void domainHasNoSpringRuntimeAnnotations() {
        Set<String> annotations = CLASSES.stream()
                .filter(type -> "domain".equals(layerOf(type.getPackageName())))
                .flatMap(type -> java.util.stream.Stream.concat(
                        type.getAnnotations().stream()
                                .map(annotation -> type.getName() + " -> " + annotation.getRawType().getName()),
                        type.getAllMembers().stream()
                                .flatMap(member -> member.getAnnotations().stream()
                                        .map(annotation -> member.getOwner().getName() + "#" + member.getName() + " -> "
                                                + annotation.getRawType().getName()))))
                .filter(annotation -> annotation.contains(" -> org.springframework."))
                .filter(annotation -> annotation.contains(".stereotype.")
                        || annotation.contains(".beans.factory.annotation.")
                        || annotation.contains(".context.annotation.")
                        || annotation.contains(".transaction.annotation.")
                        || annotation.contains(".web.bind.annotation.")
                        || annotation.contains(".amqp.")
                        || annotation.contains(".scheduling.annotation.")
                        || annotation.contains(".security.access."))
                .collect(Collectors.toCollection(TreeSet::new));

        assertThat(annotations)
                .as("Domain objects may keep JPA mapping but not Spring service/runtime annotations")
                .isEmpty();
    }

    @Test
    void dependencyClassifierRejectsRepresentativeForbiddenDirections() {
        assertThat(moduleOf("com.lumie.future.application.service")).isEqualTo("future");
        assertThat(violatesLayerDirection("com.lumie.exam.domain.entity",
                "com.lumie.exam.application.service")).isTrue();
        assertThat(violatesLayerDirection("com.lumie.exam.application.service",
                "com.lumie.exam.adapter.out.persistence")).isTrue();
        assertThat(violatesLayerDirection("com.lumie.exam.adapter.in.web",
                "com.lumie.exam.adapter.out.external")).isTrue();
        assertThat(violatesLayerDirection("com.lumie.exam.application.service",
                "com.lumie.exam.domain.repository")).isTrue();
        assertThat(violatesLayerDirection("com.lumie.exam.application.service",
                "com.lumie.exam.domain.repository.custom")).isTrue();
        assertThat(violatesLayerDirection("com.lumie.exam.adapter.in",
                "com.lumie.exam.adapter.out")).isTrue();
        assertThat(violatesLayerDirection("com.lumie.exam.application.service",
                "com.lumie.exam.domain.entity")).isFalse();
        assertThat(violatesLayerDirection("com.lumie.exam.adapter.out.persistence",
                "com.lumie.exam.application.port.out")).isFalse();
    }

    private static boolean violatesLayerDirection(String originPackage, String targetPackage) {
        String origin = moduleOf(originPackage);
        if (origin == null || !origin.equals(moduleOf(targetPackage))) {
            return false;
        }
        String layer = layerOf(originPackage);
        String targetLayer = layerOf(targetPackage);
        if ("domain".equals(layer)) {
            return "application".equals(targetLayer) || "adapter".equals(targetLayer);
        }
        if ("application".equals(layer)) {
            return "adapter".equals(targetLayer)
                    || isInPackage(targetPackage, ".domain.repository");
        }
        return isInPackage(originPackage, ".adapter.in")
                && (isInPackage(targetPackage, ".adapter.out")
                || isInPackage(targetPackage, ".domain.repository"));
    }

    private static boolean isInPackage(String packageName, String suffix) {
        return packageName.endsWith(suffix) || packageName.contains(suffix + ".");
    }

    private static String moduleOf(String packageName) {
        String[] parts = PACKAGE_SEPARATOR.split(packageName, -1);
        if (parts.length < 4 || !parts[0].equals("com") || !parts[1].equals("lumie")
                || parts[2].equals("app") || parts[2].equals("common")
                || layerOf(packageName) == null) {
            return null;
        }
        return parts[2];
    }

    private static String layerOf(String packageName) {
        String[] parts = PACKAGE_SEPARATOR.split(packageName, -1);
        if (parts.length < 4) {
            return null;
        }
        return switch (parts[3]) {
            case "domain", "application", "adapter" -> parts[3];
            default -> null;
        };
    }

    private static DependencyPair pair(String origin, String target) {
        return new DependencyPair(origin, target);
    }

    private record DependencyPair(String origin, String target) {
    }
}
