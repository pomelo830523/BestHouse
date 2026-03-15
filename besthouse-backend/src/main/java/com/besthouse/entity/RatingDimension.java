package com.besthouse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "RATING_DIMENSION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingDimension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DIMENSION_ID")
    private Long dimensionId;

    @Column(name = "DIMENSION_NAME", nullable = false, length = 100)
    private String dimensionName;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "WEIGHT", nullable = false, precision = 5, scale = 4)
    private BigDecimal weight;

    @Column(name = "SORT_ORDER", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "IS_ACTIVE", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
