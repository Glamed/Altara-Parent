package games.sparking.altara.menu.fill;

import games.sparking.altara.menu.fill.impl.AltaraFiller;
import games.sparking.altara.menu.fill.impl.BorderFiller;
import games.sparking.altara.menu.fill.impl.FillFiller;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum FillTemplate {

    /** Standard Altara frame: light-blue perimeter, light-gray background. */
    ALTARA(new AltaraFiller()),
    /** Every empty slot gets the menu's placeholder item. */
    FILL(new FillFiller()),
    /** Only the perimeter gets the menu's placeholder item. */
    BORDER(new BorderFiller());

    private final IMenuFiller menuFiller;

}
