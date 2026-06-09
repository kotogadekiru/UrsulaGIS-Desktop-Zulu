package com.ursulagis.desktop.gui.nww;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.view.orbit.OrbitView;
import gov.nasa.worldwind.view.orbit.OrbitViewInputHandler;

/**
 * Skips view updates until the orbit view is linked to a globe. Prevents NPEs in
 * {@code FlyToOrbitViewAnimator} when the map is repainted (e.g. on mouse move) before
 * the first OpenGL frame in a JavaFX {@code SwingNode}.
 */
public class SafeOrbitViewInputHandler extends OrbitViewInputHandler {

    @Override
    public void apply() {
        if (!isGlobeReady()) {
            return;
        }
        super.apply();
    }

    private boolean isGlobeReady() {
        WorldWindow wwd = getWorldWindow();
        if (wwd == null || wwd.getModel() == null) {
            return false;
        }
        Globe modelGlobe = wwd.getModel().getGlobe();
        if (modelGlobe == null) {
            return false;
        }
        View view = getView();
        if (view instanceof OrbitView) {
            return ((OrbitView) view).getGlobe() != null;
        }
        return true;
    }
}
