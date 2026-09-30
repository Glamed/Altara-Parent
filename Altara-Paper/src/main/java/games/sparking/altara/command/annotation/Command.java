package games.sparking.altara.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Command {

    String[] names();

    String permission() default "";

    boolean playerOnly() default false;

    boolean async() default false;

    boolean hidden() default false;

    /** Short help description — a sentence fragment without a trailing period. */
    String description() default "";

}
