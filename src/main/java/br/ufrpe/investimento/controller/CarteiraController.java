package br.ufrpe.investimento.controller;

import br.ufrpe.investimento.dto.PosicaoCarteiraResponseDTO;
import br.ufrpe.investimento.security.AutenticacaoContexto;
import br.ufrpe.investimento.service.CarteiraService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/carteira")
public class CarteiraController {

    private final CarteiraService carteiraService;
    private final AutenticacaoContexto autenticacaoContexto;

    public CarteiraController(CarteiraService carteiraService, AutenticacaoContexto autenticacaoContexto) {
        this.carteiraService = carteiraService;
        this.autenticacaoContexto = autenticacaoContexto;
    }

    @GetMapping
    public ResponseEntity<List<PosicaoCarteiraResponseDTO>> minhasPosicoes() {
        return ResponseEntity.ok(carteiraService.listarPosicoes(autenticacaoContexto.usuarioIdAtual()));
    }
}
