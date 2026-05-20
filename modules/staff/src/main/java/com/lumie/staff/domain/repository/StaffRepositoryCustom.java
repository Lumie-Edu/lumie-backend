package com.lumie.staff.domain.repository;

import com.lumie.staff.domain.entity.Staff;
import com.lumie.staff.domain.vo.StaffFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StaffRepositoryCustom {

    Page<Staff> findAllByFilter(StaffFilter filter, Pageable pageable);
}
