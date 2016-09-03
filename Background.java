package com.universe.defender;

import java.util.Random;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * The background image during a level.
 *
 *  Note: there is some stars (one pixel) that appears and disappears during the
 *   level. It seems nobody have notice that...
 */
public final class Background {

	/**
	 * The backgroung image.
	 */
	private static Bitmap backgroundImage;
	/**
	 * Hard encoded stars instance collection.
	 * Each dynamic star have 3 int:
	 *  - starPaintsIndex (see this.starPaints)
	 *  - X in pixel
	 *  - Y in pixel
	 */
	private int[] stars;
	/**
	 * Number of dynamic Stars
	 */
	private int nbStars = 0;
	private long lastTime = System.currentTimeMillis();
	/**
	 * Colors than a dynamic star can have
	 */
	private Paint[] starPaints;

	public Background(Context context) {
		Resources res = context.getResources();
		if(Background.backgroundImage == null){
			Background.backgroundImage = BitmapFactory.decodeResource(res, R.drawable.nebula);
		}
		this.starPaints = new Paint[5];
		this.starPaints[0] = new Paint();
		this.starPaints[0].setColor(Color.WHITE);
		this.starPaints[0].setAlpha(200);
		this.starPaints[0].setAntiAlias(true);
		this.starPaints[0].setStrokeWidth(1);
		this.starPaints[1] = new Paint();
		this.starPaints[1].setColor(Color.RED);
		this.starPaints[1].setAlpha(80);
		this.starPaints[1].setAntiAlias(true);
		this.starPaints[1].setStrokeWidth(1);
		this.starPaints[2] = new Paint();
		this.starPaints[2].setColor(Color.YELLOW);
		this.starPaints[2].setAlpha(150);
		this.starPaints[2].setAntiAlias(true);
		this.starPaints[2].setStrokeWidth(1);
		this.starPaints[3] = new Paint();
		this.starPaints[3].setColor(Color.GRAY);
		this.starPaints[3].setAlpha(150);
		this.starPaints[3].setAntiAlias(true);
		this.starPaints[3].setStrokeWidth(1);
		this.starPaints[4] = new Paint();
		this.starPaints[4].setColor(Color.GRAY);
		this.starPaints[4].setAlpha(80);
		this.starPaints[4].setAntiAlias(true);
		this.starPaints[4].setStrokeWidth(1);

	}

	private static Random random = new Random();

	public void init() {
		this.nbStars = 20;
		this.stars = new int[this.nbStars * 3];
		for (int i = 0; i < this.nbStars; i++) {
			this.stars[i * 3 + 0] = 1;
			this.stars[i * 3 + 1] = random.nextInt(CampaignMap.screenWidth);
			this.stars[i * 3 + 2] = random.nextInt(CampaignMap.screenHeight);
		}
	}

	/**
	 * Change color of some stars.
	 * (To try: It can be funny to make these stars moving)
	 */
	public void doPhysic(long now) {
		if(GalaxyDomination.isLowPerf){
			return;
		}
		int duration = (int) (now - this.lastTime);
		if (duration < 3000) {
			return;
		}
		this.lastTime = now;
		int nbStars = this.nbStars;
		int[] stars = this.stars;
		for (int i = 0; i < nbStars; i++) {
			if (random.nextInt(100) < 10) {
				int index = i * 3;
				stars[index] = random.nextInt(5);
			}
		}
	}

	/**
	 * Clean the screen by drawing the image in full screen, then the dynamic star.
	 */
	public void doDraw(Canvas canvas) {
		canvas.drawBitmap(Background.backgroundImage, 0, 0, null);

		if(GalaxyDomination.isLowPerf){
			return;
		}
		int nbStars = this.nbStars;
		int[] stars = this.stars;
		Paint[] starPaints = this.starPaints;
		for (int i = 0; i < nbStars; i++) {
			int index = i * 3;
			int state = stars[index];
			int x = stars[index + 1];
			int y = stars[index + 2];
			canvas.drawCircle(x, y, 1 * CampaignMap.mapToScreenScale, starPaints[state]);
		}
	}
}
