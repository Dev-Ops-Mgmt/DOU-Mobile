package com.universe.defender;
import java.util.ArrayList;
import java.util.Random;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.util.Log;

/**
 * Represent and manage a Planet during one game.
 * A planet know how to draw itself on the canvas using:
 * 	 - fields: X, Y, Radius, SoldierCount, xxxPaints, planetBitmap
 *   - method: doDraw()
 * It also know how to upgrade the number of soldier it own:
 * 	 - fields: Size and chanceToGrow
 *   - method: updatePhysics()
 *
 *   Note: "game-constant" means the field do not change after one level start
 */
public final class Planet {

	public int X;//In pixel, game-constant
	public int Y;//In pixel, game-constant
	public int Radius;//In pixel, game-constant
	public int Size;//small=1, medium=2, big=3, game-constant
	public int SoldiersCount;
	public Player Player;//the owner, can be null
	public int GrowingFactor;//Depending of planet size, game-constant
	private int chanceToGrow = 0;//Depending of planet AND the player stats. Changes with planet owner
	public int MaxSoldiers;//Limit from where the number of soldier grow less, game-constant
	public boolean IsBlackhole = false;//Black planet that appears in the campaign, game-constant
	private Paint paint;//Depending of the player
	private Paint fontPaint;//Normal font, game-constant
	private Paint fontPaintOnMax;//Draw in bold when planet begin to reach the max soldier, game-constant
	private long lastPhysicTime;
	private static Random random = new Random();
	private Bitmap planetBitmap;//Depend of size, game-constant

	public Planet() {
		this.fontPaint = new Paint();
		this.fontPaint.setAntiAlias(true);
		this.fontPaint.setColor(Color.BLACK);
		this.fontPaint.setTextAlign(Align.CENTER);
		//Log.i("Planet", "getTextSize" + this.fontPaint.getTextSize());
		this.fontPaint.setTextSize(12 * CampaignMap.mapToScreenScale);
		this.fontPaintOnMax = new Paint();
		this.fontPaintOnMax.setAntiAlias(true);
		this.fontPaintOnMax.setColor(Color.BLACK);
		this.fontPaintOnMax.setTextAlign(Align.CENTER);
		this.fontPaintOnMax.setTypeface(Typeface.DEFAULT_BOLD);
		this.fontPaintOnMax.setTextSize(12 * CampaignMap.mapToScreenScale);

		this.SetPlayer(null);
		this.lastPhysicTime = System.currentTimeMillis();
	}

	/*
	 *  Set if planet is a big one or a small one
	 *  Init the depending game-constant fields
	 */
	public void SetSize(GalaxyDomination ressource, int size) {
		this.Size = size;
		switch(size){
		case 1 :
			this.GrowingFactor = 5;
			break;
		case 2 :
			this.GrowingFactor = 15;
			break;
		case 3 :
			this.GrowingFactor = 30;
			break;
		default://Currently not used
			this.GrowingFactor = (size) * (size + 1);///2, 6, 12, 20

		}
		this.MaxSoldiers = size * 25;
		switch (size) {
		case 1:
			this.Radius = 18;
			break;
		case 2:
			this.Radius = 25;
			break;
		case 3:
			this.Radius = 45;
			break;
		default:
			this.Radius = 10 + 20 * size;
			break;
		}
		this.Radius = (int) (this.Radius * CampaignMap.mapToScreenScale);
		this.planetBitmap = ressource.GetPlanetBitmap(this.Radius);
	}

	/**
	 * Set planet owner
	 */
	public void SetPlayer(Player player) {
		this.Player = player;
		this.paint = new Paint();
		this.paint.setAntiAlias(true);
		if (player != null) {
			this.chanceToGrow = this.GrowingFactor * player.Growing; // around
																		// 100%
																		// for
																		// basic
			Log.d("Planet", "SetPlayer, chanceToGrow=" + chanceToGrow + " (="
					+ this.GrowingFactor + "x" + player.Growing);
			this.paint.setColor(player.Color);
			this.paint.setAlpha(50);
		} else {
			this.chanceToGrow = 0;
			this.paint.setColor(Color.GRAY);
			this.paint.setAlpha(0);
			if (this.IsBlackhole) {
				this.paint.setColor(Color.BLACK);
				this.paint.setAlpha(255);
			}
		}
	}

