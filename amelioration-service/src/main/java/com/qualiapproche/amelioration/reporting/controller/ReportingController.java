package com.qualiapproche.amelioration.reporting.controller;


import com.qualiapproche.amelioration.reporting.dto.ReportingInputDto;
import com.qualiapproche.amelioration.reporting.dto.ReportingResponseDto;
import com.qualiapproche.amelioration.reporting.service.ReportingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

import static com.qualiapproche.common.utils.ApiUrls.CREATE_REPORTING_ROOT_URL;


/**
 * @author : <a href="siguizana08@gmail.com">BRAHIMA TRAORE </a>.
 * @version : 1.0
 * @since : 08/03/2022 à 14:19:09
 */
@RestController
@Slf4j
@RequestMapping(CREATE_REPORTING_ROOT_URL)
@RequiredArgsConstructor
public class ReportingController {
    private final ReportingService reportingService;


    @PostMapping( )
    public ResponseEntity<byte[]> generateReport(@RequestBody @Valid final ReportingInputDto inputDto)
            throws IOException, JRException {
        log.debug("Generate report: {}", inputDto);

        ReportingResponseDto responseDto = reportingService.generateReport(inputDto);
        return new ResponseEntity<>(responseDto.getReportFile(), HttpStatus.CREATED);
    }
}
