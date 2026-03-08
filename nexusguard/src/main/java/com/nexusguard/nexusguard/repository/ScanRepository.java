package com.nexusguard.nexusguard.repository;

import com.nexusguard.nexusguard.model.Scan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ScanRepository extends JpaRepository<Scan, Long> {
    List<Scan> findAllByOrderByScannedAtDesc();
}