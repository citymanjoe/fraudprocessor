package com.telcoilng.fraudprocessor.Entity;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;

@Entity(name = "terminalkeys")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
@Data
public class TerminalKeys implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String terminalid;

    private String zmk;

    private String zmkKeyBlock;

    private String zmkKcv;

    private String tmk;

    private String tmkKeyBlock;

    private String tmkKcv;

    private String tsk;

    private String tskKeyBlock;

    private String tskKcv;

    private String tpk;

    private String tpkKeyBlock;

    private String tpkKcv;
}
