package games.sparking.altara.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CommandCooldown {

    int time();

    TimeUnit timeUnit() default TimeUnit.SECONDS;

    String bypassPermission() default "";

    boolean global() default false;

}
