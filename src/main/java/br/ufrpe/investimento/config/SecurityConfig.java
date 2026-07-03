package br.ufrpe.investimento.config;

import br.ufrpe.investimento.security.JwtValidacaoFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtValidacaoFilter jwtValidacaoFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtValidacaoFilter jwtValidacaoFilter, CorsConfigurationSource corsConfigurationSource) {
        this.jwtValidacaoFilter = jwtValidacaoFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Cotações são públicas (não expõem dado de usuário nenhum)
                        .requestMatchers(HttpMethod.GET, "/cotacoes/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**", "/v3/api-docs.yaml"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtValidacaoFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
