package com.universe.defender;
import java.util.ArrayList;
import java.util.HashMap;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

/**
 * Manage the play of a level until the player win or loose.
 *
 * The level can be resume on:
 *   - Init: loadgame()
 *   - while(nobody loose)
 *   	- updatePhysique()
 *      - doDraw()
 *   - give back the hand to the GalaxyDomination class.
 *
 *   Note: there is 2 threads:
 *   	- the "run-thread": that do playing (physic & draw)
 *   	- the "gui-thread": the activity thread, listen the "interruption" (ex: player touch screen)
 *
 */
public final class GalaxyThread extends Thread {

	public SurfaceHolder surfaceHolder;//Kind of locker need for Android 2D (?)
	private Handler handler;//Android Interface to invoke method on the original-GUI thread

	private boolean running = true;//run-thread loop as long this boolean is true.
	private boolean isPaused = true;//Has application focus or not? Used by the run-thread, set by the GUI-thread
	private boolean isGamePlaying = false;//False if level is loading or if the level is win/lose

	private float screenRollAngus;//Orientation of the phone

	public GalaxyThread(SurfaceHolder surfaceHolder, Context context,
			Handler handler, boolean isSreenPortrait) {
		this.surfaceHolder = surfaceHolder;
		this.handler = handler;
		this.screenRollAngus = -90;
		this.background = new Background(context);
	}

	/**
	 * Called by GUI-thread
	 */
	public void onOrientationChanged(float screenRollAngus) {
		synchronized (this.surfaceHolder) {
			this.screenRollAngus = screenRollAngus;
		}
	}

	/**
	 * Called by GUI-thread
	 * Ask the run-thread to exit.
	 */
	public void Exit() {
		this.running = false;
		while (true) {
			try {
				this.join();
				return;
			} catch (InterruptedException e) {
			}
		}
	}

	/**
	 * Called by run-thread
	 * Invoke the GalaxyView.onHandle method on gui-thread.
	 *
	 * (i.e: used to give job to the GUI-thread)
	 */
	private void sendMessage(String text) {
		Message m = this.handler.obtainMessage();
		Bundle b = new Bundle();
		b.putString("text", text);
		m.setData(b);
		this.handler.sendMessage(m);
	}
	/**
	 * Called by the run-thread
	 * Ask gui-thread to play a sound.
	 */
	public void sendPlaySound(int soundIndex) {
		Message m = this.handler.obtainMessage();
		Bundle b = new Bundle();
		b.putInt("sound", soundIndex);
		m.setData(b);
		this.handler.sendMessage(m);
	}
	/**
	 * Called by the run-thread
	 * Ask gui-thread to display text on the GalaxyView.
	 */
	public void sendMessageLoad(String textProgress) {
		this.sendMessage(this.gameMessage + "\n\n" + this.loadingPhase + "\n"
				+ textProgress);
	}

	/**
	 * Called by GUI-thread
	 *
	 * Notify the user is touching the screen.
	 */
	public boolean onTouchEvent(MotionEvent event) {
		synchronized (this.surfaceHolder) {
			if (this.planetSelection != null) {
				this.planetSelection.onTouchEvent(event);
			}
		}

		return true;
	}
	/**
	 * Called by GUI-thread.
	 * Notify the application do not have focus anymore (or not)
	 * @param b: true if application must be paused, false is level can continue.
	 */
	public void Pause(boolean b) {
		android.util.Log.d("GalaxyThread", "SetPause " + b);
		synchronized (this.surfaceHolder) {
			this.isPaused = b;
		}
	}




	/**          Funny part here                  **/





	private Background background;//Screen background (Galaxy object)
	private Player[] players;//Players of the level. 1 user + N IA.
	private Planet[] planets;//Planets of the level.
	private HashMap<Attack, Object> attacks = new HashMap<Attack, Object>();//All the Attacks currently moving on screen
	private PlanetSelection planetSelection;//Currently selected planets by the user (by touching the screen).

