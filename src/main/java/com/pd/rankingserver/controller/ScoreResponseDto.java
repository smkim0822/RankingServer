package com.pd.rankingserver.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScoreResponseDto {
    private String uuid;
    private String name;
    private int score;
}
