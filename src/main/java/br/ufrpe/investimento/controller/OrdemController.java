package br.ufrpe.investimento.controller;

import br.ufrpe.investimento.dto.MensagemResponseDTO;
import br.ufrpe.investimento.dto.OrdemResponseDTO;
import br.ufrpe.investimento.request.CriarOrdemRequest;
import br.ufrpe.investimento.security.AutenticacaoContexto;
import br.ufrpe.investimento.service.OrdemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ordens")
public class OrdemController {

    private final OrdemService ordemService;
    private final AutenticacaoContexto autenticacaoContexto;

    public OrdemController(OrdemService ordemService, AutenticacaoContexto autenticacaoContexto) {
        this.ordemService = ordemService;
        this.autenticacaoContexto = autenticacaoContexto;
    }

    @PostMapping
    public ResponseEntity<OrdemResponseDTO> criarOrdem(@Valid @RequestBody CriarOrdemRequest request) {
        OrdemResponseDTO resposta = ordemService.criarOrdem(autenticacaoContexto.usuarioIdAtual(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @GetMapping
    public ResponseEntity<List<OrdemResponseDTO>> listarMinhasOrdens() {
        return ResponseEntity.ok(ordemService.listarPorUsuario(autenticacaoContexto.usuarioIdAtual()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdemResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(ordemService.buscarPorId(autenticacaoContexto.usuarioIdAtual(), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MensagemResponseDTO> cancelarOrdem(@PathVariable UUID id) {
        ordemService.cancelarOrdem(autenticacaoContexto.usuarioIdAtual(), id);
        return ResponseEntity.ok(new MensagemResponseDTO("Ordem cancelada com sucesso"));
    }
}
