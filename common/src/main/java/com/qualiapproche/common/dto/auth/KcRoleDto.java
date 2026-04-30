package com.qualiapproche.common.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KcRoleDto {
    private String id;
    private String name;
    private String description;
    private boolean composite;
}
