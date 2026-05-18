package games.sparking.altara.command.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Header {

    String primaryColor();

    String secondaryColor();

    String tertiaryColor() default "";

    String header() default "";

    String subHeaderColor() default "";

    String subHeader() default "";


}
