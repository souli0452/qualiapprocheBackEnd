package com.qualiapproche.common.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;






@Data
@NoArgsConstructor
@AllArgsConstructor


public class UserStatusDto {
    private boolean emailVerified;
    private boolean enabled;
    private boolean temporaryPwd;
}
