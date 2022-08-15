package com.kiona.analysis.entity.repository;

import com.kiona.analysis.entity.GoogleEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GoogleEventRepository extends JpaRepository<GoogleEvent, UUID> {

    Optional<GoogleEvent> findByCategoryAndNo(String category, String no);
}