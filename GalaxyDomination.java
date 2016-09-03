package com.universe.defender;
import java.util.HashMap;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;


/**
 *
 * Entry point of the application.
 *
 *
 * Mainly, this class :
 *     - manage the different views (the main menu view, the options view, etc...) to show on screen,
 *     - start a level (go have a look on GalaxyThread.java if you want to see the funny part of the code)
 *     - save game state (i.e. the level and the options)
 *     - deal with the Android system. Ex: pause the application on lost focus.
 *

 *    emulator -avd MyAvd -prop persist.sys.language=cn
 */
public final class GalaxyDomination extends Activity {
	private MenuItem menuItemMenu;//part of the menu displayed when user click on the phone "menu" key
	private MenuItem menuItemClose;//part of the menu displayed when user click on the phone "menu" key

	private GalaxyView view;//The view of a level. Null when player is not playing a level
	private GalaxyThread thread;//The level playing manager. Null when player is not playing a level
	private Bitmap planetBitmap;//Cache of the gray-neutral-planet image to avoid to have multiple instance
	private SensorManager sensorManager;//The android object that indicate the gravity vetor
	private PowerManager.WakeLock wakeLock;//The android object that avoid the screen dim. http://www.androidsnippets.org/snippets/53/

	private int playerForce = 10;//How strong the user player is strong in attack
	private int playerDefense = 10;	//How strong the user player have planet hard to take
	private int playerSpeed = 10;//Speed of the user player's ship during an attack (during the move from one planet to another)
	private int playerGrowing = 10;//How the user player's soldiers reproduce fast on planet
	private int campaignLevel = 1;//Current level of the user in the campaign
	private int level2v2 = 1;//Current level of the user in 2 versus 2 mode
	private boolean isCustomLevelEnabled = false;//Can user choose to play a specific campaign level Custom-Level?
	private int customLevel = 25;//Default level the user will play on Custom-Level
	private int customLevelForce = 15;//Default Force the user will have on Custom-Level
	private int customLevelDefense = 15;//Default Defense the user will have on Custom-Level
	private int customLevelGroowing = 20;//Default Growing the user will have on Custom-Level
	private int customLevelSpeed = 15;//Default Speed the user will have on Custom-Level
	private int customLevelAILevel = 25;//Default AI characteristic the AI will have on Cutom-Level


	public static int aiType = 1;//AI level: 1 for Random, 2 for Nearest, 3 AttackWeaker
	public static boolean isHellMode = false;//Is player want to play against very strong AI?
	public static boolean isSpeedyMode = false;//Is player wants level be played on accelerated?
	public static boolean isLowPerf = false;//Is the player wants reduced graphics?
	public static boolean isSoundEnabled = true;//Is the player wants all the sounds to be played?
	private static Context context;//Interface to the android system
	public static Resources resssource;//Access to files saved in the folder "res/"


