package com.universe.defender;

import java.util.Random;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * A set of Ship that travel from one planet to another.
 * 
 * This manage the move of ships, the interaction with other planets and the drawing of ships.
 * 
 * Note: "travel-fixed" means value will not change during all the ships move.
 */
public final class Attack {

	public Player Player;//Owner, travel-fixed
	private int playerForce;//pseudo-optimization, travel-fixed
	private Planet Destination;//Planet to attack, travel-fixed
	private int nbSoldier;////pseudo-optimization, travel-fixed
	/**
	 * Traveling ships
	 * Note: it should have be a class, but we are on a phone, so let's avoid to have too much instance of
	 *       classes. It is maybe a stupid and useless optimization
	 *  
	 *  Each ships is encoded in 3 int:
	 *   - alive, 0 or 1
	 *   - X, in pixel
	 *   - Y, in pixel
	 *   These values are updated during travel.
	 */
	private int[] ships;	
	private long lastTime;//To move ship only after a few milliseconds tick.
	private int speedChance;//Depending of player, travel-fixed
	private int nbIteration = 0;//TTL, in case of
	private Paint paint;//the dot color, travel-fixed
	private Paint xpaint;//semi-transparent aura, travel-fixed
	private static Random random = new Random();


	public Attack(Planet source, Planet destination) {
		this.Player = source.Player;
		this.Destination = destination;
		if(GalaxyDomination.isLowPerf){			 
			
			this.nbSoldier = (source.SoldiersCount + 4) / 2;///+4 to allow the send if there is only 6 soldier
			this.nbSoldier = (this.nbSoldier / 5) * 5;///Round 5
		} else{
			this.nbSoldier = source.SoldiersCount / 2;			
		}
		source.SoldiersCount -= this.nbSoldier;
		this.playerForce = this.Player.Force;
		if(GalaxyDomination.isLowPerf)
		{
			this.nbSoldier = this.nbSoldier / 5;///Reduce number of soldier, but make it stronger
			this.playerForce = this.playerForce * 5;
		}
		
		this.ships = new int[nbSoldier * 3];
		initShipositions(nbSoldier, this.ships, source, destination);

		this.lastTime = System.currentTimeMillis();
		this.paint = new Paint();
		this.paint.setColor(source.Player.Color);
		this.paint.setStrokeWidth(2);
		this.xpaint = new Paint();
		this.xpaint.setColor(source.Player.Color);
		this.xpaint.setStrokeWidth(2);

		this.speedChance = source.Player.Speed * 20; // /10px/sec

	}

	/**
	 * To have some nuances on ships colors 
	 */
	private static int createShipColor(int playerColor) {

		if (random.nextInt(100) < 40) {
			return playerColor;
		}
		int r = (playerColor >> 16) & 255;
		int g = (playerColor >> 8) & 255;
		int b = (playerColor >> 0) & 255;
		int alpha = 100 + random.nextInt(156);
		return (alpha << 24) + (r << 16) + (g << 8) + b;
	}

	/**
	 * Init the ships array.
	 * Note: Ships must start at the "surface" of the planet, not at the radius. 
	 */
	private static void initShipositions(int nbSoldier, int[] ships,
			Planet source, Planet destination) {
		int px = source.X;
		int py = source.Y;
		float radius = source.Radius + 2;
		int playerColor = source.Player.Color;
		double destinationAngus = Math.atan2(destination.Y - source.Y,
				destination.X - source.X);
		for (int i = 0; i < nbSoldier; i++) {
			double startAngus = destinationAngus
					- ((3.14 / 2) * (random.nextDouble() - 0.5));
			ships[i * 3 + 0] = createShipColor(playerColor);
			ships[i * 3 + 1] = (int) (px + radius * Math.cos(startAngus));
			ships[i * 3 + 2] = (int) (py + radius * Math.sin(startAngus));
		}
	}

