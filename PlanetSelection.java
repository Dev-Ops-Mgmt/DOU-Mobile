package com.universe.defender;
import java.util.HashMap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Planet currently selected by the human player by touching the screen.
 */
public final class PlanetSelection {

	public HashMap<Planet, Object> SelectedPlanets = new HashMap<Planet, Object>();//Selected planets (just the key of the Hashmap is used)
	private PlanetsMap map;//To quickly know if there is a planet on point touched by user
	private GalaxyThread galaxy;
	public Planet SelectedPlanet = null;
	private Paint paint;//Green
	private Paint selectedPaint;//White semi transparent
	private int selectionRadius;//Big white circle

	public PlanetSelection(GalaxyThread galaxy, Planet[] planets, int sWidth,
			int sHeight) {

		this.galaxy = galaxy;
		this.paint = new Paint();
		this.paint.setAntiAlias(true);
		this.paint.setColor(Color.GREEN);
		this.paint.setStrokeWidth(3);
		this.paint.setAlpha(150);
		this.selectedPaint = new Paint();
		this.selectedPaint.setAntiAlias(true);
		this.selectedPaint.setColor(Color.WHITE);
		this.selectedPaint.setStrokeWidth(3);
		this.selectedPaint.setAlpha(150);
		this.selectionRadius = 50;//TODO: = (int) 50 * CampaignMap.mapToScreenScale;
		this.map = new PlanetsMap(galaxy, planets, sWidth, sHeight,
				this.selectionRadius);
	}

	/**
	 * Called by GUI-thread.
	 *
	 * Catch the user-touch-screen event to check if another planet is selected.
	 */
	public void onTouchEvent(MotionEvent event) {
		// Log.d("onTouchEvent", "onTouchEvent");
		int action = event.getAction();
		if (action == MotionEvent.ACTION_CANCEL) {
			this.SelectedPlanets.clear();
			this.SelectedPlanet = null;
			Log.d("ActionCancel", "Clear");
			return;
		}
		int x = (int) event.getX();
		int y = (int) event.getY();
		Planet current = this.map.GetPlanet(x, y);
		// if(current != null){
		// Log.d("Current", "" + current.X + " " + current.Y);
		// }
		this.SelectedPlanet = current;
		if (current != null && current.Player != null
				&& current.Player.AI == null) {
			if (!this.SelectedPlanets.containsKey(current)) {
				this.SelectedPlanets.put(current, null);
				// Log.d("PlanetSelected", "" + current.X + " " + current.Y);
			}
		}
		if (action == MotionEvent.ACTION_UP) {
			if (current != null) {

				this.attack();
			}
			// Log.d("ActionUP", "Clear");
			this.SelectedPlanets.clear();
			this.SelectedPlanet = null;
		}
	}

	/**
	 * Called by GUI-thread
	 *
	 * User attack a planet.
	 */
	private void attack() {
		synchronized (this.galaxy.surfaceHolder) {

			Planet current = this.SelectedPlanet;
			// Log.d("Attack", "" +this.SelectedPlanets.size()+ " --> " +
			// this.SelectedPlanet.X + " " + this.SelectedPlanet.Y);
			this.galaxy.sendPlaySound(GalaxyDomination.soundShootIndex);

			for (Planet p : this.SelectedPlanets.keySet()) {
				if (p.Player != null && p.Player.AI == null) {// /It can change
					this.galaxy.Attack(p, current);
				}
			}
		}

	}

	/**
	 * Called by run-thread.
	 *
	 * Draw the selection on screen.
	 */
	public void doDraw(Canvas canvas) {
		if (this.SelectedPlanets.isEmpty()) {
			return;
		}
		Paint paint = this.paint;
		for (Planet p : this.SelectedPlanets.keySet()) {
			int x = p.X;
			int y = p.Y;
			canvas.drawCircle(x, y, p.Radius + 2, paint);
		}
		if (this.SelectedPlanet != null) {
			int destX = this.SelectedPlanet.X;
			int destY = this.SelectedPlanet.Y;
			for (Planet p : this.SelectedPlanets.keySet()) {
				int x = p.X;
				int y = p.Y;
				canvas.drawLine(x, y, destX, destY, paint);
			}
			canvas.drawCircle(destX, destY, this.selectionRadius,
					this.selectedPaint);
		}
	}

}
