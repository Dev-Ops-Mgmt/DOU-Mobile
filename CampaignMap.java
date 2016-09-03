package com.universe.defender;

import java.util.ArrayList;
import java.util.Random;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;

/**
  *
 * CampaignMap class: the initial set of players and planets with witch we load a level.
 * The static methods generate random maps or "load" predetermined maps.
 *
 * This file should be split in 2 files:
 *   - GallaxyMap : the not static part
 * 	 - GallaxyMapFactory : the builder of GallaxyMap
 */
public final class CampaignMap {

	/**
	 * The funny text that appears during loading.
	 */
	public String Header;
	/**
	 * Human player (in position 0) and IA players
	 */
	public Player[] Players;
	public Planet[] Planets;
	/**
	 * constructor for campaign maps. It convert "data" into class instances.
	 */
	private CampaignMap(GalaxyDomination res, Player user, int gameLevel, int aiLevel,
			int descriptifId, int[] data) {
		if(gameLevel > 0){
		this.Header = GalaxyDomination.resssource.getString(R.string.level) +  " " + gameLevel + "\n" +
			GalaxyDomination.resssource.getString(descriptifId);
		} else{
			this.Header = GalaxyDomination.resssource.getString(descriptifId);
		}
		int aiBonusStats = aiLevel;
		int i = 0;
		int nbPlayers = data[i++];
		this.Players = createPlayers(user, nbPlayers, aiBonusStats);
		this.Planets = new Planet[(data.length - 1) / 5];
		int planetIndex = 0;
		ArrayList<Planet> checkIntersect = new ArrayList<Planet>();
		while (i < data.length) {
			Planet planet = new Planet();
			this.Planets[planetIndex++] = planet;
			planet.X = data[i++];
			planet.Y = CampaignMap.mapHeight - data[i++];// /To thing like in
															// maths.
			if (planet.X < 20 || planet.X > CampaignMap.mapWidth - 20) {
				Log.e("CampaignMap", "planet.X  out of range i=" + i + " x="
						+ planet.X);
			}
			if (planet.Y < 20 || planet.Y > CampaignMap.mapHeight - 20) {
				Log.e("CampaignMap", "planet.Y  out of range i=" + i + " y="
						+ planet.Y);
			}
			planet.X = (int) (planet.X * CampaignMap.mapToScreenWidth);
			planet.Y = (int) (planet.Y * CampaignMap.mapToScreenHeight);
			planet.SetSize(res, data[i++]);
			planet.SoldiersCount = data[i++];
			if (planet.SoldiersCount > 500) {
				planet.IsBlackhole = true;
			}
			int playerIndex = data[i++];
			if (playerIndex != 0) {
				try {
					planet.SetPlayer(this.Players[playerIndex - 1]);
				} catch (ArrayIndexOutOfBoundsException ex) {
					Log.e("CampaignMap", "Index out of range " + playerIndex
							+ " at " + i);
				}
			} else {
				planet.SetPlayer(null);// /init Paint
			}
			if (planet.IntersectWithOther(checkIntersect)) {
				Log.e("CampaignMap", "Planets Intersects i=" + i);
			}
			checkIntersect.add(planet);
		}
	}

	/**
	 * Basic constructor
	 */
	public CampaignMap(Player[] players, Planet[] planets, String header) {
		this.Players = players;
		this.Planets = planets;
		this.Header = header;
	}










	private static Random random = new Random();

	/**
	 * see initScreenSize() comment
	 */
	public static final int mapWidth = 300;//virtual screen size
	public static final int mapHeight = 400;//virtual screen size
	public static int screenWidth;///real screen size
	public static int screenHeight;///real screen size
	public static float mapToScreenWidth;///For X scale
	public static float mapToScreenHeight;///For Y scale
	public static float mapToScreenScale;///For volume scaling (ex: radius)
	/**
	 * Because phone's screens do not have all the same size, we need to resize all at the real phone screen size.
	 * So lot of unit are expressed (in code) in a virtual 300*400 screen and converted into the real coordinate after.
	 *
	 * Example: a planet at virtual position (300, 400) will be placed at final position (320, 480) on
	 *  a 320*480 screen.
	 * Example: a planet with virtual radius=20 will have a final radius=30 on a 450*1500 screen.
	 *
	 */
	public static void initScreenSize(Display display) {
		DisplayMetrics dm = new DisplayMetrics();
		display.getMetrics(dm);
		CampaignMap.screenWidth = dm.widthPixels;
		CampaignMap.screenHeight = dm.heightPixels;

		CampaignMap.mapToScreenWidth = ((float) CampaignMap.screenWidth)
				/ CampaignMap.mapWidth;
		CampaignMap.mapToScreenHeight = ((float) CampaignMap.screenHeight)
				/ CampaignMap.mapHeight;
		CampaignMap.mapToScreenScale = Math.min(CampaignMap.mapToScreenWidth,
				CampaignMap.mapToScreenHeight);
	}

	public static final int NbLevel = 100;
	public static final int NbLevelDemo = 7;

	/**
	 * Set the players attributes for the level.
	 */
	private static Player[] createPlayers(Player user, int count, int aiBonusStats) {
		ArrayList<Player> returned = new ArrayList<Player>();
		returned.add(user);
		for (int i = 1; i < count; i++)
		{
			returned.add(Player.CreateAI(aiBonusStats, i));
		}
		Player[] r = new Player[returned.size()];
		returned.toArray(r);
		Log.d("CampaignMap", "Players.count=" + r.length);
		return r;
	}



	public static CampaignMap CreateRandomMap(GalaxyDomination res, Player user, int level2v2, boolean team2v2) {
		int nbPlayers = random.nextInt(100) < 20 ? 3 : 2;
		if(team2v2)
		{
			nbPlayers = 4;
		}
		int aiBonusStats = level2v2;
		Player[] players = createPlayers(user, nbPlayers, aiBonusStats);
		if(team2v2)
		{
			players[1].Team = 0;
			players[1].Force = 10;
			players[1].Defense = 10;
			players[1].Growing = 10;
			players[1].Speed = 16;

			players[2].Team = 1;
			players[3].Team = 1;
			players[1].Color = android.graphics.Color.argb(255, 120, 200, 0);
			players[2].Color = android.graphics.Color.RED;
			players[3].Color = android.graphics.Color.argb(255, 255, 0, 120);
		}
		Planet[] planets = createRandomPlanets(res);
		for (int i = 0; i < players.length; i++) {
			planets[i].SetPlayer(players[i]);
			planets[i].SoldiersCount = 50;
		}

		return new CampaignMap(players, planets, "");
	}
	/**
	 * For the campaign after the game is ended.
	 */
	public static CampaignMap CreateCampaignRandomMap(GalaxyDomination res, Player user, int lvl) {

		int nbPlayers = 2;
		int r = random.nextInt(100);
		if(r < 5)
		{
			nbPlayers = 5;
		}
		else if(r < 25)
		{
			nbPlayers = 4;
		}
		else if(r < 50)
		{
			nbPlayers = 3;
		}

		Player[] players = createPlayers(user, nbPlayers, lvl * 2 - 100);///more and more harder
		Planet[] planets = createRandomPlanets(res);
		for (int i = 0; i < players.length && i < planets.length; i++)
		{
			planets[i].SetPlayer(players[i]);
			planets[i].SoldiersCount = 50;
		}
		return new CampaignMap(players, planets, "");
	}
	private static Planet[] createRandomPlanets(GalaxyDomination res) {
		int width = CampaignMap.screenWidth;
		int height = CampaignMap.screenHeight;
		ArrayList<Planet> returned = new ArrayList<Planet>();
		int nbPlanet = 5 + random.nextInt(30);
		for (int i = 0; i < nbPlanet || returned.size() < 5; i++) {
			Planet p = new Planet();
			int size = 1 + random.nextInt(2);
			if (random.nextInt(100) < 5) {
				size = 3;
			}
			p.SetSize(res, size);
			p.SoldiersCount = 5 * random.nextInt(9);
			p.X = 10 + p.Radius + random.nextInt(width - 20 - 2 * p.Radius);
			p.Y = 10 + p.Radius + random.nextInt(height - 20 - 2 * p.Radius);
			if (!p.IntersectWithOther(returned)) {
				returned.add(p);
			}
		}
		Planet[] r = new Planet[returned.size()];
		returned.toArray(r);
		return r;
	}