	/**
	 * Look if this planet share pixels with an other planet.
	 * Used to well separated planets.
	 */
	public boolean Intersect(Planet p) {
		int sumRadius = this.Radius + p.Radius + 10;// /20px space between each
		int diffX = this.X - p.X;
		int diffY = this.Y - p.Y;
		return diffX * diffX + diffY * diffY < sumRadius * sumRadius;
	}

	/**
	 *  Look if this planet share pixels with an other planets.
	 */
	public boolean IntersectWithOther(ArrayList<Planet> others) {
		for (Planet other : others) {
			if (this.Intersect(other)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Look if the direct way from this planet to another planet is direct for ships (i.e do not cross an other planet).
	 * Used by IA to attack only accessible planet.
	 *
	 * Note: This is done by checking there is no planet for each pixel along the line from the 2 planet's centers.
	 */
	public boolean CanGoTo(Planet destination, PlanetsMap planetsMap) {

		if (destination == this) {
			return false;
		}
		if (destination.IsBlackhole) {
			return false;
		}
		int sourceX = this.X;
		int sourceY = this.Y;
		int dX = destination.X - this.X;
		int dY = destination.Y - this.Y;
		double destinationAngus = Math.atan2(dY, dX);
		int distance = (int) Math.sqrt(dX * dX + dY * dY);
		double cosAngus = Math.cos(destinationAngus);
		double sinAngus = Math.sin(destinationAngus);
		for (int i = 0; i < distance; i += 10) {
			int px = sourceX + (int) (i * cosAngus);
			int py = sourceY + (int) (i * sinAngus);
			Planet crossedPlanet = planetsMap.GetPlanet(px, py);
			if (crossedPlanet == null || crossedPlanet == this) {
				continue;
			}
			if (crossedPlanet == destination) {
				return true;
			}
			return false;
		}
		return true;
	}

	/**
	 * Increase the number of soldier (Growing)
	 */
	public void updatePhysics(long time) {
		long lastTime = this.lastPhysicTime;
		int duration = (int) (time - lastTime);
		if (duration < (GalaxyDomination.isSpeedyMode ? 500 : 1000)) {
			return;
		}
		this.lastPhysicTime = time;
		if (this.Player == null) {
			return;
		}
		int soldierCount = this.SoldiersCount;
		int soldierTooMuch = soldierCount - this.MaxSoldiers;
		if (soldierTooMuch < 0) {
			int growing = this.chanceToGrow;
			int incrementOver100 = (growing/100);///=2 for growing=253
			this.SoldiersCount += incrementOver100;///+2
			growing -= 100 * incrementOver100;///-200 => growing=53

			int rand = random.nextInt(100);
			if (rand < growing) {
				this.SoldiersCount = soldierCount + 1;
			}
		} else {
			if (soldierTooMuch > 15) {
				this.SoldiersCount = soldierCount - 1;
			} else {// /Create an effect to do not stop too fast
				int rand = random.nextInt(15);
				if (rand < soldierTooMuch) {
					this.SoldiersCount = soldierCount - 1;
				} else {
					this.SoldiersCount = soldierCount + 1;
				}
			}
		}
	}

	/**
	 * Draw the planet on screen.
	 *
	 * @param canvas: the screen
	 * @param screenRollAngus: the phone angus, to make soldierCount text readable even if the phone is in horizontal.
	 */
	public void doDraw(Canvas canvas, float screenRollAngus) {
		int x = this.X;
		int y = this.Y;

		canvas.drawBitmap(this.planetBitmap, x - this.Radius, y - this.Radius,
				null);
		canvas.drawCircle(x, y, this.Radius, this.paint);
		String text = "" + this.SoldiersCount;

		canvas.save();
		// if(!isSreenPortrait){
		canvas.rotate(screenRollAngus + 90, x, y);
		// }
		if (this.Player == null || this.SoldiersCount < this.MaxSoldiers) {
			canvas.drawText(text, x /*- 7*/, y + (4 * CampaignMap.mapToScreenScale), this.fontPaint);
		} else {
			canvas.drawText(text, x /*- 7*/, y + (4 * CampaignMap.mapToScreenScale), this.fontPaintOnMax);
		}
		// if(!isSreenPortrait){
		canvas.restore();
		// }
	}

}
