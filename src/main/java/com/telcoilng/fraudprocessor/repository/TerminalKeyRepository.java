package com.telcoilng.fraudprocessor.repository;

import com.telcoilng.fraudprocessor.Entity.TerminalKeys;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TerminalKeyRepository extends JpaRepository<TerminalKeys,Long> {
    Optional<TerminalKeys> findByTerminalid(String terminalId);
}
