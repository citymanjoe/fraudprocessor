package com.telcoilng.fraudprocessor.repository;

import com.telcoilng.fraudprocessor.Entity.NibssKeys;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NibssKeysRepository extends JpaRepository<NibssKeys,Long> {
    NibssKeys findByTerminalId(String terminalI);
}
