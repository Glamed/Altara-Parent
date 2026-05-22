package games.sparking.altara.utils;

import games.sparking.altara.Altara;
import games.sparking.altara.SystemType;

public class IllegalSystemTypeException extends IllegalStateException {

    public IllegalSystemTypeException() {
        super(Altara.getSystemType().name());
    }

    public IllegalSystemTypeException(SystemType required) {
        super(Altara.getSystemType().name() + ", Required: " + required.name());
    }

    public static void checkOrThrow(SystemType systemType) {
        if (Altara.getSystemType() != systemType)
            throw new IllegalSystemTypeException(systemType);
    }

}