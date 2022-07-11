package com.telcoilng.fraudprocessor.Entity;

import lombok.*;

import javax.persistence.*;
import java.util.Date;

@Entity(name = "stations")
@Setter@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class Station {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String name;
    private String status;
    private String zmk;
    private String zpk;
    private String zmkKcv;
    private String zpkKcv;
    private String newZpkKcv;

    private Date lastEcho;
    private Date lastZpkChange;


}
