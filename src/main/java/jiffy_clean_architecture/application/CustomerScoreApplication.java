package jiffy_clean_architecture.application;

import jiffy_clean_architecture.usecases.CustomerScoreUseCase;
import jiffy_clean_architecture.usecases.LogEffect;
import jiffy_clean_architecture.usecases.OrderRepositoryEffect;
import jiffy_clean_architecture.usecases.ReturnRepositoryEffect;
import org.jiffy.core.EffectHandler;
import org.jiffy.core.EffectRuntime;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot application with algebraic effects support.
 * This application demonstrates Clean Architecture with algebraic effects.
 */
@SpringBootApplication
@ComponentScan(basePackages = "jiffy_clean_architecture")

public class CustomerScoreApplication {
    
    @Bean 
    public CustomerScoreUseCase getCustomerScoreUseCase() {
        return new CustomerScoreUseCase();
    }

    @Bean
    public EffectRuntime getRuntime(EffectHandler<LogEffect> logHandler,
                                    EffectHandler<OrderRepositoryEffect<?>> orderHandler,
                                    EffectHandler<ReturnRepositoryEffect<?>> returnHandler) {
        return EffectRuntime.builder()
                .withHandlerUnsafe(LogEffect.class, logHandler)
                .withHandlerUnsafe(OrderRepositoryEffect.FindByCustomerId.class, orderHandler)
                .withHandlerUnsafe(ReturnRepositoryEffect.FindByCustomerId.class, returnHandler)
                .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(CustomerScoreApplication.class, args);
    }
}