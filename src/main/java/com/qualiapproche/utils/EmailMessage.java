package com.qualiapproche.utils;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailMessage {
	private String to_address;
	private String subject;
	private String body;

}