	/**
	 * Indicate to Android this application have the menu "Menu" and "Close"
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		this.menuItemMenu = menu.add(GalaxyDomination.resssource
				.getString(R.string.Menu));
		this.menuItemClose = menu.add(GalaxyDomination.resssource
				.getString(R.string.Close));
		return true;
	}

	/**
	 * (Method called by Android: Called after user press a menu)
	 *
	 * Action of menus
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		try {
			this.stopCurrentGame();
			if (item == this.menuItemMenu) {
				this.showFormMenuMain();
				return true;
			}
			if (item == this.menuItemClose) {
				this.finish();
			}
		} catch (Exception ex) {
			Log.e("Error", "" + ex);
		}
		return false;
	}

	/**
	 *  (Method called by Android: Called when the activity is first created)
	 *
	 *  Init class attributes when application start.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		android.util.Log.d("Activity", "onCreate");
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		// turn off the window's title bar
		this.setFullscreen();
		CampaignMap.initScreenSize(getWindowManager().getDefaultDisplay());

		Log.i("Activity", "Screen [" + CampaignMap.screenWidth + "x"
				+ CampaignMap.screenHeight + "]");
		this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		this.wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK,
				"DoNotDimScreen");
		GalaxyDomination.context = this.getBaseContext();
		GalaxyDomination.resssource = GalaxyDomination.context.getResources();
		this.planetBitmap = BitmapFactory.decodeResource(GalaxyDomination.resssource,
				R.drawable.planet3);
		this.initSounds(GalaxyDomination.context);
		this.loadSaveGame();
	}

	private static AudioManager  audioManager;//Android object that is able to play sounds
	private static SoundPool soundPool;//Collections of sounds (play list).
	private static float soundVolume;//A parameter the audio manager need. Constant.
	public static int soundPlanetWinIndex;// Planet win sound
	public static int soundPlanetLostIndex;// Planet lost sound
	public static int soundLoseIndex;//Level lost sound
	public static int soundWinIndex;//Level win sound
	public static int soundShootIndex;//PLayer attack sound

	/**
	 * Init the audioManager and the soundPool.
	 */
	public void initSounds(Context context) {
		GalaxyDomination.soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
		GalaxyDomination.audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
		GalaxyDomination.soundPlanetWinIndex = GalaxyDomination.soundPool.load(context, R.raw.planetwin, 1);
		GalaxyDomination.soundPlanetLostIndex = GalaxyDomination.soundPool.load(context, R.raw.planetlost, 1);
		GalaxyDomination.soundLoseIndex = GalaxyDomination.soundPool.load(context, R.raw.lose, 1);
		GalaxyDomination.soundWinIndex = GalaxyDomination.soundPool.load(context, R.raw.win, 1);
		GalaxyDomination.soundShootIndex = GalaxyDomination.soundPool.load(context, R.raw.shoot, 1);

		float steamVolume = GalaxyDomination.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		float steamMaxVolume = GalaxyDomination.audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		GalaxyDomination.soundVolume = steamVolume / steamMaxVolume;
	}

	/**
	 * Play a sound.
	 */
	public static void PlayPlanetSound(int soundIndex)
	{
		if(!GalaxyDomination.isSoundEnabled)
		{
			if(soundIndex == soundShootIndex)
			{
				return;
			}
			if(soundIndex == soundPlanetWinIndex)
			{
				return;
			}
		}
		GalaxyDomination.soundPool.play(soundIndex, GalaxyDomination.soundVolume, GalaxyDomination.soundVolume, 1, 0, 1f);

	}

	/**
	 * Ask Android to show this application in full screen
	 * (without the menu that indicate the time and the battery level)
	 */
	public void setFullscreen() {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}

	/**
	 * (Method called by Android: Called after the activity is created or restarted)
	 *
	 * Start the game: show the main-menu.
	 *
	 * (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		super.onStart();
		this.showFormMenuMain();
	}

	/**
	 * (Method called by Android)
	 *
	 * Clean up
	 */
	@Override
	public void onDestroy() {
		android.util.Log.d("Activity", "onDestroy");
		if (this.view != null) {
			this.view.onExit();
		}
		super.onDestroy();
	}

	/**
	 * (Method called by Android: after the application lose focus)
	 *
	 * Put level in pause
	 */
	@Override
	public void onPause() {
		android.util.Log.d("Activity", "onPause");
		if (this.thread != null) {
			this.thread.Pause(true);
		}
		this.wakeLock.release();
		sensorManager.unregisterListener(mSensorListener);

		super.onPause();
	}

	/**
	 * (Method called by Android: after the application regains focus)
	 *
	 *  Unpause the level
	 */
	@Override
	public void onResume() {
		android.util.Log.d("Activity", "onResume");
		if (this.thread != null) {
			this.thread.Pause(false);
		}
		this.wakeLock.acquire();
		this.sensorManager.registerListener(mSensorListener, sensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_NORMAL);
		super.onResume();
		// super.onConfigurationChanged(newConfig)
	}



	/**
	 * Plug a function on the Android's sensor to find where is projected the gravity on the screen.
	 *
	 * It is used to have the planet's soldier counts that rotate with phone.
	 *
	 * Note that the application is locked in Vertical mode because it is a pain in the ass to deal
	 * with the default Android phone-rotate mechanism. I do not want to destroy completely the application
	 * then recreate it each time the user rotate its phone.
	 */
	private final SensorEventListener mSensorListener = new SensorEventListener() {

		/**
		 * Not used
		 */
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

			Log.d("Sensor", "onAccuracyChanged");
		}