	private PlanetsMap planetsMap;//Cache to have quickly the planet from a pixel.
	private long lastTime = 0;//Last time physic had been updated. (Can be the future to do some wait effect)
	private long timeToCleanTheHeader = 0;//Time limit where the loading header disappears
	private String gameMessage = "";//Things to display during loading
	private String loadingPhase = "";//Things to display during loading

	/**
	 * Called by GUI-thread.
	 *
	 *  Start the run-thread.
	 */
	public void StartGame(Player[] players, Planet[] planets, String header) {
		this.sendMessage("");
		Log.d("GalaxyThread", "doStart");
		synchronized (this.surfaceHolder) {
			this.planets = planets;
			this.players = players;
			this.attacks.clear();
			this.gameMessage = header;
			this.start();
		}
	}

	/**
	 * Called by run-thread. Entry point of the run-thread.
	 *
	 * Load game and run it (until the GUI-thread ask to stop)
	 */
	@Override
	public void run() {
		android.util.Log.d("GalaxyThread", "RUN");
		this.loadGame();
		while (this.running) {

			if (this.isPaused) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
				}
				continue;
			}
			if (!this.isGamePlaying) {
				continue;
			}
			Canvas c = null;
			try {
				c = this.surfaceHolder.lockCanvas(null);
				synchronized (this.surfaceHolder) {
					this.updatePhysics();
					this.doDraw(c);
					this.showFPS();
				}
			} finally {
				if (c != null) {
					this.surfaceHolder.unlockCanvasAndPost(c);
				}
			}
		}
		android.util.Log.d("GalaxyThread", "ENDRUN");
	}

	/**
	 * Do all the level computations to init it.
	 */
	private void loadGame() {
		this.background.init();
		this.loadingPhase = GalaxyDomination.resssource.getString(R.string.LoadingPlanetsMap);
		this.planetsMap = new PlanetsMap(this, planets,
				CampaignMap.screenWidth, CampaignMap.screenHeight, -1);
		this.loadingPhase = GalaxyDomination.resssource.getString(R.string.LoadingSelectZone);
		this.planetSelection = new PlanetSelection(this, this.planets,
				CampaignMap.screenWidth, CampaignMap.screenHeight);
		this.loadingPhase = GalaxyDomination.resssource.getString(R.string.LoadingTrajectory);
		Player.Init(this, this.players, this.planetsMap);
		this.loadingPhase = "";
		android.util.Log.d("GalaxyThread", "doStart ["
				+ CampaignMap.screenWidth + "x" + CampaignMap.screenHeight
				+ "]");
		if (this.gameMessage != null && this.gameMessage != "") {
			this.sendMessage(this.gameMessage + "\n\n" + "Start");
			this.timeToCleanTheHeader = System.currentTimeMillis() + 3000;
		} else {
			this.sendMessage("");
		}
		this.lastTime = System.currentTimeMillis() + 800;
		this.lastShowTime = this.lastTime;
		this.isPaused = false;
		this.isGamePlaying = true;
	}

	/**
	 * Look if player (or his team mate) is alive.
	 */
	private boolean isPlayerTeamAlive()
	{
		for (Planet planet : this.planets) {
			Player owner = planet.Player;
			if (owner == null) {
				continue;
			}
			if(owner.Team == 0)
			{
				return true;
			}
		}
		for (Attack attack : this.attacks.keySet()) {
			Player owner = attack.Player;
			if (owner == null) {
				continue;
			}
			if(owner.Team == 0)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Look if IA player is alive.
	 */
	private boolean isBadGuyTeamAlive()
	{
		for (Planet planet : this.planets) {
			Player owner = planet.Player;
			if (owner == null) {
				continue;
			}
			if(owner.Team != 0)
			{
				return true;
			}
		}
		for (Attack attack : this.attacks.keySet()) {
			Player owner = attack.Player;
			if (owner == null) {
				continue;
			}
			if(owner.Team != 0)
			{
				return true;
			}
		}
		return false;
	}

	private long lastCheckEndedTime = System.currentTimeMillis();//Just check sometime.
	private boolean gameEnded = false;//Once a player die, do not exit the level to quickly, wait a few seconds.
	/**
	 * Called by run-thread.
	 *
	 * Check if the player is dead (lost) or the IA is dead (win).
	 * If the level is ended, this method inform the GUI-thread (by invoking)
	 */
	private void checkEnded(long now) {

		if(gameEnded)//Set by a previous checkEnded()
		{
			if (now - this.lastCheckEndedTime < 2000) {///Let level run a few seconds before terminate it.
				return;
			}
			// /Game ended
			this.isGamePlaying = false;
			if (!this.isBadGuyTeamAlive()) {
				this.sendMessage("win");
			} else {
				this.sendMessage("lost");
			}
		}
		//TODO: if (now - this.lastCheckEndedTime < 1000) {
		if (now - this.lastCheckEndedTime < 3000) {//No need to check too often.
			return;
		}
		this.lastCheckEndedTime = now;
		boolean isUserAlive = this.isPlayerTeamAlive();
		boolean isComputerAlive = this.isBadGuyTeamAlive();
		if(isUserAlive && isComputerAlive)
		{
			return;///continue playing
		}
		this.gameEnded = true;

	}

	/**
	 * Called by run-thread.
	 *
	 * Move objects and so on. (i.e. do all the game logic)
	 */
	private void updatePhysics() {
		long now = System.currentTimeMillis();
		this.background.doPhysic(now);
		int dt = (int) (now - this.lastTime);
		if (dt <= 0) {///Do not start too early, let time to the user to see the map.
			return;
		}
		if (this.timeToCleanTheHeader != 0) {
			if (now > this.timeToCleanTheHeader) {
				this.timeToCleanTheHeader = 0;
				this.sendMessage("");
			}
		}
		this.lastTime = now;
		for (Planet p : this.planets) {
			p.updatePhysics(now);
		}
		ArrayList<Attack> toDelete = new ArrayList<Attack>();
		for (Attack a : this.attacks.keySet()) {
			boolean ended = a.doPhysic(this, this.planetsMap, now);
			if (ended) {
				toDelete.add(a);
			}
		}
		for (Attack a : toDelete) {
			this.attacks.remove(a);
		}
		for (Player p : this.players) {
			p.Play(this, this.planets, now);
		}
		this.checkEnded(now);
	}

	/**
	 * Called by run-thread.
	 *
	 * draw objects on screen
	 */
	private void doDraw(Canvas canvas) {
		if (canvas == null) {
			return;
		}
		// canvas.drawColor(Color.BLACK);
		this.background.doDraw(canvas);
		for (Planet p : this.planets) {
			p.doDraw(canvas, this.screenRollAngus);
		}
		for (Attack a : this.attacks.keySet()) {
			a.doDraw(canvas);
		}
		this.planetSelection.doDraw(canvas);
	}

	private int nbLoop = 0;//For debug
	private long lastShowTime = 0;//For debug
	/**
	 * For debug: show the number of doDraw() per seconds
	 *
	 * On emulator, it is around 17 images per seconds
	 */
	private void showFPS() {
		this.nbLoop++;
		long now = System.currentTimeMillis();
		long dt = now - this.lastShowTime;
		if (dt < 5000) {
			return;
		}
		float fps = ((float) this.nbLoop) * 1000 / dt;
		this.lastShowTime = now;
		this.nbLoop = 0;
		Log.d("FPS", "" + fps);
	}

	/**
	 * Called by by gui-thread (user) and run-thread (IA)
	 *
	 * Send the half of the soldiers from source planet to a trip to the destination planet.
	 */
	public void Attack(Planet source, Planet destination) {
		if (source != destination) {
			if (source.Player != null) {
				Attack attack = new Attack(source, destination);
				this.attacks.put(attack, null);
			}
		}
	}

}
