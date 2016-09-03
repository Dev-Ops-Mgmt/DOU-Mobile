package com.universe.defender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import android.util.Log;

/**
 * Medium IA 
 */
public final class AINearest implements AI {

	private static Random random = new Random();

	private int unitsKeeped;///Defense level: do not attack from a planet with too few soldier.

	public AINearest() {
		this.unitsKeeped = random.nextInt(5) * 5;
		Log.d("AI", "AINearest unitsKeeped=" + this.unitsKeeped);
	}

	/**
	 * Attack the nearest planet.
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
		boolean hasAttack = false;
		for(Planet p : myPlanets){
			if(p.SoldiersCount >= p.MaxSoldiers){///Attack anything if planet is full
				int destinationPlanet = random.nextInt(otherPlanets.length);				
				galaxy.Attack(p, otherPlanets[destinationPlanet]);
				hasAttack = true;
			}
		}
		if(hasAttack){
			return;
		}
		
		int sourcePlanet = random.nextInt(myPlanets.length);
		Planet source = myPlanets[sourcePlanet];
		if (source.SoldiersCount < this.unitsKeeped) {
			return;
		}
		ArrayList<Planet> accessible = accessiblePlanets.get(source);
		for (Planet p : accessible) {
			if(p.Player == null || p.Player.Team != me.Team){
				galaxy.Attack(source, p);
				return;
			}
		}		
	}

}
