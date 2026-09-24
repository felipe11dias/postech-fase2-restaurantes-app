package com.postech.restaurantes.infrastructure.config;

import com.postech.restaurantes.infrastructure.security.JwtAuthenticationFilter;
import com.postech.restaurantes.infrastructure.web.auth.AuthRestController;
import com.postech.restaurantes.infrastructure.web.user.UserRestController;
import jakarta.servlet.DispatcherType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Autenticação stateless por JWT.
 *
 * <p>Sem sessão e sem CSRF porque não há cookie de sessão a proteger: cada requisição carrega
 * o próprio token. A lista do que é público fica <strong>aqui, em um lugar só</strong> — o
 * filtro apenas traduz o token em contexto e nunca decide se a requisição passa.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLICOS_GET = {
        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/health", "/actuator/info"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter)
            throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Sem credenciais é 401 ("identifique-se"), não 403 ("você não pode"): o padrão do
                // Spring, sem login por formulário nem básico, seria devolver 403 para os dois casos.
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(requests -> requests
                        // O encaminhamento interno para /error não é uma requisição do cliente: barrá-lo
                        // trocaria todo erro da aplicação por um 403 sem corpo, escondendo a causa.
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                        .requestMatchers(HttpMethod.POST, AuthRestController.BASE_PATH + "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, UserRestController.BASE_PATH).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLICOS_GET).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
