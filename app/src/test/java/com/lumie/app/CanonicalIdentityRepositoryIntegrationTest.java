package com.lumie.app;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.staff.domain.repository.StaffRepository;
import com.lumie.student.domain.repository.StudentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercises Hibernate @Formula queries against Postgres while the legacy columns remain. */
class CanonicalIdentityRepositoryIntegrationTest extends FullContextTestBase {
    private final StudentRepository students;
    private final StaffRepository staff;
    private final JdbcTemplate jdbc;

    CanonicalIdentityRepositoryIntegrationTest(
            @Autowired StudentRepository students,
            @Autowired StaffRepository staff,
            @Autowired @Qualifier("primaryDataSource") DataSource primaryDataSource) {
        this.students = students;
        this.staff = staff;
        this.jdbc = new JdbcTemplate(primaryDataSource);
    }

    @BeforeEach
    void prepare() {
        jdbc.execute("""
                create table if not exists users (
                    id bigint primary key, name varchar(100), phone varchar(20),
                    email varchar(255), role varchar(20))
                """);
        jdbc.execute("""
                create table if not exists students (
                    id bigint primary key, user_id bigint, user_login_id varchar(50),
                    name varchar(100), phone varchar(20), student_highschool varchar(100),
                    student_birth_year integer, parent_phone varchar(20), student_memo text,
                    is_active boolean, version bigint, tenant_id bigint,
                    created_at timestamptz, updated_at timestamptz)
                """);
        jdbc.execute("""
                create table if not exists staff (
                    id bigint primary key, user_id bigint, user_login_id varchar(50),
                    name varchar(100), phone varchar(20), email varchar(255),
                    is_active boolean, staff_memo text, employment_status varchar(20),
                    version bigint, tenant_id bigint, created_at timestamptz, updated_at timestamptz)
                """);
        jdbc.update("insert into users(id, name, phone, email, role) values (901, ?, ?, ?, ?)",
                "User Student", "01012345678", null, "STUDENT");
        jdbc.update("insert into users(id, name, phone, email, role) values (902, ?, ?, ?, ?)",
                "User Staff", "01087654321", "user@example.com", "INSTRUCTOR");
        jdbc.update("""
                insert into students(id, user_id, user_login_id, name, phone, is_active, version, tenant_id,
                                     created_at, updated_at)
                values (911, 901, 'student_901', 'Old Student', '01099999999', true, 0, 1, now(), now())
                """);
        jdbc.update("""
                insert into staff(id, user_id, user_login_id, name, phone, email, is_active,
                                  employment_status, version, tenant_id, created_at, updated_at)
                values (912, 902, 'staff_902', 'Old Staff', '01099998888', 'old@example.com',
                        true, 'ACTIVE', 0, 1, now(), now())
                """);
        TenantContextHolder.setTenant("inst-test");
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void clear() {
        TenantContextHolder.clear();
        jdbc.update("delete from staff where id = 912");
        jdbc.update("delete from students where id = 911");
        jdbc.update("delete from users where id in (901, 902)");
    }

    @Test
    void phoneLookupAndTextSearchReadTheUserWhileOldColumnsStayIntact() {
        var student = students.findByCanonicalPhone("01012345678").orElseThrow();
        assertThat(student.getStudentName()).isEqualTo("User Student");
        assertThat(student.getStudentPhone()).isEqualTo("01012345678");
        assertThat(students.existsByCanonicalPhone("01012345678")).isTrue();
        assertThat(students.search(null, "User Student", null, null, PageRequest.of(0, 10)))
                .hasSize(1);

        var teacher = staff.findByUserId(902L).orElseThrow();
        assertThat(teacher.getStaffName()).isEqualTo("User Staff");
        assertThat(teacher.getStaffPhone()).isEqualTo("01087654321");
        assertThat(teacher.getEmail()).isEqualTo("user@example.com");
        assertThat(staff.existsByCanonicalPhone("01087654321")).isTrue();
        assertThat(staff.search(null, "user@example.com", PageRequest.of(0, 10)))
                .hasSize(1);
    }
}
