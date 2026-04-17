package com.example.org.repository;

import com.example.org.entity.OrgDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrgDepartmentRepository extends JpaRepository<OrgDepartment, Long>, JpaSpecificationExecutor<OrgDepartment> {

    boolean existsByDepartmentCode(String departmentCode);

    boolean existsByDepartmentCodeAndIdNot(String departmentCode, Long id);

    List<OrgDepartment> findByBranchId(Long branchId);
}