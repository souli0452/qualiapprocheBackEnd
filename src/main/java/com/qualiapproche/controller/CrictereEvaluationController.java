package com.qualiapproche.controller;
import com.qualiapproche.dto.CrictereEvaluationDto;
import com.qualiapproche.service.CrictereEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.utils.ApiUrls.*;

@RestController
@RequestMapping(CRICTERE_EVALUATION_ROOT_URL)
@RequiredArgsConstructor
public class CrictereEvaluationController {

    private final CrictereEvaluationService crictereEvaluationService;


    /*-----------------------------------------------------------------------/
  /           Méthode de création d'un crictère d'évaluation                 /
/--------------------------------------------------------------------------*/
    @PostMapping(CREATE_CRICTERE_EVALUATION)
    public ResponseEntity<CrictereEvaluationDto> create(@RequestBody CrictereEvaluationDto crictereEvaluationDto) {
        CrictereEvaluationDto crictereEvaluation = crictereEvaluationService.create(crictereEvaluationDto);
        return new ResponseEntity<>(crictereEvaluation, HttpStatus.CREATED);
    }

    /*------------------------------------------------------------------------/
  /           Méthode de consultation d'un crictère d'évaluation par ID      /
/--------------------------------------------------------------------------*/

    @GetMapping(GET_CRICTERE_EVALUATION_BY_ID)
    public ResponseEntity<CrictereEvaluationDto> getCrictereEvaluationById(@RequestParam UUID id) {
        CrictereEvaluationDto crictereEvaluationDto = crictereEvaluationService.getCrictereEvaluationById(id);
        return new ResponseEntity<>(crictereEvaluationDto, HttpStatus.OK);
    }

    /*----------------------------------------------------------------------------/
    /           Méthode de consultation de  tout les crictère d'évaluation       /
    /--------------------------------------------------------------------------*/

    @GetMapping(GET_ALL_CRICTERE_EVALUATION)
    public ResponseEntity<List<CrictereEvaluationDto>> allCrictereEvaluations() {
        List<CrictereEvaluationDto> crictereEvaluationDtos = crictereEvaluationService.allCrictereEvaluations();
        return new ResponseEntity<>(crictereEvaluationDtos, HttpStatus.OK);
    }

    /*----------------------------------------------------------------------------/
    /           Méthode de modification d'un crictère d'évaluation               /
    /--------------------------------------------------------------------------*/

    @PutMapping(UPDATE_CRICTERE_EVALUATION)
    public ResponseEntity<CrictereEvaluationDto> update(@RequestBody CrictereEvaluationDto crictereEvaluationDto) {
        CrictereEvaluationDto crictereEvaluation = crictereEvaluationService.update(crictereEvaluationDto);
        return new ResponseEntity<>(crictereEvaluation, HttpStatus.OK);
    }

     /*----------------------------------------------------------------------------/
     /           Méthode de suppression d'un crictère d'évaluation               /
    /--------------------------------------------------------------------------*/

    @DeleteMapping(DELETE_CRICTERE_EVALUATION)
    public void deleteyId(@RequestParam UUID id) {
        crictereEvaluationService.delete(id);
    }
}
