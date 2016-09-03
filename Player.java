package com.universe.defender;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import android.util.Log;

/**
 * Human or IA player.
 */
public final class Player {
	public static final int color_PINK = android.graphics.Color.argb(255, 255, 105, 180);//#FF69B4
	public int Force = 10;// /Red like blood
	public int Defense = 10;// /Blue
	public int Speed = 10;// /Yellow like sun
	public int Growing = 10;// /Pink
	public int Team;
	public AI AI;///Null for the human player.
	public boolean IsAlive = true;
	public int Color;
	private long lastTime = System.currentTimeMillis();
	private long aiWaitBeforePlay = 3000;

	private int AITick = 3000;

	public static Player CreateNewPlayer() {
		Player p = new Player();
		p.Color = android.graphics.Color.GREEN;
		p.Team = 0;
		return p;
	}

	private static Random random = new Random();

	/**
	 * Convert IA level to an instance of IA.
	 */
	private static AI getAIFromCode(int aiCode) {
		if (aiCode == 0)
			return null;
		if (aiCode == 1)
			return new AIRandom();
		if (aiCode == 2)
			return new AINearest();
		if (aiCode == 3)
			return new AIAttackWeaker();
		Log.e("Player", "getAIFromCode unknow code=" + aiCode);
		return new AIRandom();
	}

	/**
	 * Put Characteristic to an IA and choose its color.
	 */
	private void setAIPowerBonus(int aiBonusStats) {
		int rand = random.nextInt(4);
		if(aiBonusStats > 101)
		{
			this.Force += aiBonusStats / 4;
			this.Defense += aiBonusStats / 4;
			this.Growing += aiBonusStats / 4;
		}
		else if(aiBonusStats > 85){
			aiBonusStats -= 25;
			this.Defense += 25;
		} else if(aiBonusStats > 60){
			aiBonusStats -= 20;
			this.Defense += 20;
		} else if(aiBonusStats > 30){
			aiBonusStats -= 10;
			this.Defense += 10;
		}
		if (rand == 0) {
			this.Force += aiBonusStats;
			this.Color = android.graphics.Color.RED;
		} else if (rand == 1) {
			this.Defense += aiBonusStats;
			this.Color = android.graphics.Color.BLUE;
		} else if (rand == 2) {
			this.Speed += aiBonusStats;
			this.Color = android.graphics.Color.YELLOW;
		} else {
			this.Growing += aiBonusStats;
			this.Color = color_PINK;
		}
		if (this.Speed > 50) {///Avoid ship pass throw planets
			this.Growing += (this.Speed - 50);
			this.Speed = 50;
		}
	}

	/**
	 * Init a Player played by an IA.
	 */
	public static Player CreateAI(int aiBonusStats, int team) {
		Player p = new Player();
		p.Team = team;
		p.setAIPowerBonus(aiBonusStats);
		if (GalaxyDomination.isHellMode) {
			p.setAIPowerBonus(aiBonusStats);
		}
		p.AI = getAIFromCode(GalaxyDomination.aiType);
		p.AITick = 12000;
		p.aiWaitBeforePlay = 4000;
		if(GalaxyDomination.aiType == 2){
			p.AITick = 8000;
			p.aiWaitBeforePlay = -4000;
		} else if(GalaxyDomination.aiType == 3){
			p.AITick = 5000;
			p.aiWaitBeforePlay = -2000;///3sec
		}
		if(GalaxyDomination.isHellMode){
			p.AITick -= 1000;
		}
		if (GalaxyDomination.isSpeedyMode) {
			p.AITick -= 1000;
		}
		p.AITick += random.nextInt(500);
		return p;
	}

	/**
	 * Choose speed of the IA play
	 */
	public void SetAIDelayBeforePlay(int duration) {
		this.aiWaitBeforePlay = duration;
	}

	/**
	 * Precomputed. For IA.
	 *
	 * All accessible planet from each planet.
	 * key: the origine planet
	 * Value: list of the accessible planets (sorted by distance)
	 */
	private HashMap<Planet, ArrayList<Planet>> accessiblePlanets = null;

