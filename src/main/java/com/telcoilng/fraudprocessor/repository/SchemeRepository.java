package com.telcoilng.fraudprocessor.repository;

import com.telcoilng.fraudprocessor.Entity.Scheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchemeRepository extends JpaRepository<Scheme, Long> {
}
