package com.pd.rankingserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NicknameUpdateDto {
    @NotBlank(message = "name은 필수입니다.")
    @Size(max = 50, message = "name은 50자 이하여야 합니다.")
    private String name;
}
