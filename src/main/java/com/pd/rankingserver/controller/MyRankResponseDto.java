package com.pd.rankingserver.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MyRankResponseDto {
    private int rank;
    private String name;
    private int score;
}