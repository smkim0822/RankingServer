package com.pd.rankingserver.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(indexes = {
        @Index(name = "idx_score_score", columnList = "score")
})
@Getter @Setter
@NoArgsConstructor
public class Score {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 36, unique = true, nullable = false)
    private String uuid;

    @Column(length = 50, nullable = false)
    private String name;

    @Column(nullable = false)
    private int score;

    @Builder
    public Score(String uuid, String name, int score) {
        this.uuid = uuid;
        this.name = name;
        this.score = score;
    }

    public void  updateName(String newName){
        this.name = newName;
    }
}