package com.qualiapproche.common.utils;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailMessage {
    private String toAddress;
    private String subject;
    private String body;
    private String ccAddress;
}
