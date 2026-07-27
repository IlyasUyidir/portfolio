package com.gc2026.portfolio.domain.entity;

import com.gc2026.portfolio.domain.enums.GoalStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * C-1: Optimistic lock version. Hibernate automatically increments this on
     * every UPDATE. If two concurrent addContribution() transactions both load
     * the same version, the second save will throw OptimisticLockException.
     * GoalService catches that and maps it to a clean 409.
     */
    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "target_amount", nullable = false)
    @Positive(message = "Goal target amount must be positive")
    private Long targetAmount;

    @Column(name = "current_amount")
    @Builder.Default
    private Long currentAmount = 0L;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private GoalStatus status = GoalStatus.EN_COURS;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
