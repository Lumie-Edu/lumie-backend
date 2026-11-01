package com.lumie.exam.adapter.out.persistence;

import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeastOnce;

/** Reflection-backed parity assertion for the thin port-to-Spring-Data adapters. */
final class PersistenceAdapterContractAssertions {

    private PersistenceAdapterContractAssertions() {
    }

    static void everyPortOperationDelegates(Class<?> portType, Class<?> jpaRepositoryType,
            Object adapter, Object jpaRepository) throws Exception {
        for (Method portMethod : portType.getDeclaredMethods()) {
            if (!Modifier.isPublic(portMethod.getModifiers()) || Modifier.isStatic(portMethod.getModifiers())
                    || portMethod.isSynthetic()) {
                continue;
            }
            Object[] arguments = argumentsFor(portMethod);
            portMethod.invoke(adapter, arguments);
            if (!portMethod.isDefault()) {
                Method repositoryMethod = repositoryMethod(jpaRepositoryType, portMethod);
                repositoryMethod.invoke(verify(jpaRepository, atLeastOnce()), arguments);
            }
        }
    }

    private static Method repositoryMethod(Class<?> repositoryType, Method portMethod) throws NoSuchMethodException {
        try {
            return repositoryType.getMethod(portMethod.getName(), portMethod.getParameterTypes());
        } catch (NoSuchMethodException ignored) {
            for (Method candidate : repositoryType.getMethods()) {
                if (!candidate.getName().equals(portMethod.getName())
                        || candidate.getParameterCount() != portMethod.getParameterCount()) {
                    continue;
                }
                Class<?>[] repositoryParameters = candidate.getParameterTypes();
                Class<?>[] portParameters = portMethod.getParameterTypes();
                boolean compatible = true;
                for (int index = 0; index < repositoryParameters.length; index++) {
                    if (repositoryParameters[index] != Object.class
                            && !repositoryParameters[index].isAssignableFrom(portParameters[index])) {
                        compatible = false;
                        break;
                    }
                }
                if (compatible) {
                    return candidate;
                }
            }
            throw ignored;
        }
    }

    private static Object[] argumentsFor(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] arguments = new Object[parameterTypes.length];
        for (int index = 0; index < parameterTypes.length; index++) {
            arguments[index] = argumentFor(parameterTypes[index]);
        }
        return arguments;
    }

    private static Object argumentFor(Class<?> type) {
        if (type == Long.class || type == long.class) {
            return 1L;
        }
        if (type == Integer.class || type == int.class) {
            return 1;
        }
        if (type == String.class) {
            return "test";
        }
        if (type == Class.class) {
            return Object.class;
        }
        if (type == Instant.class) {
            return Instant.parse("2026-11-01T00:00:00Z");
        }
        if (type == Collection.class || type == List.class || type == Iterable.class) {
            return List.of();
        }
        if (type == org.springframework.data.domain.Pageable.class) {
            return PageRequest.of(0, 1);
        }
        if (type.isEnum()) {
            return type.getEnumConstants()[0];
        }
        return org.mockito.Mockito.mock(type);
    }
}
