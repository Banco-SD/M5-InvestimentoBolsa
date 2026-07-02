package br.ufrpe.investimento.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Este módulo NÃO gerencia usuários - quem faz isso é o módulo de Autenticação.
 * Aqui apenas validamos a assinatura do JWT (mesmo segredo `jwt.secret`
 * compartilhado entre os módulos) para extrair o usuarioId de forma segura,
 * sem precisar bater no módulo de Autenticação a cada requisição.
 *
 * O "principal" da Authentication é diretamente o UUID do usuário
 * (ver AutenticacaoContexto para como recuperá-lo no controller).
 */
@Component
public class JwtValidacaoFilter extends OncePerRequestFilter {

    private final SecretKey chave;

    public JwtValidacaoFilter(@Value("${jwt.secret}") String segredo) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(header.substring(7))
                    .getPayload();

            UUID usuarioId = UUID.fromString(claims.getSubject());

            var authToken = new UsernamePasswordAuthenticationToken(usuarioId, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authToken);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
