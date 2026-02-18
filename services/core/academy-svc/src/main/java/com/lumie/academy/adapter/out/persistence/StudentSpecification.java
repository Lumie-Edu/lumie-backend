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

    public static Specification<Student> searchByKeyword(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }

            String pattern = "%" + search.toLowerCase() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("studentHighschool")), pattern),
                    cb.like(root.get("phone"), "%" + search + "%"),
                    cb.like(cb.toString(root.get("studentBirthYear")), "%" + search + "%")
            );
        };
    }
}
