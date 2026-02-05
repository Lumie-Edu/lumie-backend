package com.lumie.academy.adapter.out.persistence;

import com.lumie.academy.domain.entity.Student;
import org.springframework.data.jpa.domain.Specification;

public class StudentSpecification {

    public static Specification<Student> hasAcademyId(Long academyId) {
        return (root, query, cb) -> {
            if (academyId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("academy").get("id"), academyId);
        };
    }

    public static Specification<Student> hasIsActive(Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isActive"), isActive);
        };
    }

    public static Specification<Student> searchByField(String search, String searchField) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }

            String field = searchField != null ? searchField : "name";
            String searchPattern = "%" + search.toLowerCase() + "%";

            return switch (field) {
                case "name" -> cb.like(cb.lower(root.get("name")), searchPattern);
                case "studentHighschool" -> cb.like(cb.lower(root.get("studentHighschool")), searchPattern);
                case "studentBirthYear" -> cb.like(
                        cb.toString(root.get("studentBirthYear")),
                        "%" + search + "%"
                );
                case "phone" -> cb.like(root.get("phone"), "%" + search + "%");
                default -> cb.like(cb.lower(root.get("name")), searchPattern);
            };
        };
    }
}
