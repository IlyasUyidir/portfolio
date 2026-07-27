package com.gc2026.portfolio.domain.entity;

import com.gc2026.portfolio.domain.enums.CategoryType;
import jakarta.persistence.*;
import lombok.*;

@Entity
// I-8: Declare uniqueConstraint matching DB constraint unique_user_category so H2-based tests
// can validate duplicate-insert bugs that would otherwise only surface in production.
@Table(name = "categories", uniqueConstraints = {
    @UniqueConstraint(name = "unique_user_category", columnNames = {"user_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 7)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoryType type;

    @Column(name = "is_system")
    @Builder.Default
    private Boolean isSystem = false;
}
