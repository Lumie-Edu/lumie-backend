package com.lumie.academy.domain.entity;

import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "academies", uniqueConstraints = {
    @UniqueConstraint(columnNames = "name"),
    @UniqueConstraint(columnNames = "phone")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Academy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100, unique = true)
    private String name;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "phone", length = 20, unique = true)
    private String phone;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "academy", fetch = FetchType.LAZY)
    private List<Student> students = new ArrayList<>();

    @Builder
    private Academy(String name, String address, String phone, Boolean isActive) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.isActive = isActive != null ? isActive : true;
    }

    public static Academy create(String name, String address, String phone) {
        return Academy.builder()
                .name(name)
                .address(address)
                .phone(phone)
                .isActive(true)
                .build();
    }

    public void updateInfo(String name, String address, String phone) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (address != null) {
            this.address = address;
        }
        if (phone != null) {
            this.phone = phone;
        }
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(this.isActive);
    }
}
