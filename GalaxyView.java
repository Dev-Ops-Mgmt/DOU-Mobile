package com.universe.defender;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;


public final class GalaxyView extends SurfaceView implements
		SurfaceHolder.Callback {

	private GalaxyThread thread;//The run-thread object
	private TextView textView;//A text panel over the level view to display the header.
	private GalaxyDomination activity;///Main application

	public GalaxyView(GalaxyDomination activity, Context context) {
		super(context);

		this.activity = activity;

		// register our interest in hearing about changes to our surface
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);

		Handler handler = new Handler() {
			@Override
			public void handleMessage(Message m) {
				onHandle(m);
			}
		};
		int orientation = activity.getWindowManager().getDefaultDisplay()
				.getOrientation();
		boolean isSreenPortrait = isScreenPortrait(orientation);
		// create thread only; it's started in surfaceCreated()
		this.thread = new GalaxyThread(holder, context, handler,
				isSreenPortrait);
		// thread.start();
		setFocusable(true); // make sure we get key events
	}

	/**
	 * Executed on the GUI-thread.
	 * Invoked by the run-thread.
	 *
	 * Allow the run-thread to notify the gui-thread something happen and have job to do.
	 *  - level ended
	 *  - sound needed to be played
	 *  - header had changed
	 */
	private void onHandle(Message m) {
		Bundle bundle = m.getData();
		if(bundle.containsKey("sound"))
		{
			GalaxyDomination.PlayPlanetSound(bundle.getInt("sound"));
		}
		else{
			String text = bundle.getString("text");
			if (text == "win" || text == "lost") {
				this.activity.gameEnded(text == "win");
			} else {
				this.textView.setVisibility((text.length() > 0 ? 1 : 0));
				this.textView.setText(text);
			}
		}
	}

	/**
	 *  GUI-thread
	 */
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.d("GalaxyView", "surfaceChanged");
	}
	/**
	 *  GUI-thread
	 */
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		thread.Pause(false);
	}
	/**
	 *  GUI-thread
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		thread.Pause(false);
	}

	/**
	 *  GUI-thread
	 *
	 *  Stop the GalaxyThread. It stop the current level playing.
	 */
	public void onExit() {
		thread.Exit();

	}
	/**
	 *  GUI-thread
	 */
	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		if (!hasWindowFocus) {
			thread.Pause(true);
		} else {
			thread.Pause(false);
		}
	}

	public GalaxyThread getThread() {
		return this.thread;
	}

	public void setTextView(TextView textView) {
		this.textView = textView;
	}

	public boolean onTouchEvent(MotionEvent event) {
		return thread.onTouchEvent(event);
	}

	/**
	 * Not used anymore?
	 */
	private static boolean isScreenPortrait(int orientation) {
		return (orientation == Configuration.ORIENTATION_UNDEFINED)
				|| (orientation == Configuration.ORIENTATION_PORTRAIT);
	}

	public void onOrientationChanged(float gAngus) {
		thread.onOrientationChanged(gAngus);
	}

}
