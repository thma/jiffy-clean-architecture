package jiffy_clean_architecture.adapters;

import org.jiffy.core.EffectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jiffy_clean_architecture.usecases.LogEffect;

/**
 * Production implementation of LogEffect handler using SLF4J.
 */
@Component
public class Slf4jLogHandler implements EffectHandler<LogEffect> {

    private static final Logger logger = LoggerFactory.getLogger("Application");

    @Override
    public <T> T handle(LogEffect effect) {
        if (effect instanceof LogEffect.Info(String message)) {
            logger.info(message);
        } else if (effect instanceof LogEffect.Error(String message, Throwable error)) {
            if (error != null) {
                logger.error(message, error);
            } else {
                logger.error(message);
            }
        } else if (effect instanceof LogEffect.Warning(String message)) {
            logger.warn(message);
        } else if (effect instanceof LogEffect.Debug(String message)) {
            logger.debug(message);
        }
        return null; // LogEffect always returns Void
    }
}