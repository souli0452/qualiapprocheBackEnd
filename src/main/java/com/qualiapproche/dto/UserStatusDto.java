package com.qualiapproche.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class UserStatusDto {
    private boolean emailVerified;
    private boolean enabled;
    private boolean temporaryPwd;
}
