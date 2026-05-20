package com.lumie.admin.domain.repository;

import com.lumie.admin.domain.entity.Admin;
import com.lumie.admin.domain.vo.AdminFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminRepositoryCustom {

    Page<Admin> findAllByFilter(AdminFilter filter, Pageable pageable);
}
