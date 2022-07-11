package com.telcoilng.fraudprocessor.repository;

import com.telcoilng.fraudprocessor.Entity.StatusType;
import com.telcoilng.fraudprocessor.Entity.Terminals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TerminalRepository extends JpaRepository<Terminals, Long> {
    Optional<Terminals> findTerminalsByTerminalIdAndStatus(String terminalId, StatusType statusType);

}