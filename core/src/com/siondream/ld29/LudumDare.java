package com.siondream.ld29;

import java.util.Iterator;

import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenManager;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class LudumDare extends Game {
	public static final String TAG = "LudumDare";
	
	private OrthographicCamera camera;
	private Viewport viewport;
	private Assets assets;
	private TweenManager tweenManager;
	
	private ObjectMap<Class<? extends Screen>, Screen> screens;
	private Screen nextScreen;
	
	@Override
	public void create () {
		Env.init(this);
		
		camera = new OrthographicCamera();
		viewport = new ExtendViewport(960, 720, 1280, 720, camera);
		assets = new Assets();
		
		tweenManager = new TweenManager();
		Tween.setCombinedAttributesLimit(4);
		Tween.registerAccessor(Actor.class, new ActorTweener());
		
		screens = new ObjectMap<Class<? extends Screen>, Screen>();
		screens.put(GameScreen.class, new GameScreen());
		
		setScreen(GameScreen.class);
	}
	
	@Override
	public void resize(int width, int height) {
		viewport.update(width, height);
		camera.position.set(viewport.getWorldWidth() * 0.5f, viewport.getWorldHeight() * 0.5f, 0.0f);
		super.resize(width, height);
	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		camera.update();
		tweenManager.update(Gdx.graphics.getDeltaTime());
		
		super.render();
		
		performScreenChange();
	}
	
	@Override
	public void dispose() {
		assets.dispose();
		
		Iterator<Screen> screenIt = screens.values().iterator();
		
		while (screenIt.hasNext()) {
			screenIt.next().dispose();
		}
	}
	
	public OrthographicCamera getCamera() {
		return camera;
	}
	
	public Viewport getViewport() {
		return viewport;
	}
	
	public Assets getAssets() {
		return assets;
	}
	
	public TweenManager getTweenManager() {
		return tweenManager;
	}
	
	public Screen getScreen(Class<? extends Screen> screenClass) {
		Screen screen = screens.get(screenClass);
 		
		if (screen == null) {
			return null;
		}
		
		return screen;
	}
	
	public void setScreen(Class<? extends Screen> screenClass) {
		Screen screen = screens.get(screenClass);
		
		if (screen == null) {
			Gdx.app.log(TAG, "there is no registered screen of class " + screenClass.getSimpleName());
		}
		else {
			nextScreen = screen;
		}
	}
	
	@Override
	public void setScreen(Screen screen) {
		nextScreen = screen;
	}
	
	private void performScreenChange() {
		if (nextScreen != null) {
			super.setScreen(nextScreen);
			nextScreen = null;
		}
	}
}
