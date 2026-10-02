package com.smartlib.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMessageDto {

    @NotBlank(message = "Role must not be blank")
    private String role;

    @NotBlank(message = "Content must not be blank")
    @Size(max = 1000, message = "Content must not exceed 1000 characters")
    private String content;
}