	/**
	 * Move ships, look if it cross some planet, manage the attack. 
	 */
	public boolean doPhysic(GalaxyThread thread, PlanetsMap planetMap, long now) {

		int duration = (int) (now - this.lastTime);
		if (duration < (GalaxyDomination.isSpeedyMode ? 50 : 100)) {
			return false;
		}
		this.lastTime = now;
		this.nbIteration++;
		if (this.nbIteration > 10 * 60) {// /TTL of 1 minute
			return true;
		}
		boolean areAllDead = true;
		int nbSoldier = this.nbSoldier;
		int speedChance = this.speedChance;
		int[] ships = this.ships;
		int destinationX = this.Destination.X;
		int destinationY = this.Destination.Y;
		for (int i = 0; i < nbSoldier; i++) {
			int index = i * 3;
			if (ships[index] == 0) {
				continue;
			}
			areAllDead = false;
			int x = ships[index + 1];
			int y = ships[index + 2];
			int distanceX = destinationX - x;
			int distanceY = destinationY - y;
			int incr = 0;
			int speedShip = speedChance;
			while (speedShip > 100) {
				incr++;
				speedShip -= 100;
			}
			int randSpeed = random.nextInt(100);
			if (randSpeed < speedShip) {
				incr++;
			}
			int randDirection = random.nextInt(100);
			if (distanceX * distanceX * randDirection * randDirection > distanceY
					* distanceY * 50 * 50) {
				if (distanceX > 0) {
					x += incr;
				} else {
					x -= incr;
				}
				ships[index + 1] = x;
			} else {
				if (distanceY > 0) {
					y += incr;
				} else {
					y -= incr;
				}
				ships[index + 2] = y;
			}
			Planet impactedPlanet = planetMap.GetPlanet(x, y);
			if (impactedPlanet != null &&
				 (impactedPlanet == this.Destination || impactedPlanet.Player == null || impactedPlanet.Player.Team != this.Player.Team)) {
				// Log.d("Impact", "Impact " + impactedPlanet.X + " " +
				// impactedPlanet.Y);
				ships[index] = 0;
				if (impactedPlanet.IsBlackhole) 
				{
					///Ship die stupidly
				} 
				else if (impactedPlanet.SoldiersCount == 0) 
				{
					if(this.Player.AI == null){
						thread.sendPlaySound(GalaxyDomination.soundPlanetWinIndex);
					}					
					impactedPlanet.SetPlayer(this.Player);	
					if(GalaxyDomination.isLowPerf){						
						impactedPlanet.SoldiersCount += 5;
					} else{
						impactedPlanet.SoldiersCount ++;
					}
				}
				else if (impactedPlanet.Player != null && impactedPlanet.Player.Team == this.Player.Team)///Team planet: renforce
				{
					if(GalaxyDomination.isLowPerf){						
						impactedPlanet.SoldiersCount += 5;
					} else{
						impactedPlanet.SoldiersCount ++;
					}
				}
				else
				{					
					if (impactedPlanet.Player == null) {///Attack
						if(GalaxyDomination.isLowPerf){
							impactedPlanet.SoldiersCount -= 5;	
							if(impactedPlanet.SoldiersCount < 0){
								impactedPlanet.SoldiersCount = 0;
							}
						} else{						
							impactedPlanet.SoldiersCount --;
						}
					}
					else///Planet below to another Player 
					{
						// /Need to take force into account
						int shipForce = this.playerForce;
						int planetForce = impactedPlanet.Player.Defense;									
						while (shipForce >= planetForce) {
							shipForce -= planetForce;
							impactedPlanet.SoldiersCount--;
						}
						int randFight = random.nextInt(100);
						if (randFight * planetForce < 100 * shipForce) {
							impactedPlanet.SoldiersCount--;
						}
						if (impactedPlanet.SoldiersCount <= 0) {
							impactedPlanet.SoldiersCount = 0;
							if(impactedPlanet.Player.AI == null){
								thread.sendPlaySound(GalaxyDomination.soundPlanetLostIndex);
							}
							impactedPlanet.SetPlayer(null);
						}
					}
				}
			}
		}
		return areAllDead;
	}

	/**
	 * Draw each ships on screen. 
	 */
	public void doDraw(Canvas canvas) {

		Paint paint = this.paint;
		if(!GalaxyDomination.isLowPerf){
			for (int i = 0; i < nbSoldier; i++) {			
				int index = i * 3;
				int shipColor = ships[index];
				if (shipColor == 0) {
					continue;
				}
				int x = ships[index + 1];
				int y = ships[index + 2];
				
				xpaint.setColor(shipColor);
				xpaint.setAlpha(20);
				canvas.drawCircle(x, y, 4 * CampaignMap.mapToScreenScale, xpaint);

				paint.setColor(shipColor);
				canvas.drawCircle(x, y, 1 * CampaignMap.mapToScreenScale,paint);
			} 
		}
		else///LowPerf
		{
			paint.setColor(this.Player.Color);
			for (int i = 0; i < nbSoldier; i++) {			
				int index = i * 3;
				int shipColor = ships[index];
				if (shipColor == 0) {
					continue;
				}
				int x = ships[index + 1];
				int y = ships[index + 2];				
				canvas.drawPoint(x, y, paint);
			}
		}
	}

}