		/**
		 * Compute the angus of the vector 'g' in the screen.
		 *
		 * The idea is:
		 *  If the phone is in horizontal mode, this angus should be -90 degree.
		 *  If the phone is in vertical mode, this angus should be -180 degree.
		 *
		 */
		@Override
		public void onSensorChanged(SensorEvent event) {
			float gX = event.values[SensorManager.DATA_X];
			float gY = event.values[SensorManager.DATA_Y];
			double gAngus = -90;
			if (gX != 0 && gY != 0) {
				double exactAngus = Math.atan2(-gY, gX) * 180 / Math.PI;
				gAngus = 45 * Math.round(exactAngus / 45);///Do not move too often
			}
			Log.d("Sensor", "DataZ=" + gAngus);
			if (view != null) {
				view.onOrientationChanged((float) gAngus);
			}
		}
	};

	/**
	 * Cache of the planet bitmap.
	 *  Index is the size (1, 2 or 3) of the planets.
	 *  Value is the bitmap at the good size.
	 */
	private HashMap<Integer, Bitmap> planetBitmapSized = new HashMap<Integer, Bitmap>();

	/**
	 * Retrieve or create planet bitmap at the good size.
	 */
	public Bitmap GetPlanetBitmap(int radius) {
		Integer i = new Integer(radius);
		if (!this.planetBitmapSized.containsKey(i)) {
			Bitmap resized = Bitmap.createScaledBitmap(this.planetBitmap,
					radius * 2, radius * 2, true);
			this.planetBitmapSized.put(i, resized);
		}
		return this.planetBitmapSized.get(i);
	}

	/**
	 * Stop the current level (if any)
	 */
	private void stopCurrentGame() {
		if (this.view != null) {
			this.view.onExit();
			this.view = null;
			this.thread = null;
		}
	}

	/**
	 * Show the main menu on screen.
	 */
	private void showFormMenuMain() {
		this.stopCurrentGame();
		this.setContentView(R.layout.mainmenu);
		TextView header = (TextView) this.findViewById(R.id.TextViewHeader);
		header.setTextColor(Color.GREEN);

		Button tutorialFight = (Button) this.findViewById(R.id.ButtonTutorial);
		tutorialFight.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showFormTutorial(1);
			}
		});
		Button campaign1Fight = (Button) this.findViewById(R.id.ButtonCampaign);
		campaign1Fight.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				playGame("campaign");
			}
		});
		Button customFight = (Button) this.findViewById(R.id.ButtonCustomFight);
		customFight.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showFormCustomFight();
			}
		});
		if(!this.isCustomLevelEnabled)
		{
			//android.util.Log.d("GalaxyDomination", "isCustomLevelEnabled");
			customFight.setVisibility(View.GONE);
		}
		Button randomFight = (Button) this.findViewById(R.id.ButtonRandomFight);
		randomFight.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				playGame("random");
			}
		});
		Button team2v2 = (Button) this.findViewById(R.id.Team2v2);
		team2v2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				playGame("team2v2");
			}
		});
		Button options = (Button) this.findViewById(R.id.ButtonOptions);
		options.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showFormOptions();
			}
		});
		Button reset = (Button) this.findViewById(R.id.ButtonResetStats);
		reset.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showFormResetStats();
			}
		});
		TextView stat = (TextView) this.findViewById(R.id.TextViewPlayerStat);
		stat.setTextColor(Color.GRAY);
		stat.setText(this.getCurrentStatsDisplay() + " - " + GalaxyDomination.resssource.getString(R.string.level) +
				" " + this.campaignLevel + " - " + " 2v2: " + this.level2v2);

	}

	/**
	 * Show the options view on screen.
	 */
	private void showFormOptions() {
		this.stopCurrentGame();
		this.setContentView(R.layout.options);

		RadioButton ia1 = (RadioButton) this.findViewById(R.id.RadioButtonIA1);
		RadioButton ia2 = (RadioButton) this.findViewById(R.id.RadioButtonIA2);
		RadioButton ia3 = (RadioButton) this.findViewById(R.id.RadioButtonIA3);
		CheckBox hellMode = (CheckBox) this.findViewById(R.id.CheckBoxHell);
		CheckBox speedyMode = (CheckBox) this.findViewById(R.id.CheckSpeedyMode);
		CheckBox lowPerf = (CheckBox) this.findViewById(R.id.CheckBoxLowPerf);
		CheckBox sound = (CheckBox) this.findViewById(R.id.CheckBoxSound);

		ia1.setChecked(GalaxyDomination.aiType == 1);
		ia2.setChecked(GalaxyDomination.aiType == 2);
		ia3.setChecked(GalaxyDomination.aiType == 3);
		hellMode.setChecked(GalaxyDomination.isHellMode);
		speedyMode.setChecked(GalaxyDomination.isSpeedyMode);
		lowPerf.setChecked(GalaxyDomination.isLowPerf);
		sound.setChecked(GalaxyDomination.isSoundEnabled);

		Button ok = (Button) this.findViewById(R.id.ButtonOk);
		ok.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onOptionsOk();
			}
		});
		Button cancel = (Button) this.findViewById(R.id.ButtonCancel);
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showFormMenuMain();
			}
		});

	}

	/**
	 * Show the Reset view on screen.
	 */
	private void showFormResetStats() {
		this.stopCurrentGame();
		this.setContentView(R.layout.resetstats);

		TextView stat = (TextView) this.findViewById(R.id.TextViewPlayerStat);
		stat.setTextColor(Color.GRAY);
		stat.setText(this.getCurrentStatsDisplay() + " - "
				+ GalaxyDomination.resssource.getString(R.string.level) + " "
				+ this.campaignLevel + " - 2v2 " + this.level2v2);

		Button delete = (Button) this.findViewById(R.id.ButtonDelete);
		delete.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onOptionsDelete();
			}
		});
		Button cancel = (Button) this.findViewById(R.id.ButtonCancel);
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showFormMenuMain();
			}
		});
	}

	/**
	 * Show the "end campaign" view on screen.
	 */
	private void showFormEndCampaign() {
		this.stopCurrentGame();
		this.setContentView(R.layout.endcampaign);

		Button menu = (Button) this.findViewById(R.id.ButtonMenu);
		menu.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showFormMenuMain();
			}
		});
	}

	/**
	 * Show the "You lose" view on screen.
	 */
	private void showFormGameLost() {
		this.stopCurrentGame();
		this.setContentView(R.layout.gamelost);

		Button menu = (Button) this.findViewById(R.id.ButtonMenu);
		menu.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showFormMenuMain();
			}
		});
		Button retry = (Button) this.findViewById(R.id.ButtonRetry);
		retry.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				playGame(campaignName);
			}
		});
	}

	/**
	 * Show the "You win" view on screen.
	 */
	private void showFormGameWinRandom() {
		this.stopCurrentGame();
		this.setContentView(R.layout.gamewinrandom);

		Button menu = (Button) this.findViewById(R.id.ButtonMenu);
		menu.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showFormMenuMain();
			}
		});
	}

	/**
	 * Show the custom fight view on screen.
	 */
	private void showFormCustomFight()
	{
		this.stopCurrentGame();
		this.setContentView(R.layout.customlevel);
		((EditText) this.findViewById(R.id.lvl)).setText("" + this.customLevel);
		((EditText) this.findViewById(R.id.Force)).setText("" + this.customLevelForce);
		((EditText) this.findViewById(R.id.Defense)).setText("" + this.customLevelDefense);
		((EditText) this.findViewById(R.id.Growing)).setText("" + this.customLevelGroowing);
		((EditText) this.findViewById(R.id.Speed)).setText("" + this.customLevelSpeed);
		((EditText) this.findViewById(R.id.AILevel)).setText("" + this.customLevelAILevel);

		Button ok = (Button) this.findViewById(R.id.ButtonOk);
		ok.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				customFightOk();
			}
		});
		((Button) this.findViewById(R.id.ButtonCancel)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showFormMenuMain();
			}
		});
	}

	/**
	 * Get the integer that is currently displayed in a textBox (user input).
	 */
	private int  getIntFromTextView(int idTextView, int defaultValue)
	{
		String s = ((EditText)this.findViewById(idTextView)).getText().toString();
		int i = 1;
		try{
		i = Integer.parseInt(s);
		} catch(NumberFormatException e)
		{}
		//android.util.Log.d("GalaxyDomination", "getIntFromTextView=> " + s + " - "+ i);
		if(i < 1)
		{
			return 1;
		}
		return i;
	}

	/**
	 * Start the CustomFight level when user had press "OK" on the CustomFight view
	 */
	private void customFightOk()
	{
		this.customLevel = getIntFromTextView(R.id.lvl, 1);
		this.customLevelForce = getIntFromTextView(R.id.Force, 1);
		this.customLevelDefense = getIntFromTextView(R.id.Defense, 1);
		this.customLevelGroowing = getIntFromTextView(R.id.Growing, 1);
		this.customLevelSpeed = getIntFromTextView(R.id.Speed, 1);
		this.customLevelAILevel = getIntFromTextView(R.id.AILevel, this.customLevel);
		this.savePlayerToFile();
		this.playGame("customFight");

	}

	private int tutorialCurrentPage = 0;//Progression of the tutorial by the user.

	/**
	 * Show the view corresponding to the page X of the 10 pages tutorial.
	 */
	private void showFormTutorial(int page) {
		this.stopCurrentGame();
		this.tutorialCurrentPage = page;
		this.setContentView(R.layout.tutorial);
		Button next = (Button) this.findViewById(R.id.ButtonNext);
		next.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showFormTutorial(tutorialCurrentPage + 1);
			}
		});
		TextView textView = (TextView) this.findViewById(R.id.TextViewContent);
		ImageView imageView = (ImageView) this.findViewById(R.id.ImageView);

		switch (page) {
		case 1:
			textView.setText(GalaxyDomination.resssource
					.getString(R.string.TutoYouAreGreen));
			imageView.setImageResource(R.drawable.tutoyouaregreen);
			break;
		case 2:
			textView.setText(GalaxyDomination.resssource
					.getString(R.string.TutoPopulation));
			imageView.setImageResource(R.drawable.tutopopulation);
			break;
		case 3:
			textView.setText(GalaxyDomination.resssource
					.getString(R.string.TutoAi));
			imageView.setImageResource(R.drawable.tutoai);
			break;
		case 4:
			textView.setText(GalaxyDomination.resssource
					.getString(R.string.TutoWin));
			imageView.setImageResource(R.drawable.tutowin);
			break;
		case 5:
			textView.setText(GalaxyDomination.resssource
					.getString(R.string.TutoAttack));
			imageView.setImageResource(R.drawable.tutoattack);
			break;
		case 6:
			this.playGame("tutorial");
			break;
		case 7:
			textView.setText(GalaxyDomination.resssource
					.getString(R.string.TutoCampaign));
			break;
		case 8:
			textView.setText(GalaxyDomination.resssource
					.getString(R.string.TutoStats));
			break;
		case 9:
			textView.setText(GalaxyDomination.resssource
					.getString(R.string.TutoAiType));
			break;
		case 10:
			textView.setText(GalaxyDomination.resssource
					.getString(R.string.TutoAiStats));
			break;
			default:
				this.showFormMenuMain();
		}
	}

	/**
	 * Show the "You win the tutorial dummy game" view on screen.
	 * (It is not exactly the same than the basic "You win" form.
	 */
	private void showFormGameWinTuto() {
		this.stopCurrentGame();
		this.setContentView(R.layout.gamewintuto);

		Button menu = (Button) this.findViewById(R.id.ButtonNext);
		menu.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showFormTutorial(tutorialCurrentPage + 1);
			}
		});
	}

	/**
	 * Show the "Which characteristic increase?" view on screen.
	 */
	private void showFormIncreasePlayerStat() {
		this.stopCurrentGame();
		this.setContentView(R.layout.increaseplayerstat);
		TextView stat = (TextView) this.findViewById(R.id.TextViewPlayerStat);
		stat.setText(this.getCurrentStatsDisplay() + "\n");

		Button force = (Button) this.findViewById(R.id.ButtonForce);
		force.setTextColor(Color.RED);
		force.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				playerIncrease("force");
			}
		});
		Button defense = (Button) this.findViewById(R.id.ButtonDefense);
		defense.setTextColor(Color.BLUE);
		defense.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				playerIncrease("defense");
			}
		});
		Button speed = (Button) this.findViewById(R.id.ButtonSpeed);
		speed.setTextColor(Color.YELLOW);
		speed.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				playerIncrease("speed");
			}
		});
		Button growing = (Button) this.findViewById(R.id.ButtonGrowing);
		growing.setTextColor(Player.color_PINK);
		growing.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				playerIncrease("growing");
			}
		});
	}

	/**
	 * Get a string that summarize the user campaign's characteristic.
	 */
	private String getCurrentStatsDisplay() {
		return GalaxyDomination.resssource.getString(R.string.YourStatsAre)
				+ " " + this.playerForce + "/" + this.playerDefense + "/"
				+ this.playerSpeed + "/" + this.playerGrowing;
	}

	/**
	 * Save user preferences when user changes options on view "Options".
	 */
	private void onOptionsOk() {
		Log.d("GalaxyDomination", "onOptionsOk");
		// RadioButton ia1 = (RadioButton)
		// this.findViewById(R.id.RadioButtonIA1);
		RadioButton ia2 = (RadioButton) this.findViewById(R.id.RadioButtonIA2);
		RadioButton ia3 = (RadioButton) this.findViewById(R.id.RadioButtonIA3);
		CheckBox hellMode = (CheckBox) this.findViewById(R.id.CheckBoxHell);
		CheckBox speedyMode = (CheckBox) this.findViewById(R.id.CheckSpeedyMode);
		CheckBox lowPerf = (CheckBox) this.findViewById(R.id.CheckBoxLowPerf);
		CheckBox sound = (CheckBox) this.findViewById(R.id.CheckBoxSound);
		if (ia2.isChecked()) {
			GalaxyDomination.aiType = 2;
		} else if (ia3.isChecked()) {
			GalaxyDomination.aiType = 3;
		} else {
			GalaxyDomination.aiType = 1;
		}
		GalaxyDomination.isHellMode = hellMode.isChecked();
		GalaxyDomination.isSpeedyMode = speedyMode.isChecked();
		GalaxyDomination.isLowPerf = lowPerf.isChecked();
		GalaxyDomination.isSoundEnabled = sound.isChecked();
		this.savePlayerToFile();
		this.showFormMenuMain();
	}

	/**
	 * Reset user campaign progression when user press the "delete" button in options view
	 */
	private void onOptionsDelete() {
		CheckBox confirmDelete = (CheckBox) this
				.findViewById(R.id.CheckBoxDelete);
		if (confirmDelete.isChecked()) {
			this.deleteSaveGame();
			this.showFormMenuMain();
		}
	}

	/**
	 * Begin to init a level:
	 * Create the GalaxyView and the GalaxyThread and display it on the screen.
	 * (map is not yet loaded)
	 */
	private void prepareGame() {
		Context context = this.getBaseContext();

		this.view = new GalaxyView(this, context);
		this.setContentView(this.view);

		TextView textView = new TextView(context);
		textView.setText(GalaxyDomination.resssource.getString(R.string.Loading));
		textView.setTextColor(Color.GREEN);
		textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
		textView.setTextSize(24);
		LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT);
		this.addContentView(textView, params);

		this.view.setTextView(textView);
		this.thread = this.view.getThread();
	}

	/**
	 * Init and start a level.
	 * Note that the first thing the galaxyThread will do is "loading..."
	 */
	private void startGame(CampaignMap map) {
		this.prepareGame();
		this.thread.StartGame(map.Players, map.Planets, map.Header);
	}


	private String campaignName;//type of level that is currently played. Kept to retry on lost.

	/**
	 * Start to play a level of desired type.
	 *
	 * @param campaignName: type of level
	 */
	private void playGame(String campaignName) {
		this.stopCurrentGame();
		this.campaignName = campaignName;
		if (campaignName == "tutorial") {
			Player user = Player.CreateNewPlayer();
			user.Force = 10;
			user.Defense = 10;
			user.Speed = 16;
			user.Growing = 10;
			CampaignMap map = CampaignMap.GetTutorial(this, user);
			this.startGame(map);
		} else if (campaignName == "campaign") {
			Player user = Player.CreateNewPlayer();
			user.Force = this.playerForce;
			user.Defense = this.playerDefense;
			user.Speed = this.playerSpeed;
			user.Growing = this.playerGrowing;
			CampaignMap map = CampaignMap.GetCampaignMap(this, user, this.campaignLevel, this.campaignLevel);
			this.startGame(map);
		} else if (campaignName == "random") {
			Player user = Player.CreateNewPlayer();
			user.Force = 10;
			user.Defense = 10;
			user.Speed = 16;
			user.Growing = 10;
			CampaignMap map = CampaignMap.CreateRandomMap(this, user, this.level2v2, false);
			this.startGame(map);
		} else if (campaignName == "team2v2") {
			Player user = Player.CreateNewPlayer();
			user.Force = 10;
			user.Defense = 10;
			user.Speed = 16;
			user.Growing = 10;
			CampaignMap map = CampaignMap.CreateRandomMap(this, user, this.level2v2, true);
			this.startGame(map);
		}
		else if (campaignName == "customFight") {
			Player user = Player.CreateNewPlayer();
			user.Force = this.customLevelForce;
			user.Defense = this.customLevelDefense;
			user.Speed = this.customLevelSpeed;
			user.Growing = this.customLevelGroowing;
			CampaignMap map = CampaignMap.GetCampaignMap(this, user, this.customLevel, this.customLevelAILevel);
			this.startGame(map);
		}
		else {
			Log.e("GalaxyDomination", "playGame unknow campaignName "
					+ campaignName);
			this.showFormMenuMain();
		}
	}

	/**
	 * Method invoked by the GalaxyThread at the end of a level.
	 *
	 * Stop the level and show the appropriate (lose/win) view on screen.
	 */
	public void gameEnded(boolean hasWin) {
		if(hasWin)
		{
			PlayPlanetSound(soundWinIndex);
		}
		else
		{
			PlayPlanetSound(soundLoseIndex);
		}

		this.stopCurrentGame();
		if (!hasWin) {
			this.showFormGameLost();
			return;
		}
		if (this.campaignName == "tutorial") {
			this.showFormGameWinTuto();
			return;
		}
		if(this.campaignName == "team2v2" )
		{
			this.level2v2++;
			this.savePlayerToFile();
		}
		if (this.campaignName == "random"
			|| this.campaignName == "customFight"
			|| this.campaignName == "team2v2" ) {

			this.showFormGameWinRandom();
			return;
		}
		if (this.campaignName == "campaign") {
			if(this.campaignLevel >= CampaignMap.NbLevel)
			{
				this.isCustomLevelEnabled = true;
			}
			if (this.campaignLevel == CampaignMap.NbLevel)
			{
				this.campaignLevel++;///Continue on random mode

				this.savePlayerToFile();
				this.showFormEndCampaign();
			}
			else
			{
				this.campaignLevel++;
				this.showFormIncreasePlayerStat();
			}
			if(this.campaignLevel >= CampaignMap.NbLevel)
			{
				this.isCustomLevelEnabled = true;
			}
		}
	}

	/**
	 * Increase the selected user characteristic.
	 * Used after a player win a campaign level.
	 */
	private void playerIncrease(String attr) {
		if (attr == "force") {
			this.playerForce++;
		} else if (attr == "defense") {
			this.playerDefense++;
		} else if (attr == "speed") {
			this.playerSpeed++;
		} else if (attr == "growing") {
			this.playerGrowing++;
		} else {
			Log.e("GallaxyDomination", "playerIncrease unknow code " + attr);
		}
		this.savePlayerToFile();
		this.playGame(this.campaignName);
	}

	/**
	 * Save Options and player's characteristic in file to do not loose it when application will close.
	 */
	private void savePlayerToFile() {
		android.util.Log.d("Activity", "savePlayerToFile");
		SharedPreferences settings = getSharedPreferences(
				"GalaxyDominationSettings", 0);
		SharedPreferences.Editor editor = settings.edit();

		editor.putInt("playerForce", this.playerForce);
		editor.putInt("playerDefense", this.playerDefense);
		editor.putInt("playerSpeed", this.playerSpeed);
		editor.putInt("playerGrowing", this.playerGrowing);
		editor.putInt("campaignLevel", this.campaignLevel);
		editor.putInt("level2v2", this.level2v2);
		editor.putInt("aiType", GalaxyDomination.aiType);
		editor.putBoolean("isHellMode", GalaxyDomination.isHellMode);
		editor.putBoolean("isSpeedyMode", GalaxyDomination.isSpeedyMode);
		editor.putBoolean("isLowPerf", GalaxyDomination.isLowPerf);
		editor.putBoolean("isSoundEnabled", GalaxyDomination.isSoundEnabled);

		editor.putBoolean("isCustomLevelEnabled", this.isCustomLevelEnabled);
		editor.putInt("customLevel", this.customLevel);
		editor.putInt("customLevelForce", this.customLevelForce);
		editor.putInt("customLevelDefense", this.customLevelDefense);
		editor.putInt("customLevelGroowing", this.customLevelGroowing);
		editor.putInt("customLevelSpeed", this.customLevelSpeed);
		editor.putInt("customLevelAILevel", this.customLevelAILevel);

		editor.commit();// /Save on disk
	}

	/**
	 * Retrieve values from previous application's save.
	 */
	private void loadSaveGame() {
		SharedPreferences settings = getSharedPreferences(
				"GalaxyDominationSettings", 0);
		if (settings.contains("playerForce")) {
			android.util.Log.d("Activity", "loadSaveGame");
			this.playerForce = settings.getInt("playerForce", 10);
			this.playerDefense = settings.getInt("playerDefense", 10);
			this.playerSpeed = settings.getInt("playerSpeed", 10);
			this.playerGrowing = settings.getInt("playerGrowing", 10);
			this.campaignLevel = settings.getInt("campaignLevel", 1);
			this.level2v2 = settings.getInt("level2v2", 1);
			GalaxyDomination.aiType = settings.getInt("aiType", 1);
			GalaxyDomination.isHellMode = settings.getBoolean("isHellMode", false);
			GalaxyDomination.isSpeedyMode = settings.getBoolean("isSpeedyMode", false);
			GalaxyDomination.isLowPerf = settings.getBoolean("isLowPerf", false);
			GalaxyDomination.isSoundEnabled = settings.getBoolean("isSoundEnabled", true);
		}
		if(settings.contains("isCustomLevelEnabled"))
		{
			this.isCustomLevelEnabled = settings.getBoolean("isCustomLevelEnabled", false);
			this.customLevel = settings.getInt("customLevel", 25);
			this.customLevelForce = settings.getInt("customLevel", 15);
			this.customLevelDefense = settings.getInt("customLevelDefense", 15);
			this.customLevelGroowing = settings.getInt("customLevelGroowing", 20);
			this.customLevelSpeed = settings.getInt("customLevelSpeed", 15);
			this.customLevelAILevel = settings.getInt("customLevelAILevel", 25);
		}
	}

	/**
	 * Restart campaign
	 */
	private void deleteSaveGame() {
		android.util.Log.d("Activity", "deleteSaveGame");
		this.playerForce = 10;
		this.playerDefense = 10;
		this.playerSpeed = 10;
		this.playerGrowing = 10;
		this.campaignLevel = 1;
		this.level2v2 = 1;

//		this.playerForce = 100;
//		this.playerDefense = 100;
//		this.playerSpeed = 30;
//		this.playerGrowing = 10;

		this.savePlayerToFile();
		this.showFormMenuMain();
	}
}