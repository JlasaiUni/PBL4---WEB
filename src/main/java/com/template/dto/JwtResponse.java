package com.template.dto;

import lombok.*;

import java.util.List;

/** DTO de respuesta tras autenticacion JWT exitosa. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {

    private String token;
    @Builder.Default
    private String type = "Bearer";
    private String username;
    private String email;
    private List<String> roles;
}
