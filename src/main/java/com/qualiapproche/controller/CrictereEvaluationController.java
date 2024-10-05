package com.qualiapproche.controller;

import com.qualiapproche.dto.CrictereEvaluationDto;
import com.qualiapproche.service.CrictereEvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.qualiapproche.utils.ApiUrls.CREATE_CRICTERE_EVALUATION;
import static com.qualiapproche.utils.ApiUrls.CRICTERE_EVALUATION_ROOT_URL;

@RestController
@RequestMapping(CRICTERE_EVALUATION_ROOT_URL)
public class CrictereEvaluationController {

    @Autowired
    private CrictereEvaluationService crictereEvaluationService;


    @PostMapping(CREATE_CRICTERE_EVALUATION)
    public ResponseEntity<CrictereEvaluationDto> create(@RequestBody CrictereEvaluationDto crictereEvaluationDto) {
        CrictereEvaluationDto crictereEvaluation = crictereEvaluationService.create(crictereEvaluationDto);
        return new ResponseEntity<>(crictereEvaluation, HttpStatus.OK);
    }
}
