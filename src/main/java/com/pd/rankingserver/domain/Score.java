package com.pd.rankingserver.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Score {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String uuid;

    private String name;
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