	/**
	 * Get the map from a level number.
	 * See the MapEncodingComment for details about the array of int "data"
	 */
	public static CampaignMap GetCampaignMap(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {

		if(gameLevel > CampaignMap.NbLevel)
		{
			return CreateCampaignRandomMap(res, user, gameLevel);
		}
		switch (gameLevel) {
		case 1:
			return lvl001BasicLevel1(res, user, gameLevel, aiLevel);
		case 2:
			return lvl002BasicLevel2(res, user, gameLevel, aiLevel);
		case 3:
			return lvl003SmallPlanet(res, user, gameLevel, aiLevel);
		case 4:
			return lvl004BigPlanet(res, user, gameLevel, aiLevel);
		case 5:
			return lvl005BigCentralPlanet(res, user, gameLevel, aiLevel);
		case 6:
			return lvl0063DivideToConquer(res, user, gameLevel, aiLevel);
		case 7:
			return lvl007Dollard(res, user, gameLevel, aiLevel);
		case 8:
			return lvl008Circle(res, user, gameLevel, aiLevel);
		case 9:
			return lvl009_2Rives(res, user, gameLevel, aiLevel);
		case 10:
			return lvl010_2X(res, user, gameLevel, aiLevel);
		case 11:
			return lvl011_2Duel(res, user, gameLevel, aiLevel);
		case 12:
			return lvl012_3OneBig(res, user, gameLevel, aiLevel);
		case 13:
			return lvl013_2LotOfSmall(res, user, gameLevel, aiLevel);
		case 14:
			return lvl014_3OneBigFarAway(res, user, gameLevel, aiLevel);
		case 15:
			return lvl015_2BasicLevel(res, user, gameLevel, aiLevel);
		case 16:
			return lvl016_2Texture(res, user, gameLevel, aiLevel);
		case 17:
			return lvl017_2TowBigPlanets(res, user, gameLevel, aiLevel);
		case 18:
			return lvl018_3AlignLotOfSmall(res, user, gameLevel, aiLevel);
		case 19:
			return lvl019_2Path(res, user, gameLevel, aiLevel);
		case 20:
			return lvl020_6NaturalSelection(res, user, gameLevel, aiLevel);
		case 21:
			return lvl021_2H(res, user, gameLevel, aiLevel);
		case 22:
			return lvl022_2NoUnits(res, user, gameLevel, aiLevel);
		case 23:
			return lvl023_2Orion(res, user, gameLevel, aiLevel);
		case 24:
			return lvl024_2Bomb(res, user, gameLevel, aiLevel);
		case 25:
			return lvl025_2TowBigTwoMiddle(res, user, gameLevel, aiLevel);
		case 26:
			return lvl026_Sagittarius(res, user, gameLevel, aiLevel);
		case 27:
			return lvl027_3DirectPath(res, user, gameLevel, aiLevel);
		case 28:
			return lvl028_CanisMajor(res, user, gameLevel, aiLevel);
		case 29:
			return lvl029_3AllTogether(res, user, gameLevel, aiLevel);
		case 30:
			return lvl030_2Smiley(res, user, gameLevel, aiLevel);
		case 31:
			return lvl031_Sagitta(res, user, gameLevel, aiLevel);
		case 32:
			return lvl032_3TowBig(res, user, gameLevel, aiLevel);
		case 33:
			return lvl033_3K(res, user, gameLevel, aiLevel);
		case 34:
			return lvl034_4LotOfSmall(res, user, gameLevel, aiLevel);
		case 35:
			return lvl035_4BigOnBorder(res, user, gameLevel, aiLevel);
		case 36:
			return lvl036_Grus(res, user, gameLevel, aiLevel);
		case 37:
			return lvl037_2Oo(res, user, gameLevel, aiLevel);
		case 38:
			return lvl038_3OnlyBigPlanets(res, user, gameLevel, aiLevel);
		case 39:
			return lvl039_2TooEasy(res, user, gameLevel, aiLevel);
		case 40:
			return lvl040_CircleDot(res, user, gameLevel, aiLevel);
		case 41:
			return lvl041_3BigPlanets(res, user, gameLevel, aiLevel);
		case 42:
			return lvl042_3Ring(res, user, gameLevel, aiLevel);
		case 43:
			return lvl043_2WhereAreMyUnits(res, user, gameLevel, aiLevel);
		case 44:
			return lvl044_2Herculus(res, user, gameLevel, aiLevel);
		case 45:
			return lvl045_FirstBlackHole(res, user, gameLevel, aiLevel);
		case 46:
			return lvl046_Horologium(res, user, gameLevel, aiLevel);
		case 47:
			return lvl047_3LotOfSmall(res, user, gameLevel, aiLevel);
		case 48:
			return lvl048_3Align2Bigs(res, user, gameLevel, aiLevel);
		case 49:
			return lvl049_Lacerta(res, user, gameLevel, aiLevel);
		case 50:
			return lvl050_Orbit(res, user, gameLevel, aiLevel);
		case 51:
			return lvl051_Lepus(res, user, gameLevel, aiLevel);
		case 52:
			return lvl052_Parable(res, user, gameLevel, aiLevel);
		case 53:
			return lvl053_2NoUnitsRemakeWithUnits(res, user, gameLevel, aiLevel);
		case 54:
			return lvl054_2Andromeda(res, user, gameLevel, aiLevel);
		case 55:
			return lvl055_2Path4Players(res, user, gameLevel, aiLevel);
		case 56:
			return lvl056_Leo(res, user, gameLevel, aiLevel);
		case 57:
			return lvl057_5PlayersSmallPlanets(res, user, gameLevel, aiLevel);
		case 58:
			return lvl058_Lupus(res, user, gameLevel, aiLevel);
		case 59:
			return lvl059_Lyra(res, user, gameLevel, aiLevel);
		case 60:
			return lvl060_Line(res, user, gameLevel, aiLevel);
		case 61:
			return lvl061_3LotOfSmall2(res, user, gameLevel, aiLevel);
		case 62:
			return lvl062_3J(res, user, gameLevel, aiLevel);
		case 63:
			return lvl063_2Partition2(res, user, gameLevel, aiLevel);
		case 64:
			return lvl064_2TrueDuel(res, user, gameLevel, aiLevel);
		case 65:
			return lvl065_2Serpentin(res, user, gameLevel, aiLevel);
		case 66:
			return lvl066_4DontStayInTheMiddle(res, user, gameLevel, aiLevel);
		case 67:
			return lvl067_UrsaMinor(res, user, gameLevel, aiLevel);
		case 68:
			return lvl068_Galaxy(res, user, gameLevel, aiLevel);
		case 69:
			return lvl069_Gears(res, user, gameLevel, aiLevel);
		case 70:
			return lvl070_Wall(res, user, gameLevel, aiLevel);
		case 71:
			return lvl071_3OneBig2(res, user, gameLevel, aiLevel);
		case 72:
			return lvl072_c3NoUnits(res, user, gameLevel, aiLevel);
		case 74:
			return lvl074_Cross4Players(res, user, gameLevel, aiLevel);
		case 75:
			return lvl075_Offside(res, user, gameLevel, aiLevel);
		case 76:
			return lvl076_3AllTogetherBlackhole(res, user, gameLevel, aiLevel);
		case 77:
			return lvl077_UrsaMajor(res, user, gameLevel, aiLevel);
		case 78:
			return lvl078_BlackGears(res, user, gameLevel, aiLevel);
		case 79:
			return lvl079_2OneBigOneBlackhole(res, user, gameLevel, aiLevel);
		case 80:
			return lvl080_RunAsYouCan(res, user, gameLevel, aiLevel);
		case 81:
			return lvl081_3TextureBlackhole(res, user, gameLevel, aiLevel);
		case 82:
			return lvl082_2Partition(res, user, gameLevel, aiLevel);
		case 83:
			return lvl083_4Tournament(res, user, gameLevel, aiLevel);
		case 84:
			return lvl084_4GrowFastOrDie(res, user, gameLevel, aiLevel);
		case 85:
			return lvl085_Cross(res, user, gameLevel, aiLevel);
		case 86:
			return lvl086_9LiveAndLetDie(res, user, gameLevel, aiLevel);
		case 87:
			return lvl087_3Duel(res, user, gameLevel, aiLevel);
		case 88:
			return lvl088_BetweenHoles(res, user, gameLevel, aiLevel);
		case 89:
			return lvl089_4Draughts2(res, user, gameLevel, aiLevel);
		case 90:
			return lvl090_Pegasus(res, user, gameLevel, aiLevel);
		case 91:
			return lvl091_TurnAroundTheBlackhole(res, user, gameLevel, aiLevel);
		case 92:
			return lvl092_Virgo(res, user, gameLevel, aiLevel);
		case 93:
			return lvl093_4ToTheBigs(res, user, gameLevel, aiLevel);
		case 94:
			return lvl094_Cross4PlayersBlackHole(res, user, gameLevel, aiLevel);
		case 95:
			return lvl095_3OneBigPlayerFar(res, user, gameLevel, aiLevel);
		case 96:
			return lvl096_2AvoidBlackhole(res, user, gameLevel, aiLevel);
		case 97:
			return lvl097_4Draughts(res, user, gameLevel, aiLevel);
		case 98:
			return lvl098_4LuckWillNotBeEnought(res, user, gameLevel, aiLevel);
		case 99:
			return lvl099_Matrix(res, user, gameLevel, aiLevel);
		case 100:
			return lvl100_5End(res, user, gameLevel, aiLevel);

		default:
			return lvl020_6NaturalSelection(res, user, gameLevel, aiLevel);
		}
	}

	/**
	 * The really easy game you have when you play the tutorial.
	 * See the MapEncodingComment for details about the array of int "data"
	 */
	public static CampaignMap GetTutorial(GalaxyDomination res, Player user) {
		int[] data = { 2,
				150, 100, 2, 50, 1,
				150, 350, 2, 10, 2,
				50, 130, 2,	5, 0,
				250, 110, 2, 5, 0,
				60, 300, 2, 10, 0,
				260, 280, 2, 5, 0,
				40, 50, 2, 5, 0,
				200, 30, 2, 0, 0,
				130, 240, 2, 25, 0 };
		CampaignMap r = new CampaignMap(res, user, 0, 0, R.string.cTutorial, data);
		r.Players[1].SetAIDelayBeforePlay(60000);
		r.Planets[1].MaxSoldiers = 10;
		return r;
	}








	/**   Hard to read code   **/

	private static CampaignMap lvl001BasicLevel1(GalaxyDomination res, Player user,
			int gameLevel, int aiLevel) {
		int[] data = { 2,
				40, 40, 2,	5,	2,// /AI
				120, 100, 2, 3, 0,
				220, 160, 2, 0, 0,
				50, 200, 2, 2, 0,
				160, 250, 2, 0, 0,
				70, 280, 2, 5, 0,
				250, 350, 2, 0, 0,
				40, 350, 2,	50, 1,// /User
		};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cGreenCivilisationWillDomine, data);
	}

