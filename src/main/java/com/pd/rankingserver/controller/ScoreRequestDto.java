package com.pd.rankingserver.controller;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScoreRequestDto {
    private String uuid;
    private String name;
    private int score;
    private String hash;
}