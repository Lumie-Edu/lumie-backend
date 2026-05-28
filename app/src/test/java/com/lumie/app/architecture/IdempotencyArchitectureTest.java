package com.lumie.app.architecture;

import com.lumie.common.idempotency.IdempotencyService;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Compile-time guard for the Lumie idempotency contract.
 *
 * <p>Phase E established that every state-mutating request under {@code /v1/**}
 * must (a) carry an {@code Idempotency-Key} header (enforced at runtime by
 * {@code IdempotencyHeaderInterceptor}) and (b) route the handler body through
 * {@link IdempotencyService#executeOnce} so retries are deduped. This test
 * pins the second half: any controller class declaring a POST/PUT/PATCH/DELETE
 * handler under any non-webhook adapter must declare a dependency on
 * {@link IdempotencyService}.
 *
 * <p>Webhook controllers are exempt — PG-managed retry semantics replace
 * client-driven idempotency keys.
 */
@AnalyzeClasses(packages = "com.lumie")
public class IdempotencyArchitectureTest {

    private static final Set<Class<?>> MUTATION_ANNOTATIONS = Set.of(
            PostMapping.class, PutMapping.class, PatchMapping.class, DeleteMapping.class
    );

    /**
     * Phase E scope: billing module only. Other modules (attendance, lecture,
     * assignment, etc.) mutate domain state that is not a financial transaction
     * — they may adopt the same pattern later but the urgency that justified
     * Phase E (double-charge / duplicate billing-key issuance) is specific to
     * the billing surface. Widen this rule's package when we extend the
     * universal-idempotency policy to other modules.
     */
    @ArchTest
    public static final ArchRule billingMutationControllersMustWireIdempotencyService =
            classes()
                    .that().resideInAPackage("com.lumie.billing.adapter.in.web..")
                    .and().haveSimpleNameEndingWith("Controller")
                    .and(haveAnyMutationEndpoint())
                    .and(isNotWebhookController())
                    .should().accessClassesThat().areAssignableTo(IdempotencyService.class)
                    .as("Every billing /v1 mutation controller (POST/PUT/PATCH/DELETE) must wire "
                            + "IdempotencyService and route through executeOnce. "
                            + "Phase E contract — see lumie-subscription-edge-cases.md §3.1.");

    private static DescribedPredicate<JavaClass> haveAnyMutationEndpoint() {
        return new DescribedPredicate<>("have any @PostMapping/@PutMapping/@PatchMapping/@DeleteMapping handler") {
            @Override
            public boolean test(JavaClass javaClass) {
                for (JavaMethod method : javaClass.getMethods()) {
                    for (Class<?> ann : MUTATION_ANNOTATIONS) {
                        if (method.isAnnotatedWith(ann.getName())) {
                            return true;
                        }
                    }
                    // @RequestMapping(method = RequestMethod.POST, ...) fallback
                    if (method.isAnnotatedWith(RequestMapping.class.getName())) {
                        RequestMapping rm = method.getAnnotationOfType(RequestMapping.class);
                        for (RequestMethod m : rm.method()) {
                            if (m == RequestMethod.POST || m == RequestMethod.PUT
                                    || m == RequestMethod.PATCH || m == RequestMethod.DELETE) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            }
        };
    }

    private static DescribedPredicate<JavaClass> isNotWebhookController() {
        return new DescribedPredicate<>("is not a webhook controller") {
            @Override
            public boolean test(JavaClass javaClass) {
                String name = javaClass.getSimpleName();
                String pkg = javaClass.getPackageName();
                return !name.contains("Webhook") && !pkg.contains("webhook");
            }
        };
    }
}
