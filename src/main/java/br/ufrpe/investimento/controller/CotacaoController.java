package br.ufrpe.investimento.controller;

import br.ufrpe.investimento.dto.CotacaoResponseDTO;
import br.ufrpe.investimento.service.CotacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cotacoes")
public class CotacaoController {

    private final CotacaoService cotacaoService;

    public CotacaoController(CotacaoService cotacaoService) {
        this.cotacaoService = cotacaoService;
    }

    @GetMapping("/{ticker}")
    public ResponseEntity<CotacaoResponseDTO> buscarCotacao(@PathVariable String ticker) {
        return ResponseEntity.ok(cotacaoService.buscarCotacao(ticker));
    }
}
