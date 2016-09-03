package com.universe.defender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import android.util.Log;

/**
 * Hard AI.
 */
public final class AIAttackWeaker implements AI {

	private static Random random = new Random();

	private int unitsKeeped;///Defense level: do not attack from a planet with too few soldier.

	public AIAttackWeaker() {
		this.unitsKeeped = random.nextInt(8) * 5;
		Log.d("AI", "AIAttackWeaker unitsKeeped=" + this.unitsKeeped);
	}

	/**
	 * Attack a planet only if that planet is weaker than the starting one.
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
		///Avoid planet has too much soldier.
		for(Planet p : myPlanets){
			if(p.SoldiersCount >= p.MaxSoldiers){
				if( this.tryAttack(galaxy, p, me, accessiblePlanets)){
					hasAttack = true;
				} else{///Avoid the limitation due to the max
					ArrayList<Planet> nearest = accessiblePlanets.get(p);
					if(nearest.size() > 0){
						Planet destination = nearest.get(random.nextInt(nearest.size()));
						galaxy.Attack(p, destination);
					}
				}
			}
		}
		if(hasAttack){
			return;
		}
		///Find a planet that can attack		
		int retry = 0;
		Planet source = null;
		while (true) {
			int sourcePlanet = random.nextInt(myPlanets.length);
			Planet randPlanet = myPlanets[sourcePlanet];
			if (randPlanet.SoldiersCount < this.unitsKeeped) {
				retry++;
				if (retry >= 3) {
					break;
				}
				continue;
			} else {
				source = randPlanet;
				break;
			}
		}
		if (source != null) {
			hasAttack = this.tryAttack(galaxy, source, me, accessiblePlanets);
			if(hasAttack){
				return;
			}
		}

		// /If can not take a planet do a full attack if have enought soldiers.
		int myPlanetsStrong = 0;
		int myPlanetsWeak = 0;
		for (Planet p : myPlanets) {
			if (p.SoldiersCount < this.unitsKeeped) {
				myPlanetsWeak++;
			} else if (p.SoldiersCount > p.MaxSoldiers / 2) {
				myPlanetsStrong++;
			}
		}
		if (myPlanetsStrong > myPlanetsWeak) {
			int destinationPlanetIndex = random.nextInt(otherPlanets.length);
			Planet destinationPlanet = otherPlanets[destinationPlanetIndex];
			for (Planet s : myPlanets) {				
				galaxy.Attack(s, destinationPlanet);
			}
		}
	}
	
	/**
	 * Try to find a weaker planet.
	 */
	private boolean tryAttack(GalaxyThread galaxy, Planet source, Player me, HashMap<Planet, ArrayList<Planet>> accessiblePlanets){
		ArrayList<Planet> accessible = accessiblePlanets.get(source);
		boolean hasAttack = false;
		for (Planet p : accessible) {
			if(p.Player == null || p.Player.Team != me.Team){				
				int planetDef = 10;
				if (p.Player != null) {
					planetDef = p.Player.Defense;
				}
				if (p.SoldiersCount * planetDef < source.SoldiersCount
						* me.Force / 2) {
					galaxy.Attack(source, p);
					if(source.SoldiersCount < 5){
						break;
					}
					hasAttack = true;
				}
			}
		}
		return hasAttack;
	}

}