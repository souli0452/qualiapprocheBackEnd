package com.qualiapproche.controller;

import com.qualiapproche.service.kcService.PieceJointeService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/api/load-pj")
public class PjController {

  private final PieceJointeService pieceJointeService;

  @GetMapping
  public ResponseEntity<byte[] > get(@RequestParam Long id) {

    return ResponseEntity.ok().body(pieceJointeService.loadFileByPjId(id));

  }
}
