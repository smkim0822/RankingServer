package com.pd.rankingserver.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Public representation of a ranking entry.
 *
 * <p>The persistence entity is intentionally not exposed by the API so that
 * database identifiers and internal user identifiers do not become part of
 * the public response contract.</p>
 */
@Getter
@AllArgsConstructor
public class RankingResponseDto {
    private String name;
    private int score;
}
