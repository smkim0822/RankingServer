package com.pd.rankingserver.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Getter
@Setter
public class ScoreRequestDto {
    @NotBlank(message = "uuid는 필수입니다.")
    @Pattern(
            regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
            message = "uuid 형식이 올바르지 않습니다."
    )
    private String uuid;

    @NotBlank(message = "name은 필수입니다.")
    @Size(max = 50, message = "name은 50자 이하여야 합니다.")
    private String name;

    @NotNull(message = "score는 필수입니다.")
    @Min(value = 0, message = "score는 0 이상이어야 합니다.")
    private Integer score;

    @NotBlank(message = "hash는 필수입니다.")
    private String hash;
}