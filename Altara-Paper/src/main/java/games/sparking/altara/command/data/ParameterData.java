package games.sparking.altara.command.data;

import games.sparking.altara.command.parameter.ParameterType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;


@Getter
@AllArgsConstructor
public class ParameterData implements Data {

    private final String name;
    private final String defaultValue;
    private final Class<?> type;
    private final boolean wildCard;
    private final ParameterType parameterType;
    private final List<String> completionFlags;
    private final int index;

}
