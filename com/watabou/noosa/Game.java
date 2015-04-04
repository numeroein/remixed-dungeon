/*
 * Copyright (C) 2012-2014  Oleg Dolya
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.watabou.noosa;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import com.watabou.glscripts.Script;
import com.watabou.gltextures.TextureCache;
import com.watabou.input.Keys;
import com.watabou.input.Touchscreen;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.pixeldungeon.utils.Utils;
import com.watabou.utils.BitmapCache;
import com.watabou.utils.SystemTime;

public class Game extends Activity implements GLSurfaceView.Renderer, View.OnTouchListener {

	private static Game instance;
	private static Context context;
	
	// Actual size of the screen
	public static int width;
	public static int height;
	
	// Density: mdpi=1, hdpi=1.5, xhdpi=2...
	public static float density = 1;
	
	public static String version;
	
	// Current scene
	protected Scene scene;
	// true if scene switch is requested
	protected boolean requestedReset = true;
	// New scene class
	protected Class<? extends Scene> sceneClass;
	
	// Current time in milliseconds
	protected long now;
	// Milliseconds passed since previous update 
	protected long step;
	
	public static float timeScale = 1f;
	public static float elapsed = 0f;
	
	protected GLSurfaceView view;
	protected SurfaceHolder holder;
	
	// Accumulated touch events
	protected ArrayList<MotionEvent> motionEvents = new ArrayList<MotionEvent>();
	
	// Accumulated key events
	protected ArrayList<KeyEvent> keysEvents = new ArrayList<KeyEvent>();
	
	public Game( Class<? extends Scene> c ) {
		super();
		sceneClass = c;
	}
	
    @SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public static long getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        if(android.os.Build.VERSION.SDK_INT < 18){
	        long blockSize = stat.getBlockSize();
	        long availableBlocks = stat.getAvailableBlocks();
	        return availableBlocks * blockSize;
        }
        return stat.getAvailableBytes();
    }
	
	public void useLocale(String lang){
		Locale locale = new Locale(lang);
		Configuration config = getBaseContext().getResources().getConfiguration();
		config.locale = locale;
		getBaseContext().getResources().updateConfiguration(config,  
		getBaseContext().getResources().getDisplayMetrics());		
	}
	
	public void doRestart(){
		Intent i = instance().getBaseContext().getPackageManager()
	             .getLaunchIntentForPackage(getBaseContext().getPackageName() );
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);

		int piId = 123456;
		PendingIntent pi = PendingIntent.getActivity(getBaseContext(), piId, i, PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager mgr = (AlarmManager)getBaseContext().getSystemService(ContextWrapper.ALARM_SERVICE);
		mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pi);
		
		System.exit(0);
	}
	
	public static void toast(final String text, final Object... args){
		instance().runOnUiThread( new Runnable(){
			@Override
			public void run() {
				String toastText = text;
				Context context = instance().getApplicationContext();
				
				if (args.length > 0) {
					toastText = Utils.format( text, args );
				}
				
				android.widget.Toast toast = android.widget.Toast.makeText(context, toastText, android.widget.Toast.LENGTH_LONG);
				toast.show();
			}
		});

	}
	
	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		
		//Criado para manter o contexto e poder fazer a busca dos resources
		context = getApplicationContext();
		
		BitmapCache.context = TextureCache.context = instance(this);
		
		DisplayMetrics m = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics( m );
		density = m.density;
		
		try {
			version = getPackageManager().getPackageInfo( getPackageName(), 0 ).versionName;
		} catch (NameNotFoundException e) {
			version = "???";
		}
		
		setVolumeControlStream( AudioManager.STREAM_MUSIC );
		
		view = new GLSurfaceView( this );
		view.setEGLContextClientVersion( 2 );
		//Hope this allow game work on broader devices list
		//view.setEGLConfigChooser( false );
		view.setRenderer( this );
		view.setOnTouchListener( this );
		setContentView( view );
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		now = 0;
		view.onResume();
		
		Music.INSTANCE.resume();
		Sample.INSTANCE.resume();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		if (scene != null) {
			scene.pause();
		}
		
		view.onPause();
		Script.reset();
		
		Music.INSTANCE.pause();
		Sample.INSTANCE.pause();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		destroyGame();
		
		Music.INSTANCE.mute();
		Sample.INSTANCE.reset();
	}

	@SuppressLint({ "Recycle", "ClickableViewAccessibility" })
	@Override
	public boolean onTouch( View view, MotionEvent event ) {
		synchronized (motionEvents) {
			motionEvents.add( MotionEvent.obtain( event ) );
		}
		return true;
	}
	
	@Override
	public boolean onKeyDown( int keyCode, KeyEvent event ) {
		
		if (keyCode == Keys.VOLUME_DOWN || 
			keyCode == Keys.VOLUME_UP) {
			
			return false;
		}
		
		synchronized (motionEvents) {
			keysEvents.add( event );
		}
		return true;
	}
	
	@Override
	public boolean onKeyUp( int keyCode, KeyEvent event ) {
		
		if (keyCode == Keys.VOLUME_DOWN || 
			keyCode == Keys.VOLUME_UP) {
			
			return false;
		}
		
		synchronized (motionEvents) {
			keysEvents.add( event );
		}
		return true;
	}
	
	@Override
	public void onDrawFrame( GL10 gl ) {
		
		if (width == 0 || height == 0) {
			return;
		}
		
		SystemTime.tick();
		long rightNow = SystemTime.now;
		step = (now == 0 ? 0 : rightNow - now);
		now = rightNow;
		
		step();

		NoosaScript.get().resetCamera();
		GLES20.glScissor( 0, 0, width, height );
		GLES20.glClear( GLES20.GL_COLOR_BUFFER_BIT );
		draw();
	}

	@Override
	public void onSurfaceChanged( GL10 gl, int width, int height ) {
		
		GLES20.glViewport( 0, 0, width, height );
		
		Game.width = width;
		Game.height = height;

	}

	@Override
	public void onSurfaceCreated( GL10 gl, EGLConfig config ) {
		GLES20.glEnable( GL10.GL_BLEND );
		// For premultiplied alpha:
		// GLES20.glBlendFunc( GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA );
		GLES20.glBlendFunc( GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA );
		
		GLES20.glEnable( GL10.GL_SCISSOR_TEST );
		
		TextureCache.reload();
	}
	
	protected void destroyGame() {
		if (scene != null) {
			scene.destroy();
			scene = null;
		}
		
		instance(null);
	}
	
	public static void resetScene() {
		switchScene( instance().sceneClass );
	}
	
	public static void switchScene( Class<? extends Scene> c ) {
		instance().sceneClass     = c;
		instance().requestedReset = true;
	}
	
	public static Scene scene() {
		return instance().scene;
	}
	
	protected void step() {
		
		if (requestedReset) {
			requestedReset = false;
			try {
				switchScene(sceneClass.newInstance());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		update();
	}
	
	protected void draw() {
		scene.draw();
	}
	
	protected void switchScene(Scene requestedScene) {

		Camera.reset();
		
		if (scene != null) {
			scene.destroy();
		}
		scene = requestedScene;
		scene.create();
		
		Game.elapsed = 0f;
		Game.timeScale = 1f;
	}
	
	protected void update() {
		Game.elapsed = Game.timeScale * step * 0.001f;
		
		synchronized (motionEvents) {
			Touchscreen.processTouchEvents( motionEvents );
			motionEvents.clear();
		}
		synchronized (keysEvents) {
			Keys.processTouchEvents( keysEvents );
			keysEvents.clear();
		}
		
		scene.update();		
		Camera.updateAll();
	}
	
	public static void vibrate( int milliseconds ) {
		((Vibrator)instance().getSystemService( VIBRATOR_SERVICE )).vibrate( milliseconds );
	}
	
	public static String getVar(int id){
		return context.getResources().getString(id);
	}
	public static String[] getVars(int id){
		return context.getResources().getStringArray(id);
	}

	public static Game instance() {
		return instance;
	}

	public static Game instance(Game instance) {
		Game.instance = instance;
		return instance;
	}
	
	protected void donate(int level) {
	}
}
