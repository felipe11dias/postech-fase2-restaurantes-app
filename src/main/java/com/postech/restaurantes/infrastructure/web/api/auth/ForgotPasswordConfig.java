package com.postech.restaurantes.infrastructure.web.api.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Fila do "esqueci minha senha": poucas threads e fila limitada. Um pico de pedidos — legítimo ou
 * não — ocupa no máximo estas duas threads e cem lugares na fila; o que passar disso é descartado
 * com aviso no log, sem tomar as threads que atendem o resto da API.
 */
@Configuration
public class ForgotPasswordConfig {

    public static final String EXECUTOR = "forgotPasswordExecutor";

    @Bean(name = EXECUTOR)
    public ThreadPoolTaskExecutor forgotPasswordExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("esqueci-senha-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }
}