	/**
	 * All accessible planet from a planet sorted by distance.
	 */
	private static ArrayList<Planet> getAccessiblePlanets(Planet source,
			PlanetsMap planetsMap) {
		TreeMap<Integer, Planet> sort = new TreeMap<Integer, Planet>();
		for (Planet destination : planetsMap.planets) {
			if (!source.CanGoTo(destination, planetsMap)) {
				// Log.d("CanGoTo", "CanGoTo=FALSE " + source.X + "x"+source.Y +
				// " ---> " + destination.X + "x" + destination.Y);
				continue;
			}
			// Log.d("CanGoTo", "CanGoTo=TRUE " + source.X + "x"+source.Y +
			// " ---> " + destination.X + "x" + destination.Y);

			int distanceSquare = (source.X - destination.X)
					* (source.X - destination.X) + (source.Y - destination.Y)
					* (source.Y - destination.Y);
			while (true) {
				Integer distanceSquareI = new Integer(distanceSquare);
				if (sort.containsKey(distanceSquareI)) {// /UseFull? Can have
														// duplicate key?
					distanceSquare++;
				} else {
					sort.put(distanceSquareI, destination);
					break;
				}
			}
		}
		ArrayList<Planet> returned = new ArrayList<Planet>();
		Set<Entry<Integer, Planet>> set = sort.entrySet();
		for (Entry<Integer, Planet> entry : set) {
			returned.add(entry.getValue());
		}
		if (returned.size() == 0) {
			Log.w("Player", "Planet can go nowhere. Planet=" + source.X + "x"
					+ source.Y);
		}
		return returned;
	}

	/**
	 * Give level information to the IA.
	 */
	private void init(HashMap<Planet, ArrayList<Planet>> accessiblePlanets) {
		if (this.AI != null) {
			this.accessiblePlanets = accessiblePlanets;
			this.lastTime = System.currentTimeMillis() + this.aiWaitBeforePlay;
			//Log.d("Player", "Init() this.lastTime=" + this.lastTime);
		}
	}

	/**
	 * Do all the pre-computation for the new level.
	 */
	public static void Init(GalaxyThread progressReporter, Player[] players,
			PlanetsMap planetsMap) {
		HashMap<Planet, ArrayList<Planet>> accessiblePlanets = new HashMap<Planet, ArrayList<Planet>>();
		int planetCount = planetsMap.planets.length;
		for (int i = 0; i < planetCount; i++) {
			Planet p = planetsMap.planets[i];
			accessiblePlanets.put(p, getAccessiblePlanets(p, planetsMap));
			progressReporter.sendMessageLoad("" + i + " / " + planetCount);
		}
		for (Player p : players) {
			p.init(accessiblePlanets);
		}
	}

	/**
	 * Let IA play.
	 */
	public void Play(GalaxyThread galaxy, Planet[] allPlanets, long now) {
		if (this.AI == null) {
			return;
		}
		int duration = (int) (now - this.lastTime);
		if (duration < this.AITick) {
			return;
		}
		this.lastTime = now;
		//Log.d("Player", "Play() this.lastTime=" + this.lastTime + "; now=" + now + ";this.AITick=" + this.AITick);
		ArrayList<Planet> myPlanets = new ArrayList<Planet>();
		ArrayList<Planet> otherPlanets = new ArrayList<Planet>();
		for (Planet p : allPlanets) {

			if (p.Player == this) {
				myPlanets.add(p);
			} else {
				if(p.Player != null && p.Player.Team == this.Team){
					continue;
				}
				if (p.IsBlackhole) {
					continue;
				}
				otherPlanets.add(p);
			}
		}
		Planet[] myP = new Planet[myPlanets.size()];
		myPlanets.toArray(myP);
		Planet[] otherP = new Planet[otherPlanets.size()];
		otherPlanets.toArray(otherP);
		this.AI.Play(galaxy, this, myP, otherP, this.accessiblePlanets);
	}



}
