package com.telcoilng.fraudprocessor.Entity;

import lombok.*;

import javax.persistence.*;
import java.math.BigInteger;

@Entity(name = "routing_rule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class RoutingRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String type;
    private String userId;
    private BigInteger minimum_amount;
    private BigInteger maximum_amount;
    private String  values;
    private String  scheme;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="stationId", nullable=false)
    private Station stationId;
    private int precedence;
    private boolean deleted;
}
