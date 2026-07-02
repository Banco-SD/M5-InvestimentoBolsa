package br.ufrpe.investimento.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Pequeno helper pra não espalhar `(UUID) SecurityContextHolder...getPrincipal()`
 * por todos os controllers.
 */
@Component
public class AutenticacaoContexto {

    public UUID usuarioIdAtual() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (UUID) principal;
    }
}
