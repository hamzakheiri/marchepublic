package fr._42.marchepublic.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "scraper_config")
public class ScraperConfig {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(nullable = false)
    private int maxPages = 1;

    @Column(nullable = false)
    private boolean stopOnDuplicate = true;

    /** Stop after saving this many new results. 0 = unlimited. */
    @Column(nullable = false)
    private int maxResults = 0;

    @Column(nullable = false)
    private int intervalMinutes = 60;

    @Column
    private LocalDateTime lastRunAt;

    @Column
    private String lastRunStatus;
}
