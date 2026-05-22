package games.sparking.altara.menu.fill;

import games.sparking.altara.menu.fill.impl.BorderFiller;
import games.sparking.altara.menu.fill.impl.FillFiller;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum FillTemplate {

    FILL(new FillFiller()),
    BORDER(new BorderFiller());

    private final IMenuFiller menuFiller;

}
