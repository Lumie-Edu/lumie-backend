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
@Table(name = "academies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Academy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "business_number", length = 20)
    private String businessNumber;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @OneToMany(mappedBy = "academy", fetch = FetchType.LAZY)
    private List<Student> students = new ArrayList<>();

    @Builder
    private Academy(String name, String description, String address, String phone,
                   String email, String businessNumber, boolean isDefault) {
        this.name = name;
        this.description = description;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.businessNumber = businessNumber;
        this.isDefault = isDefault;
        this.status = "ACTIVE";
    }

    public static Academy create(String name, String address, String phone) {
        return Academy.builder()
                .name(name)
                .address(address)
                .phone(phone)
                .build();
    }

    public void updateInfo(String name, String description, String address,
                          String phone, String email, String businessNumber) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
        if (address != null) {
            this.address = address;
        }
        if (phone != null) {
            this.phone = phone;
        }
        if (email != null) {
            this.email = email;
        }
        if (businessNumber != null) {
            this.businessNumber = businessNumber;
        }
    }

    public void deactivate() {
        this.status = "INACTIVE";
    }

    public void activate() {
        this.status = "ACTIVE";
    }

    public boolean isActive() {
        return "ACTIVE".equals(this.status);
    }
}
