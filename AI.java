package com.universe.defender;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Artificial Intelligence.
 *
 * */
public interface AI {

	public void Play(GalaxyThread galaxy, Player me, Planet[] myPlanets,
			Planet[] otherPlanets,
			HashMap<Planet, ArrayList<Planet>> accessiblePlanets);

}
