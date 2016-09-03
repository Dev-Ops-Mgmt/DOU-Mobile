package com.universe.defender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import android.util.Log;

/**
 * Easy IA: attack a planet at random.
 *
 */
public final class AIRandom implements AI {

	private static Random random = new Random();

	private int unitsKeeped;///Defense level: do not attack from a planet with too few soldier.

	public AIRandom() {
		this.unitsKeeped = random.nextInt(5) * 5;
		Log.d("AI", "AIRandom unitsKeeped=" + this.unitsKeeped);
	}

	/**
	 * Choose an accessible planet and attack it.
	 */
	@Override
	public void Play(GalaxyThread galaxy, Player me, Planet[] myPlanets,
			Planet[] otherPlanets,
			HashMap<Planet, ArrayList<Planet>> accessiblePlanets) {
		if (myPlanets.length == 0) {
			return;
		}
		if (otherPlanets.length == 0) {
			return;
		}
		int sourcePlanet = random.nextInt(myPlanets.length);
		Planet source = myPlanets[sourcePlanet];
		if (source.SoldiersCount < this.unitsKeeped) {
			return;
		}
		ArrayList<Planet> accessible = accessiblePlanets.get(source);
		if(accessible.size() != 0){
			Planet destination = accessible.get(random.nextInt(accessible.size()));
			if(destination.Player == null || destination.Player.Team != me.Team){
				galaxy.Attack(source, destination);
				return;
			}
		}
		int destinationPlanet = random.nextInt(otherPlanets.length);
		galaxy.Attack(source, otherPlanets[destinationPlanet]);
	}

}
