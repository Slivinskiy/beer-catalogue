package com.haufe.beercatalogue.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "beers")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Beer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotNull
    @DecimalMin(value = "0.0")
    @DecimalMax("100.0")
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal abv;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BeerType type;

    @NotBlank
    @Size(max = 1000)
    @Column(nullable = false)
    private String description;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manufacturer_id", nullable = false)
    private Manufacturer manufacturer;

    @Lob
    @Column(name = "image")
    private byte[] image;

    @Column(name = "image_content_type")
    private String imageContentType;

    public Beer(String name, BigDecimal abv, BeerType type, String description, Manufacturer manufacturer) {
        this.name = name;
        this.abv = abv;
        this.type = type;
        this.description = description;
        this.manufacturer = manufacturer;
    }
}
