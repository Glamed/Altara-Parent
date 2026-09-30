package games.sparking.altara.command.annotation;

import games.sparking.altara.utils.Panel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Panel shown by a command group's help listing, e.g.
 * {@code @Header(value = "Rank", panel = Panel.STAFF)}.  Without it the command label is
 * used as the title with the {@link Panel#PLAYER} colours.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Header {

    String value();

    /** Optional {@code [Title > Subtitle]}. */
    String subtitle() default "";

    Panel panel() default Panel.PLAYER;
}