	private static CampaignMap lvl002BasicLevel2(GalaxyDomination res, Player user,
			int gameLevel, int aiLevel) {
		int[] data = { 2,
				40, 40, 2, 5, 0,
				260, 40, 2, 2, 0,
				150, 80, 2,	50,	1,// /User
				40, 120, 2, 5, 0,
				260, 120, 2, 0, 0,
				150, 160, 2, 5, 0,
				40,	200, 2, 0, 0,
				260, 200, 2, 0, 0,
				40, 300, 2, 0, 0,
				260, 300, 2, 0, 0,
				150, 360, 2, 25, 2,// /AI
		};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cMakeLoveNotWar, data);
	}

	private static CampaignMap lvl003SmallPlanet(GalaxyDomination res, Player user,
			int gameLevel, int aiLevel) {
		int[] data = { 2, 150, 60,
				2,
				50,
				1,// /User
				80, 60, 1, 0, 0,
				110, 100, 1, 2, 0,
				150, 120, 1, 0, 0,
				190, 100, 1, 3, 0,
				210, 60, 1, 0, 0,
				40, 210, 2, 20, 0,
				40, 140, 1, 5, 0,
				80, 160, 1, 0, 0,
				100, 200, 1, 5, 0,
				80, 250, 1, 0, 0,
				50, 270, 1, 0, 0,

				260, 190, 1, 0, 0,

				200, 350, 2, 40, 2,// /AI
				150, 340, 1, 5, 0,
				170, 310, 1, 0, 0,
				200, 270, 1, 0, 0,
				230, 310, 1, 2, 0, };
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cSmallPlanetsProduceLess, data);
	}

	private static CampaignMap lvl004BigPlanet(GalaxyDomination res, Player user,
			int gameLevel, int aiLevel) {
		int[] data = { 2,
				40, 40, 2, 50, 1,// /User
				150, 60, 2, 5, 0,
				250, 50, 2, 40,	2,// /AI
				100, 120, 2, 5, 0,
				200, 130, 2, 5, 0,
				260, 170, 1, 1, 0,
				60,	190, 1, 1, 0,
				140, 200, 2, 5, 0,
				150, 350, 3, 10, 0 // /The big
																	// one
		};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cBigPlanetsProduceMore, data);
	}

	private static CampaignMap lvl005BigCentralPlanet(GalaxyDomination res,
			Player user, int gameLevel, int aiLevel) {
		int[] data = { 2,
				40, 40, 2, 50, 1,// /User
				150, 60, 2, 5, 0,
				250, 50, 1, 5, 0,
				100, 120, 2, 5, 0,
				200, 110, 1, 5, 0,
				150, 200, 3, 25, 0, // /The big one

				110, 360, 2, 5, 0,
				250, 340, 2, 50, 2,///AI
				40, 340, 1, 5, 0,
				90,	290, 1, 2, 0,
				260, 170, 1, 1, 0,

		};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cAllInTheCenter, data);
	}

	private static CampaignMap lvl0063DivideToConquer(GalaxyDomination res,
			Player user, int gameLevel, int aiLevel) {
		int[] data = { 3,
				40, 40, 2, 50,	1,// /User
				150, 60, 2, 5, 0,
				60, 100, 2, 5, 0,
				150, 110, 1, 10, 0,

				280, 50, 3, 30, 	0, // /The big one

				50, 360, 2, 40, 2,///AI
				70, 300, 2, 5, 0,
				150, 340, 2, 5, 0,
				210, 260, 2, 15, 0,
				260, 330, 2, 40, 3,///AI
				250, 180, 1, 5, 0,
				220, 160, 1, 5, 0,
				230, 220, 1, 5, 0,

		};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c3DivideToConquer, data);
	}

	private static CampaignMap lvl007Dollard(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				143, 26, 1, 10, 0,
				163, 77, 1, 5, 0,
				96, 80, 1, 5, 0,
				225, 89, 1, 0, 0,
				51, 103, 1, 50, 1,
				150, 143, 1, 25, 0,
				239, 151, 1, 5, 0,
				192, 195, 1, 15, 0,
				135, 211, 1, 25, 0,
				77, 233, 1, 5, 0,
				159, 275, 1, 30, 0,
				66, 291, 1, 10, 0,
				246, 300, 1, 50, 2,
				187, 326, 1, 5, 0,
				124, 331, 1, 10, 0,
				162, 374, 1, 5, 0,
			};


		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cDollard, data);
	}

	private static CampaignMap lvl008Circle(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				150, 50, 2, 50, 1,
				57, 105, 2, 10, 0,
				241, 107, 2, 10, 0,
				34, 209, 2, 10, 0,
				261, 211, 2, 15, 0,
				46, 317, 2, 50, 2,
				238, 318, 2, 15, 0,
				142, 360, 2, 10, 0,
			};

		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cCircle, data);
	}

	private static CampaignMap lvl009_2Rives(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				49, 57, 2, 50, 1,
				176, 72, 1, 10, 0,
				126, 137, 1, 10, 0,
				46, 162, 1, 10, 0,
				252, 250, 1, 10, 0,
				182, 288, 1, 10, 0,
				147, 346, 1, 10, 0,
				254, 356, 2, 50, 2,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2Rives, data);
	}

	private static CampaignMap lvl010_2X(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				44, 100, 2, 20, 0,
				246, 105, 2, 50, 1,
				100, 158, 1, 8, 0,
				194, 170, 1, 8, 0,
				150, 226, 1, 10, 0,
				91, 273, 1, 8, 0,
				199, 286, 1, 12, 0,
				245, 347, 2, 20, 0,
				42, 349, 2, 60, 2,
			};

		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2X, data);
	}

	private static CampaignMap lvl011_2Duel(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				135, 42, 2, 50, 1,
				138, 204, 3, 90, 0,///One big on the center
				139, 357, 2, 50, 2,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2Duel, data);
	}

	private static CampaignMap lvl012_3OneBig(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				242, 51, 2, 50, 1,
				124, 71, 2, 5, 0,
				52, 156, 2, 40, 2,
				261, 184, 1, 0, 0,
				159, 176, 3, 25, 0,
				78, 272, 2, 10, 0,
				241, 282, 2, 10, 0,
				139, 357, 2, 40, 3,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c3OneBig, data);
	}

	private static CampaignMap lvl013_2LotOfSmall(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				51, 49, 2, 10, 0,
				205, 63, 2, 50, 1,
				276, 154, 1, 20, 0,
				117, 161, 1, 5, 0,
				28, 244, 1, 30, 0,
				168, 267, 1, 5, 0,
				78, 338, 2, 50, 2,
				260, 342, 2, 15, 0,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2LotOfSmall, data);
	}

	private static CampaignMap lvl014_3OneBigFarAway(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				250, 39, 1, 0, 0,
				86, 65, 3, 20, 0,
				242, 122, 1, 0, 0,
				208, 199, 2, 50, 1,
				110, 252, 1, 5, 0,
				223, 278, 1, 15, 0,
				124, 347, 1, 5, 0,
				260, 342, 2, 50, 2,
				47, 349, 2, 50, 3,
			};

		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c3OneBigFarAway, data);
	}

	private static CampaignMap lvl015_2BasicLevel(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				164, 49, 1, 5, 0,
				97, 52, 1, 0, 0,
				37, 54, 1, 10, 0,
				250, 50, 2, 50, 1,
				37, 131, 1, 5, 0,
				116, 133, 2, 25, 0,
				231, 136, 2, 10, 0,
				123, 202, 1, 0, 0,
				198, 202, 1, 10, 0,
				43, 204, 1, 15, 0,
				231, 342, 2, 20, 0,
				42, 349, 2, 60, 2,
			};

		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2BasicLevel, data);
	}

	private static CampaignMap lvl016_2Texture(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				145, 31, 1, 5, 0,
				254, 36, 1, 5, 0,
				41, 42, 1, 5, 0,
				207, 124, 2, 20, 0,
				96, 129, 2, 50, 1,
				33, 203, 1, 5, 0,
				149, 204, 1, 15, 0,
				269, 210, 1, 5, 0,
				201, 282, 2, 60, 2,
				92, 284, 2, 20, 0,
				30, 366, 1, 10, 0,
				142, 366, 1, 5, 0,
				260, 369, 1, 5, 0,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2Texture, data);
	}
	private static CampaignMap lvl017_2TowBigPlanets(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				149, 47, 2, 50, 1,
				60, 201, 3, 26, 0,
				226, 201, 3, 26, 0,
				139, 357, 2, 50, 2,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2TowBigPlanets, data);
	}
	private static CampaignMap lvl018_3AlignLotOfSmall(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				125, 38, 1, 10, 0,
				56, 102, 1, 5, 0,
				149, 118, 1, 0, 0,
				245, 135, 1, 5, 0,
				55, 186, 2, 50, 1,
				147, 209, 2, 60, 2,
				239, 241, 2, 60, 3,
				49, 273, 1, 0, 0,
				135, 312, 1, 5, 0,
				239, 332, 1, 0, 0,
				183, 371, 1, 10, 0,
			};

		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c3AlignLotOfSmall, data);
	}
	private static CampaignMap lvl019_2Path(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				163, 55, 1, 10, 0,
				78, 51, 2, 50, 1,
				247, 54, 2, 20, 0,
				256, 135, 1, 0, 0,
				76, 144, 1, 0, 0,
				218, 206, 1, 15, 0,
				100, 220, 1, 15, 0,
				246, 276, 1, 0, 0,
				71, 290, 1, 0, 0,
				140, 353, 1, 10, 0,
				244, 353, 2, 30, 0,
				50, 358, 2, 60, 2,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2Path, data);
	}
	private static CampaignMap lvl020_6NaturalSelection(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {6,
				77, 34, 1, 5, 0,
				250, 39, 1, 5, 0,
				159, 45, 2, 50, 1,
				163, 199, 1, 15, 0,
				59, 207, 2, 75, 2,
				248, 210, 2, 75, 3,
				85, 277, 1, 5, 0,
				223, 278, 1, 5, 0,
				158, 272, 2, 75, 4,
				153, 350, 1, 5, 0,
				47, 349, 2, 75, 5,
				254, 353, 2, 75, 6,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c6NaturalSelection, data);
	}
	private static CampaignMap lvl021_2H(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				56, 48, 2, 50, 1,
				246, 52, 2, 20, 0,
				54, 117, 1, 0, 0,
				257, 124, 1, 5, 0,
				52, 173, 1, 5, 0,
				156, 180, 2, 10, 0,
				254, 189, 1, 5, 0,
				52, 230, 1, 5, 0,
				252, 243, 1, 5, 0,
				53, 285, 1, 5, 0,
				251, 295, 1, 5, 0,
				160, 299, 2, 10, 0,
				47, 349, 2, 75, 2,
				254, 353, 2, 75, 3,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2H, data);
	}
	private static CampaignMap lvl022_2NoUnits(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				256, 52, 1, 1, 1,
				72, 62, 2, 0, 0,
				187, 129, 2, 0, 0,
				59, 177, 2, 0, 0,
				247, 234, 2, 0, 0,
				133, 276, 2, 0, 0,
				234, 349, 2, 0, 0,
				41, 358, 1, 9, 2,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2NoUnits, data);
	}
	private static CampaignMap lvl023_2Orion(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				41, 75, 1, 15, 0,
				258, 102, 2, 25, 1,
				87, 201, 2, 20, 0,
				155, 217, 2, 20, 0,
				221, 235, 2, 20, 0,
				48, 353, 2, 35, 2,
				272, 369, 1, 15, 0,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2Orion, data);
	}

	private static CampaignMap lvl024_2Bomb(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				153, 48, 2, 50, 1,
				209, 94, 1, 5, 0,
				93, 101, 1, 0, 0,
				42, 152, 2, 20, 0,
				244, 153, 2, 20, 0,
				96, 207, 1, 5, 0,
				187, 211, 1, 5, 0,
				138, 251, 1, 10, 0,
				92, 295, 1, 5, 0,
				193, 296, 1, 10, 0,
				245, 347, 2, 20, 0,
				42, 349, 2, 75, 2,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2Bomb, data);
	}
	private static CampaignMap lvl025_2TowBigTwoMiddle(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				149, 47, 2, 50, 1,
				70, 141, 2, 10, 0,
				236, 148, 3, 25, 0,
				152, 208, 1, 0, 0,
				222, 268, 2, 10, 0,
				57, 259, 3, 25, 0,
				139, 357, 2, 75, 2,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2TowBigTwoMiddle, data);
	}

	private static CampaignMap lvl026_Sagittarius (GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				154, 42, 1, 30, 0,
				157, 97, 1, 50, 1,
				249, 104, 1, 0, 0,
				60, 108, 1, 5, 0,
				192, 158, 1, 0, 0,
				36, 171, 1, 5, 0,
				126, 223, 1, 5, 0,
				189, 230, 1, 0, 0,
				57, 236, 1, 75, 2,
				272, 290, 1, 0, 0,
				34, 304, 1, 5, 0,
				75, 345, 1, 75, 3,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cSagittarius, data);
	}

	private static CampaignMap lvl027_3DirectPath(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				56, 48, 2, 50, 1,
				246, 52, 2, 80, 0,
				54, 117, 1, 0, 0,
				257, 124, 1, 5, 0,
				52, 173, 1, 5, 0,
				156, 180, 2, 80, 0,
				254, 189, 1, 5, 0,
				52, 230, 1, 5, 0,
				252, 243, 1, 5, 0,
				53, 285, 1, 5, 0,
				251, 295, 1, 5, 0,
				160, 299, 2, 10, 0,
				47, 349, 2, 75, 2,
				254, 353, 2, 75, 3,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c3DirectPath, data);
	}

	private static CampaignMap lvl028_CanisMajor (GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				59, 65, 1, 50, 1,
				260, 68, 1, 10, 0,
				68, 154, 1, 15, 0,
				244, 169, 2, 25, 0,
				139, 213, 1, 25, 0,
				86, 221, 1, 75, 2,
				37, 249, 1, 25, 0,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cCanisMajor, data);
	}

	private static CampaignMap lvl029_3AllTogether(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				138, 42, 1, 2, 0,
				163, 199, 1, 8, 0,
				85, 277, 1, 15, 0,
				223, 278, 1, 5, 0,
				159, 274, 2, 50, 1,
				153, 350, 1, 15, 0,
				47, 349, 2, 75, 2,
				254, 353, 2, 75, 3,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c3AllTogether, data);
	}
	private static CampaignMap lvl030_2Smiley(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				119, 83, 1, 0, 0,
				189, 84, 1, 5, 0,
				57, 126, 1, 0, 0,
				242, 130, 1, 10, 0,
				154, 210, 2, 10, 0,
				69, 290, 2, 50, 1,
				215, 290, 2, 80, 2,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2Smiley, data);
	}
	private static CampaignMap lvl031_Sagitta(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				157, 115, 1, 50, 1,
				203, 145, 1, 60, 2,
				137, 202, 2, 15, 0,
				65, 320, 2, 25, 0,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cSagitta, data);
	}
	private static CampaignMap lvl032_3TowBig(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				61, 47, 2, 50, 1,
				242, 51, 2, 80, 2,
				150, 54, 2, 15, 0,
				70, 141, 2, 10, 0,
				236, 148, 3, 25, 0,
				152, 208, 1, 0, 0,
				222, 268, 2, 10, 0,
				57, 259, 3, 25, 0,
				139, 357, 2, 80, 3,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c3TowBig, data);
	}
	private static CampaignMap lvl033_3K(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				249, 63, 2, 10, 0,
				55, 92, 3, 25, 0,
				166, 126, 2, 80, 2,
				55, 209, 2, 50, 1,
				170, 272, 2, 80, 3,
				246, 339, 2, 10, 0,
				51, 327, 3, 25, 0,
			};

		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c3K, data);
	}
	private static CampaignMap lvl034_4LotOfSmall(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {4,
				163, 44, 1, 10, 0,
				250, 45, 2, 50, 1,
				56, 102, 1, 5, 0,
				133, 110, 1, 0, 0,
				223, 146, 1, 5, 0,
				132, 172, 1, 0, 0,
				58, 169, 2, 80, 2,
				248, 216, 1, 0, 0,
				59, 239, 1, 0, 0,
				185, 263, 2, 80, 3,
				135, 312, 1, 5, 0,
				239, 332, 1, 0, 0,
				56, 356, 2, 80, 4,
				183, 371, 1, 10, 0,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c4LotOfSmall, data);
	}

	private static CampaignMap lvl035_4BigOnBorder(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {4,
				269, 46, 2, 50, 1,
				50, 48, 2, 80, 2,
				167, 127, 1, 0, 0,
				136, 212, 1, 0, 0,
				270, 221, 2, 20, 0,
				41, 233, 2, 20, 0,
				192, 292, 1, 5, 0,
				110, 305, 1, 10, 0,
				38, 363, 2, 80, 3,
				267, 367, 2, 80, 4,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c4BigOnBorder, data);
	}
	private static CampaignMap lvl036_Grus (GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				41, 71, 1, 50, 1,
				210, 143, 2, 15, 0,
				74, 147, 2, 20, 0,
				132, 215, 1, 5, 0,
				188, 259, 1, 20, 0,
				218, 318, 1, 5, 0,
				262, 369, 2, 80, 2,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cGrus, data);
	}
	private static CampaignMap lvl037_2Oo(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				191, 42, 1, 5, 0,
				112, 61, 1, 15, 0,
				248, 92, 1, 5, 0,
				57, 127, 1, 15, 0,
				232, 165, 1, 10, 0,
				96, 206, 1, 15, 0,
				171, 212, 1, 5, 0,
				237, 347, 2, 50, 1,
				48, 349, 2, 90, 2,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2Oo, data);
	}

	private static CampaignMap lvl038_3OnlyBigPlanets(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				53, 59, 3, 50, 1,
				238, 64, 3, 80, 2,
				147, 157, 3, 20, 0,
				52, 228, 3, 20, 0,
				242, 229, 3, 20, 0,
				61, 328, 3, 80, 3,
				191, 328, 3, 20, 0,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c3OnlyBigPlanets, data);
	}
	private static CampaignMap lvl039_2TooEasy(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				140, 104, 3, 50, 1,
				133, 331, 1, 5, 2, ///Really absurd
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2TooEasy, data);
	}

	private static CampaignMap lvl040_CircleDot(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				150, 50, 2, 50, 1,
				57, 105, 2, 10, 0,
				241, 107, 2, 10, 0,
				145, 208, 1, 0, 0,
				34, 209, 2, 5, 0,
				261, 211, 2, 0, 0,
				46, 317, 2, 90, 2,
				238, 318, 2, 90, 3,
				142, 360, 2, 20, 0,
			};

		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cCircleDot, data);
	}
	private static CampaignMap lvl041_3BigPlanets(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				242, 61, 3, 20, 0,
				56, 114, 1, 50, 1,
				153, 192, 3, 25, 0,
				253, 273, 1, 90, 2,
				60, 328, 3, 20, 0,
			};

		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c3BigPlanets, data);
	}

	private static CampaignMap lvl042_3Ring(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				266, 50, 2, 10, 0,
				154, 52, 2, 50, 1,
				43, 75, 2, 15, 0,
				261, 184, 1, 0, 0,
				55, 209, 2, 90, 2,
				247, 318, 2, 10, 0,
				43, 330, 2, 10, 0,
				139, 357, 2, 90, 3,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c3Ring, data);
	}

	private static CampaignMap lvl043_2WhereAreMyUnits(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				49, 62, 1, 15, 0,
				226, 68, 1, 5, 1,
				235, 199, 1, 25, 0,
				62, 205, 1, 25, 0,
				139, 260, 1, 50, 0,
				233, 356, 1, 11, 0,
				41, 358, 1, 10, 2,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2WhereAreMyUnits, data);
	}

	private static CampaignMap lvl044_2Herculus(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				275, 27, 1, 50, 1,
				31, 48, 1, 0, 0,
				235, 64, 1, 20, 0,
				104, 130, 1, 0, 0,
				191, 142, 1, 30, 0,
				89, 226, 1, 0, 0,
				182, 246, 1, 90, 2,
				23, 287, 1, 0, 0,
				223, 301, 1, 5, 0,
				137, 346, 2, 30, 0,
				259, 369, 1, 90, 3,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2Herculus, data);
	}

	private static CampaignMap lvl045_FirstBlackHole(GalaxyDomination res,
			Player user, int gameLevel, int aiLevel) {
		int[] data = { 2,
				40, 50, 2, 5, 0,
				200, 30, 2, 10, 0,
				140, 100,2,	50,	1,// /User
				240, 110, 2, 10, 0,
				50, 130, 2, 25, 0,
				250, 170, 2, 25, 0,
				150, 200, 3, 999, 0,// /Black hole
				60, 220, 2, 10, 0,
				260, 280, 2, 15, 0,
				140, 280, 2, 20, 0,
				60, 300, 2, 10, 0,
				150, 350, 2, 50, 2,// /IA
				25, 370, 2, 10, 0 };
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cCapitainBewareABlackhole, data);
	}

	private static CampaignMap lvl046_Horologium (GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				41, 71, 1, 50, 1,
				210, 143, 2, 15, 0,
				74, 147, 2, 10, 0,
				132, 215, 1, 5, 0,
				188, 259, 1, 10, 0,
				218, 318, 1, 5, 0,
				262, 369, 2, 90, 2,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cHorologium, data);
	}

	private static CampaignMap lvl047_3LotOfSmall(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				125, 38, 1, 10, 0,
				250, 45, 2, 50, 1,
				56, 102, 1, 5, 0,
				149, 118, 1, 0, 0,
				245, 135, 1, 5, 0,
				64, 198, 1, 0, 0,
				147, 209, 2, 90, 2,
				236, 240, 1, 0, 0,
				49, 273, 1, 0, 0,
				135, 312, 1, 5, 0,
				239, 332, 1, 0, 0,
				48, 347, 2, 90, 3,
				183, 371, 1, 10, 0,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c3LotOfSmall, data);
	}

	private static CampaignMap lvl048_3Align2Bigs(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				249, 63, 2, 10, 0,
				59, 75, 3, 25, 0,
				204, 129, 1, 0, 0,
				51, 201, 2, 50, 1,
				147, 209, 2, 90, 2,
				232, 210, 2, 90, 3,
				143, 293, 1, 0, 0,
				55, 340, 2, 10, 0,
				239, 347, 3, 25, 0,
			};

		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c3Align2Bigs, data);
	}

	private static CampaignMap lvl049_Lacerta (GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				182, 33, 1, 10, 0,
				116, 128, 2, 25, 1,
				182, 195, 1, 5, 0,
				102, 276, 3, 15, 0,
				207, 373, 2, 50, 2,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cLacerta, data);
	}
	private static CampaignMap lvl050_Orbit(GalaxyDomination res, Player user,
			int gameLevel, int aiLevel) {
		int[] data = { 4,
				40, 40, 2, 50,	1,// /User
				260, 40, 2, 80,	2,// /AI
				40, 360, 2, 80,	3,// /AI
				260, 360, 2, 80,4,// /AI

				160, 220, 3, 40, 0,// /The big

				160, 50, 2, 10, 0,
				250, 220, 2, 18, 0,
				160, 350, 2, 10, 0,
				50,	220, 2, 10, 0,
				120, 100, 1, 16, 0,
				200, 100, 1, 8, 0,
				240, 150, 1, 18, 0,
				240, 280, 1, 8, 0,
				200, 300, 1, 18, 0,
				120, 300, 1, 15, 0,
				60, 150, 1, 18, 0,
				60, 280, 1, 8, 0,

		};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cTheOrbit, data);
	}
	private static CampaignMap lvl051_Lepus(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				74, 91, 1, 5, 0,
				262, 112, 2, 25, 1,
				29, 137, 1, 15, 0,
				169, 181, 2, 10, 0,
				165, 280, 2, 50, 2,
				263, 317, 1, 15, 0,
				77, 349, 1, 5, 0,
				25, 369, 1, 10, 0,
			};

		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cLepus, data);
	}
	private static CampaignMap lvl052_Parable(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				56, 48, 2, 50, 1,
				246, 52, 2, 20, 0,
				54, 117, 1, 10, 0,
				257, 124, 1, 5, 0,
				52, 173, 1, 5, 0,
				156, 180, 2, 999, 0,
				254, 189, 1, 15, 0,
				52, 230, 1, 5, 0,
				252, 243, 1, 15, 0,
				53, 285, 1, 15, 0,
				251, 295, 1, 15, 0,
				160, 299, 2, 999, 0,
				47, 349, 2, 100, 2,
				254, 353, 2,100, 3,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cParable, data);
	}
	private static CampaignMap lvl053_2NoUnitsRemakeWithUnits(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				256, 52, 1, 50, 1,
				72, 62, 2, 50, 0,
				187, 129, 2, 50, 0,
				59, 177, 2, 50, 0,
				247, 234, 2, 50, 0,
				133, 276, 2, 50, 0,
				234, 349, 2, 50, 0,
				41, 358, 1, 90, 2,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2NoUnitsRemakeWithUnits, data);
	}
	private static CampaignMap lvl054_2Andromeda(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				177, 139, 1, 30, 0,
				265, 155, 2, 50, 1,
				86, 178, 2, 25, 0,
				202, 212, 1, 25, 0,
				139, 268, 1, 25, 0,
				33, 273, 2, 90, 2,
				71, 358, 1, 10, 0,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2Andromeda, data);
	}
	private static CampaignMap lvl055_2Path4Players(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {4,
				163, 55, 1, 10, 0,
				78, 51, 2, 50, 1,
				247, 54, 2, 90, 2,
				256, 135, 1, 0, 0,
				76, 144, 1, 0, 0,
				218, 206, 1, 10, 0,
				100, 220, 1, 10, 0,
				246, 276, 1, 0, 0,
				71, 290, 1, 0, 0,
				140, 353, 1, 10, 0,
				244, 353, 2, 90, 3,
				50, 358, 2, 90, 4,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2Path4Players, data);
	}
	private static CampaignMap lvl056_Leo(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				228, 50, 2, 30, 1,
				137, 126, 1, 5, 0,
				225, 130, 1, 15, 0,
				123, 260, 2, 10, 0,
				51, 284, 1, 5, 0,
				178, 316, 1, 30, 0,
				239, 310, 2, 50, 2,
				21, 358, 1, 50, 3,
				74, 375, 1, 10, 0,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cLeo, data);
	}
	private static CampaignMap lvl057_5PlayersSmallPlanets(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {5,
				250, 50, 2, 50, 1,
				56, 70, 2, 90, 2,
				196, 102, 1, 10, 0,
				125, 105, 1, 10, 0,
				224, 159, 1, 0, 0,
				73, 160, 1, 15, 0,
				138, 196, 1, 25, 0,
				195, 235, 1, 15, 0,
				67, 250, 1, 15, 0,
				130, 267, 1, 0, 0,
				235, 310, 2, 90, 3,
				35, 321, 2, 90, 4,
				148, 351, 2, 90, 5,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c5PlayersSmallPlanets, data);
	}
	private static CampaignMap lvl058_Lupus(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				52, 68, 1, 0, 0,
				76, 135, 1, 5, 0,
				262, 154, 2, 25, 1,
				84, 206, 1, 5, 0,
				228, 220, 1, 10, 0,
				104, 284, 2, 50, 2,
				199, 288, 2, 50, 0,
				74, 375, 1, 10, 0,
				218, 375, 1, 30, 0,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cLupus, data);
	}
	private static CampaignMap lvl059_Lyra(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				66, 61, 2, 40, 1,
				146, 88, 2, 15, 0,
				92, 206, 1, 15, 0,
				166, 225, 1, 15, 0,
				214, 328, 3, 50, 2,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cLyra, data);
	}
	private static CampaignMap lvl060_Line(GalaxyDomination res, Player user,
			int gameLevel, int aiLevel) {
		int[] data = { 2,
				40, 40, 2, 50, 1,// /User

				40, 100, 2, 10, 0,
				40, 160, 2, 20, 0,
				40, 220, 2, 30, 0,
				40,	280, 2, 20, 0,
				40, 340, 2, 100, 2,// /IA
				280, 200, 1, 100, 0, };
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cTheLine, data);
	}
	private static CampaignMap lvl061_3LotOfSmall2(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				114, 47, 1, 20, 1,
				227, 63, 1, 15, 0,
				43, 96, 2, 10, 0,
				148, 132, 1, 0, 0,
				271, 167, 1, 10, 0,
				88, 199, 1, 5, 0,
				241, 218, 1, 30, 0,
				163, 235, 1, 5, 0,
				34, 245, 1, 30, 0,
				108, 282, 1, 0, 0,
				193, 297, 1, 10, 0,
				257, 306, 2, 50, 2,
				44, 317, 1, 10, 0,
				179, 367, 1, 30, 0,
				91, 382, 1, 50, 3,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c3LotOfSmall2, data);
	}
	private static CampaignMap lvl062_3J(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				125, 50, 1, 5, 0,
				66, 71, 1, 0, 0,
				191, 87, 1, 15, 0,
				40, 121, 1, 20, 1,
				195, 161, 1, 0, 0,
				193, 230, 1, 5, 0,
				193, 297, 1, 10, 0,
				258, 351, 1, 50, 2,
				120, 352, 1, 50, 3,
				187, 354, 1, 15, 0,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c3J, data);
	}
	private static CampaignMap lvl063_2Partition2(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				250, 43, 1, 10, 0,
				42, 47, 1, 50, 1,
				250, 96, 1, 999, 0,
				42, 97, 1, 999, 0,
				43, 150, 1, 10, 0,
				251, 153, 1, 10, 0,
				45, 201, 1, 999, 0,
				252, 206, 1, 999, 0,
				43, 254, 1, 10, 0,
				252, 257, 1, 10, 0,
				46, 306, 1, 999, 0,
				255, 311, 1, 999, 0,
				47, 356, 1, 10, 0,
				256, 360, 1, 100, 2,
			};

		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2Partition2, data);
	}
	private static CampaignMap lvl064_2TrueDuel(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				150, 120, 2, 100, 1,
				150, 225, 2, 100, 2
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2TrueDuel, data);
	}
	private static CampaignMap lvl065_2Serpentin(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				164, 49, 1, 15, 0,
				97, 52, 1, 0, 0,
				37, 54, 1, 10, 0,
				250, 50, 2, 50, 1,
				37, 131, 1, 5, 0,
				220, 132, 2, 999, 0,
				141, 136, 2, 999, 0,
				123, 202, 1, 0, 0,
				198, 202, 1, 20, 0,
				43, 204, 1, 25, 0,
				220, 261, 1, 0, 0,
				151, 272, 2, 999, 0,
				69, 279, 2, 999, 0,
				246, 318, 1, 20, 0,
				117, 351, 1, 5, 0,
				42, 349, 2, 100, 2,
				191, 359, 1, 0, 0,
			};

		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2Serpentin, data);
	}
	private static CampaignMap lvl066_4DontStayInTheMiddle(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {4,
				269, 46, 2, 50, 1,
				50, 48, 2, 100, 2,
				167, 127, 1, 0, 0,
				136, 212, 1, 0, 0,
				270, 221, 2, 20, 0,
				41, 233, 2, 20, 0,
				192, 292, 1, 5, 0,
				110, 305, 1, 10, 0,
				38, 363, 2, 100, 3,
				267, 367, 2, 100, 4,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c4DontStayInTheMiddle, data);
	}
	private static CampaignMap lvl067_UrsaMinor(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				62, 56, 2, 30, 1,
				136, 76, 2, 10, 0,
				41, 156, 1, 10, 0,
				111, 156, 1, 50, 2,
				114, 215, 1, 10, 0,
				139, 268, 1, 15, 0,
				214, 351, 3, 10, 0,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cUrsaMinor, data);
	}
	private static CampaignMap lvl068_Galaxy(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				71, 32, 1, 10, 0,
				143, 50, 1, 10, 0,
				193, 89, 1, 20, 1,
				84, 144, 1, 0, 0,
				225, 146, 1, 10, 0,
				145, 157, 1, 25, 0,
				41, 181, 1, 10, 0,
				245, 212, 1, 50, 2,
				135, 214, 2, 999, 0,
				38, 252, 1, 15, 0,
				239, 269, 1, 0, 0,
				134, 275, 1, 0, 0,
				191, 298, 1, 10, 0,
				51, 323, 1, 0, 0,
				280, 343, 1, 15, 0,
				100, 366, 1, 30, 3,
				167, 378, 1, 15, 0,
				241, 384, 1, 0, 0,
			};

		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cGalaxy, data);
	}
	private static CampaignMap lvl069_Gears(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				176, 45, 1, 30, 0,
				259, 60, 1, 10, 0,
				113, 105, 1, 20, 1,
				276, 148, 1, 15, 0,
				201, 134, 3, 50, 0,
				122, 194, 1, 0, 0,
				69, 197, 1, 0, 0,
				223, 219, 1, 5, 0,
				169, 229, 1, 5, 0,
				18, 237, 1, 30, 0,
				193, 297, 1, 10, 0,
				97, 296, 3, 50, 0,
				14, 324, 1, 10, 0,
				179, 367, 1, 30, 0,
				91, 382, 1, 80, 2,
			};

		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cGears, data);
	}
	private static CampaignMap lvl070_Wall(GalaxyDomination res, Player user,
			int gameLevel, int aiLevel) {
		int[] data = { 2,
				40, 40, 2, 5, 0,
				40, 110, 2, 10, 0,
				150, 70, 2,	50,	1,// /User
				240, 40, 2, 5, 0,
				260, 90, 2, 10, 0,
				70, 80, 1, 15, 0,
				200, 110, 1, 15, 0,
				40, 200, 2, 100, 0,
				100, 200, 2, 110, 0,
				160, 200, 2, 120, 0,
				220, 200, 2, 110, 0,
				280, 200, 2, 100, 0,
				190, 330, 1, 15, 0,
				80, 300, 1, 15, 0,
				40, 390, 2, 5, 0,
				40,	300, 2, 10, 0,
				150, 350, 2, 120, 2,// /AI
				260, 390, 2, 5, 0,
				250, 310, 2, 10, 0,

		};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cThewall, data);
	}
	private static CampaignMap lvl071_3OneBig2(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				154, 52, 2, 50, 1,
				251, 81, 2, 10, 0,
				60, 96, 2, 15, 0,
				261, 184, 1, 0, 0,
				38, 210, 2, 100, 2,
				166, 194, 3, 25, 0,
				241, 282, 2, 10, 0,
				43, 330, 2, 10, 0,
				139, 357, 2, 100, 3,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c3OneBig2, data);
	}
	private static CampaignMap lvl072_c3NoUnits(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				49, 62, 1, 10, 0,
				226, 68, 1, 15, 1,
				146, 137, 1, 10, 0,
				235, 199, 1, 30, 0,
				62, 205, 1, 25, 0,
				139, 260, 1, 50, 0,
				222, 270, 1, 50, 2,
				139, 338, 1, 25, 0,
				249, 352, 1, 20, 0,
				41, 358, 1, 50, 3,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c3NoUnits, data);
	}
	private static CampaignMap lvl074_Cross4Players(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {4,
				93, 39, 1, 10, 0,
				210, 42, 1, 10, 0,
				153, 43, 2, 50, 1,
				48, 133, 1, 10, 0,
				258, 133, 1, 0, 0,
				45, 195, 2, 50, 2,
				258, 198, 2, 100, 3,
				154, 203, 3, 25, 0,
				50, 262, 1, 10, 0,
				256, 271, 1, 5, 0,
				210, 352, 1, 10, 0,
				91, 356, 1, 0, 0,
				148, 351, 2, 100, 4,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cCross4Players, data);
	}
	private static CampaignMap lvl075_Offside(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {4,
				78, 51, 2, 50, 1,
				189, 214, 1, 10, 0,
				257, 240, 1, 5, 0,
				46, 271, 1, 10, 0,
				125, 284, 2, 110, 2,
				136, 354, 1, 0, 0,
				50, 358, 2, 50, 3,
				236, 338, 3, 110, 4,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cOffside, data);
	}
	private static CampaignMap lvl076_3AllTogetherBlackhole(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				53, 44, 2, 5, 0,
				259, 45, 2, 5, 0,
				152, 134, 3, 999, 0,
				53, 194, 1, 5, 0,
				249, 203, 1, 5, 0,
				85, 277, 1, 5, 0,
				223, 278, 1, 5, 0,
				159, 274, 2, 50, 1,
				153, 350, 1, 5, 0,
				47, 349, 2, 110, 2,
				254, 353, 2, 110, 3,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c3AllTogetherBlackhole, data);
	}
	private static CampaignMap lvl077_UrsaMajor(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				92, 50, 1, 20, 1,
				66, 125, 1, 0, 0,
				77, 190, 1, 0, 0,
				115, 240, 1, 15, 0,
				210, 244, 1, 15, 0,
				229, 334, 1, 50, 2,
				126, 367, 1, 20, 0,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cUrsaMajor, data);
	}
	private static CampaignMap lvl078_BlackGears(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				176, 45, 1, 30, 0,
				259, 60, 1, 10, 0,
				113, 105, 1, 20, 1,
				196, 132, 2, 999, 0,
				276, 148, 1, 15, 0,
				122, 194, 1, 0, 0,
				69, 197, 1, 0, 0,
				223, 219, 1, 5, 0,
				169, 229, 1, 5, 0,
				18, 237, 1, 30, 0,
				193, 297, 1, 10, 0,
				97, 293, 2, 999, 0,
				14, 324, 1, 10, 0,
				179, 367, 1, 30, 0,
				91, 382, 1, 50, 2,
			};


		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cBlackGears, data);
	}
	private static CampaignMap lvl079_2OneBigOneBlackhole(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				272, 40, 1, 15, 0,
				46, 41, 1, 15, 0,
				154, 62, 3, 60, 0,
				45, 212, 1, 5, 0,
				261, 212, 1, 5, 0,
				152, 199, 3, 999, 0,
				250, 341, 2, 50, 1,
				147, 351, 1, 5, 0,
				48, 349, 2, 110, 2,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2OneBigOneBlackhole, data);
	}
	private static CampaignMap lvl080_RunAsYouCan(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				229, 37, 1, 0, 0,
				125, 49, 1, 5, 0,
				262, 102, 1, 5, 0,
				196, 110, 1, 0, 0,
				44, 113, 1, 0, 0,
				124, 129, 1, 5, 0,
				223, 198, 1, 10, 0,
				144, 200, 1, 0, 0,
				79, 203, 1, 10, 0,
				137, 288, 2, 50, 1,
				138, 364, 2, 150, 2,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cRunAsYouCan, data);
	}
	private static CampaignMap lvl081_3TextureBlackhole(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				145, 31, 1, 5, 0,
				254, 36, 1, 30, 0,
				41, 42, 1, 50, 1,
				207, 124, 2, 999, 0,
				96, 129, 2, 999, 0,
				33, 203, 1, 5, 0,
				149, 204, 1, 110, 2,
				269, 210, 1, 5, 0,
				201, 282, 2, 999, 0,
				92, 284, 2, 999, 0,
				30, 366, 1, 30, 0,
				142, 366, 1, 5, 0,
				260, 369, 1, 110, 3,
			};

		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c3TextureBlackhole, data);
	}
	private static CampaignMap lvl082_2Partition(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				103, 55, 2, 999, 0,
				246, 55, 2, 110, 2,
				40, 57, 2, 15, 0,
				188, 153, 2, 999, 0,
				246, 153, 2, 10, 0,
				41, 154, 2, 10, 0,
				249, 257, 2, 15, 0,
				42, 259, 2, 50, 1,
				104, 259, 2, 999, 0,
				185, 259, 2, 999, 0,
				44, 353, 2, 15, 0,
				252, 355, 2, 15, 0,
			};

		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2Partition, data);
	}
	private static CampaignMap lvl083_4Tournament(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {4,
				142, 81, 3, 20, 0,
				64, 191, 2, 10, 0,
				229, 191, 2, 10, 0,
				148, 203, 2, 999, 0,
				105, 276, 1, 5, 1,
				278, 276, 1, 5, 2,
				201, 277, 1, 5, 3,
				28, 278, 1, 5, 4,
				66, 281, 1, 999, 0,
				153, 282, 1, 999, 0,
				240, 283, 1, 999, 0,
			};

		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c4Tournament, data);
	}
	private static CampaignMap lvl084_4GrowFastOrDie(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {4,
				57, 74, 1, 0, 0,
				249, 75, 1, 0, 0,
				155, 66, 3, 25, 0,
				156, 163, 1, 5, 1,
				262, 161, 2, 10, 0,
				50, 166, 2, 10, 0,
				196, 220, 1, 10, 2,
				118, 223, 1, 10, 3,
				272, 231, 1, 0, 0,
				44, 275, 1, 0, 0,
				160, 282, 1, 10, 4,
				272, 286, 1, 10, 0,
				63, 344, 1, 10, 0,
				244, 354, 1, 10, 0,
				154, 364, 2, 10, 0,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c4GrowFastOrDie, data);
	}
	private static CampaignMap lvl085_Cross(GalaxyDomination res, Player user,
			int gameLevel, int aiLevel) {
		int[] data = { 2,
				40, 40, 3, 50, 0,
				260, 40, 3, 50, 0,
				40, 360, 3, 50,	0,
				260, 360, 3, 50, 0,

				160, 220, 2, 999, 0,// /The Black hole

				100, 100, 2, 15, 0,
				100, 300, 2, 15, 0,
				220, 100, 2, 15, 0,
				220, 300, 2, 15, 0,

				140, 140, 1, 10, 1,// /User
				140, 260, 1, 15, 0,
				180, 140, 1, 15, 0,
				180, 260, 1, 45, 2 // /AI

		};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cCross, data);
	}
	private static CampaignMap lvl086_9LiveAndLetDie(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {9,
				255, 35, 1, 5, 1,
				136, 41, 2, 10, 0,
				57, 74, 1, 5, 2,
				193, 88, 1, 0, 0,
				107, 114, 1, 0, 0,
				246, 134, 1, 0, 0,
				156, 163, 1, 5, 3,
				59, 171, 1, 0, 0,
				252, 206, 1, 5, 4,
				103, 223, 1, 0, 0,
				190, 227, 1, 5, 0,
				52, 274, 1, 5, 5,
				160, 282, 1, 5, 6,
				272, 286, 1, 5, 7,
				109, 306, 1, 0, 0,
				200, 338, 1, 0, 0,
				51, 357, 1, 5, 8,
				269, 361, 1, 5, 9,
				131, 364, 2, 15, 0,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c9LiveAndLetDie, data);
	}
	private static CampaignMap lvl087_3Duel(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				184, 137, 2, 100, 1,
				51, 225, 2, 100, 2,
				193, 304, 2, 100, 3,
			};

		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c3Duel, data);
	}
	private static CampaignMap lvl088_BetweenHoles(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				214, 31, 1, 0, 0,
				117, 34, 1, 5, 0,
				66, 104, 2, 50, 1,
				260, 115, 2, 20, 0,
				166, 136, 1, 10, 0,
				92, 190, 3, 999, 0,
				235, 216, 3, 999, 0,
				154, 284, 1, 10, 0,
				256, 301, 2, 110, 2,
				65, 302, 2, 0, 0,
				175, 364, 1, 10, 0,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cBetweenHoles, data);
	}
	private static CampaignMap lvl089_4Draughts2(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {4,
				54, 40, 2, 40, 1,
				156, 43, 2, 999, 0,
				251, 44, 2, 100, 2,
				157, 115, 2, 10, 0,
				60, 116, 2, 999, 0,
				248, 120, 2, 999, 0,
				158, 200, 2, 999, 0,
				252, 200, 2, 100, 0,
				58, 202, 2, 5, 0,
				154, 282, 2, 0, 0,
				250, 282, 2, 999, 0,
				60, 285, 2, 999, 0,
				250, 358, 2, 100, 3,
				152, 361, 2, 999, 0,
				60, 364, 2, 100, 4,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c4Draughts2, data);
	}
	private static CampaignMap lvl090_Pegasus(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				207, 74, 1, 30, 0,
				38, 89, 1, 25, 0,
				174, 134, 1, 50, 1,
				73, 144, 1, 0, 0,
				129, 186, 1, 999, 0,
				174, 234, 1, 15, 0,
				80, 236, 1, 120, 2,
				207, 285, 1, 30, 0,
				283, 307, 1, 0, 0,
				238, 328, 1, 120, 3,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cPegasus, data);
	}
	private static CampaignMap lvl091_TurnAroundTheBlackhole(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				137, 41, 1, 5, 0,
				216, 44, 1, 0, 0,
				46, 47, 2, 50, 1,
				166, 121, 1, 10, 0,
				239, 130, 1, 5, 0,
				211, 209, 1, 20, 0,
				63, 210, 3, 999, 0,
				166, 264, 1, 10, 0,
				255, 288, 1, 0, 0,
				169, 354, 1, 10, 0,
				256, 359, 1, 10, 0,
				50, 358, 2, 150, 2,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cTurnAoundTheBlackhole, data);
	}
	private static CampaignMap lvl092_Virgo(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				151, 38, 1, 20, 0,
				131, 107, 1, 50, 1,
				48, 132, 1, 5, 0,
				134, 199, 1, 0, 0,
				71, 250, 1, 120, 2,
				166, 257, 1, 15, 0,
				31, 297, 1, 10, 0,
				196, 327, 1, 120, 3,
				155, 383, 1, 0, 0,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cVirgo, data);
	}
	private static CampaignMap lvl093_4ToTheBigs(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {4,
				251, 52, 3, 20, 0,
				153, 54, 3, 999, 0,
				55, 56, 3, 20, 0,
				207, 217, 1, 999, 0,
				99, 219, 1, 999, 0,
				263, 234, 2, 10, 0,
				45, 235, 2, 10, 0,
				152, 237, 2, 10, 0,
				206, 253, 1, 999, 0,
				99, 256, 1, 999, 0,
				272, 366, 1, 20, 1,
				115, 367, 1, 50, 2,
				153, 367, 1, 999, 0,
				192, 367, 1, 50, 3,
				232, 367, 1, 999, 0,
				76, 368, 1, 999, 0,
				37, 369, 1, 50, 4,
			};

		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c4ToTheBigs, data);
	}
	private static CampaignMap lvl094_Cross4PlayersBlackHole(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {4,
				93, 39, 1, 10, 0,
				210, 42, 1, 10, 0,
				153, 43, 2, 50, 1,
				48, 133, 1, 10, 0,
				258, 133, 1, 0, 0,
				45, 195, 2, 120, 2,
				258, 198, 2, 120, 3,
				154, 203, 3, 999, 0,
				50, 262, 1, 10, 0,
				256, 271, 1, 5, 0,
				210, 352, 1, 10, 0,
				91, 356, 1, 0, 0,
				148, 351, 2, 120, 4,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cCross4PlayersBlackHole, data);
	}
	private static CampaignMap lvl095_3OneBigPlayerFar(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {3,
				53, 44, 1, 5, 0,
				259, 45, 1, 5, 0,
				154, 46, 2, 50, 1,
				53, 194, 1, 5, 0,
				249, 203, 1, 5, 0,
				155, 195, 3, 999, 0,
				235, 278, 1, 5, 0,
				90, 287, 1, 5, 0,
				156, 331, 1, 5, 0,
				47, 349, 2, 150, 2,
				254, 353, 2, 150, 3,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c3AllTogetherBlackhole, data);
	}
	private static CampaignMap lvl096_2AvoidBlackhole(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {2,
				158, 31, 1, 5, 0,
				264, 31, 1, 10, 0,
				54, 40, 2, 10, 1,
				45, 111, 2, 999, 0,
				98, 111, 2, 999, 0,
				170, 109, 3, 999, 0,
				262, 204, 1, 10, 0,
				164, 205, 1, 5, 0,
				63, 204, 2, 0, 0,
				44, 294, 2, 999, 0,
				99, 295, 2, 999, 0,
				173, 295, 3, 999, 0,
				260, 368, 1, 10, 0,
				60, 364, 2, 150, 2,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c2AvoidBlackhole, data);
	}
	private static CampaignMap lvl097_4Draughts(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {4,
				54, 40, 2, 20, 1,
				156, 43, 2, 999, 0,
				251, 44, 2, 50, 2,
				157, 115, 2, 10, 0,
				60, 116, 2, 999, 0,
				248, 120, 2, 999, 0,
				158, 200, 2, 999, 0,
				252, 200, 2, 10, 0,
				58, 202, 2, 5, 0,
				154, 282, 2, 0, 0,
				250, 282, 2, 999, 0,
				60, 285, 2, 999, 0,
				250, 358, 2, 50, 3,
				152, 361, 2, 999, 0,
				60, 364, 2, 50, 4,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c4Draughts, data);
	}
	private static CampaignMap lvl098_4LuckWillNotBeEnought(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {4,
				250, 43, 1, 10, 0,
				42, 47, 1, 10, 1,
				250, 96, 1, 999, 0,
				42, 97, 1, 999, 0,
				43, 150, 1, 10, 0,
				251, 153, 1, 20, 2,
				45, 201, 1, 999, 0,
				252, 206, 1, 999, 0,
				43, 254, 1, 20, 3,
				252, 257, 1, 10, 0,
				46, 306, 1, 999, 0,
				255, 311, 1, 999, 0,
				47, 356, 1, 10, 0,
				256, 360, 1, 20, 4,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c4LuckWillNotBeEnought, data);
	}
	private static CampaignMap lvl099_Matrix(GalaxyDomination res, Player user,
			int gameLevel, int aiLevel) {
		int[] data = {
				6,
				40,
				40,
				1,
				50,
				1,// /User
				80, 40, 1, 10, 0,
				120, 40, 1, 10, 0,
				160, 40, 1, 10, 0,
				200, 40, 1, 10, 0,
				240, 40, 1, 10, 0,
				280, 40, 1, 200, 2,

				40, 80, 1, 10, 0,
				80, 80, 1, 10, 0,
				120, 80, 1, 10, 0,
				160, 80, 1, 10, 0,
				200, 80, 1, 10, 0,
				240, 80, 1, 10, 0,
				280, 80, 1, 10,	0,

				40, 120, 1, 10, 0,
				80, 120, 1, 10, 0,
				120, 120, 1, 10, 0,
				160, 120, 1, 10, 0,
				200, 120, 1, 10, 0,
				240, 120, 1, 10, 0,
				280, 120, 1, 10, 0,

				40, 160, 1, 10, 0,
				80, 160, 1, 5, 0,
				120, 160, 1, 5, 0,
				160, 160, 1, 5, 0,
				200, 160, 1, 5, 0,
				240, 160, 1, 5, 0,
				280, 160, 1, 10, 0,

				40, 200, 1, 200, 5,
				80, 200, 1, 5, 0,
				120, 200, 1, 0, 0,
				160, 200, 1, 0, 0,
				200, 200, 1, 0, 0,
				240, 200, 1, 5, 0,
				280, 200, 1, 10, 0,

				40, 240, 1, 10, 0,
				80, 240, 1, 5, 0,
				120, 240, 1, 0, 0,
				160, 240, 1, 0, 0,
				200, 240, 1, 0, 0,
				240, 240, 1, 5, 0,
				280, 240, 1, 10, 6,

				40, 280, 1, 10, 0,
				80, 280, 1, 5, 0,
				120, 280, 1, 5, 0,
				160, 280, 1, 5, 0,
				200, 280, 1, 5, 0,
				240, 280, 1, 5, 0,
				280, 280, 1, 10, 0,

				40, 320, 1, 10, 0,
				80, 320, 1, 10, 0,
				120, 320, 1, 10, 0,
				160, 320, 1, 10, 0,
				200, 320, 1, 10, 0,
				240, 320, 1, 10, 0,
				280, 320, 1, 10, 0,

				40, 360, 1, 200, 3,
				80, 360, 1, 10, 0,
				120, 360, 1, 10, 0,
				160, 360, 1, 10, 0,
				200, 360, 1, 10, 0,
				240, 360, 1, 10, 0,
				280, 360, 1, 200, 4,

		};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.cTheLastStruggle, data);

	}
	private static CampaignMap lvl100_5End(GalaxyDomination res, Player user, int gameLevel, int aiLevel) {
		int[] data = {5,
				249, 25, 1, 10, 1,
				201, 26, 1, 0, 0,
				74, 27, 1, 25, 2,
				155, 27, 1, 5, 0,
				94, 80, 1, 5, 0,
				145, 80, 1, 0, 0,
				215, 115, 2, 999, 0,
				86, 154, 1, 0, 0,
				139, 159, 1, 0, 0,
				185, 199, 1, 50, 3,
				140, 230, 1, 0, 0,
				82, 231, 1, 0, 0,
				200, 256, 2, 999, 0,
				75, 305, 1, 0, 0,
				145, 307, 1, 5, 0,
				235, 307, 1, 0, 0,
				198, 351, 1, 50, 4,
				92, 353, 1, 15, 5,
				140, 355, 1, 5, 0,
			};
		return new CampaignMap(res, user, gameLevel, aiLevel, R.string.c5End, data);
	}





}
