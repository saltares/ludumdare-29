package com.siondream.ld29;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.TweenEquations;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldListener;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.siondream.ld29.room.Action;
import com.siondream.ld29.room.ActionResult;
import com.siondream.ld29.room.AddFactPostAction;
import com.siondream.ld29.room.ChangeDescriptionPostAction;
import com.siondream.ld29.room.ChangeRoomPostAction;
import com.siondream.ld29.room.FactCondition;
import com.siondream.ld29.room.Room;
import com.siondream.ld29.room.RoomManager;

public class GameScreen extends ScreenAdapter implements InputProcessor {

	private RoomManager roomManager;
	private VignetteController vignetteController;

	// UI Stuff
	private Stage stage;
	private TextField actionField;
	private Image titleImage;
	private Image backgroundImage;
	private Image resultImage;
	private Image actionImage;
	private Image descriptionImage;
	private WidgetGroup descriptionGroup;
	private WidgetGroup actionGroup;
	private WidgetGroup resultGroup;
	private WidgetGroup globalGroup;
	private TypeWriterLabel descriptionLabel;
	private TypeWriterLabel resultLabel;
	private Label actionLabel;
	private Label creditLabel;
	private Label thanksLabel;
	
	private int previousWidth;
	private int previousHeight;

	private float resolution[];

	FrameBuffer fbo;
	TextureRegion fboRegion;
	
	Timeline timeline;

	public GameScreen() {
		loadRooms();

		vignetteController = new VignetteController();

		Viewport viewport = Env.game.getViewport();
		stage = new Stage(viewport);

		resolution = new float[2];

		previousWidth = 0;
		previousHeight = 0;
		
		createUI();
	}

	@Override
	public void render(float delta) {
		vignetteController.update(delta);

		// Update stage
		stage.act(delta);

		Viewport viewport = Env.game.getViewport();
		resolution[0] = Gdx.graphics.getWidth();
		resolution[1] = Gdx.graphics.getHeight();

		// Render
		stage.getSpriteBatch().setShader(null);
		fbo.begin();
		viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		stage.draw();
		fbo.end();

		Batch batch = stage.getSpriteBatch();
		batch.setShader(Assets.shader);

		viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		batch.begin();
		Assets.shader.setUniform2fv("resolution", resolution, 0, 2);
		Assets.shader.setUniformf("radius", vignetteController.getRadius());
		batch.draw(fboRegion, 0.0f, 0.0f);
		batch.end();

		stage.draw();
	}

	@Override
	public void dispose() {

	}

	@Override
	public void hide() {
		Assets.song.stop();
	}

	@Override
	public void show() {
		Gdx.input.setInputProcessor(this);
		roomManager.reset();
		stage.setKeyboardFocus(actionField);
		setRoom(roomManager.getRoom().getName());
		descriptionLabel.stop();
		Assets.song.setLooping(true);
		Assets.song.play();
		animateIn();
	}

	@Override
	public void resize(int width, int height) {
		if (width != previousWidth && height != previousHeight) {
			positionUI();
			Viewport viewport = Env.game.getViewport();
			fbo = new FrameBuffer(Format.RGB888, viewport.getViewportWidth(),
					viewport.getViewportHeight(), false);
			fboRegion = new TextureRegion(fbo.getColorBufferTexture());
			fboRegion.flip(false, true);
			
			previousWidth = width;
			previousHeight = height;
		}
	}

	public RoomManager getRoomManager() {
		return roomManager;
	}

	@Override
	public boolean keyDown(int keycode) {
		stage.keyDown(keycode);
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		stage.keyUp(keycode);
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		stage.keyTyped(character);
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		stage.touchDown(screenX, screenY, pointer, button);
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		stage.touchUp(screenX, screenY, pointer, button);
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		stage.touchDragged(screenX, screenY, pointer);
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		stage.mouseMoved(screenX, screenY);
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		stage.scrolled(amount);
		return false;
	}
	
	public void setRoom(String name) {
		roomManager.setRoom(name);
		descriptionLabel.setText(roomManager.getRoom().getDescription());
		actionField.setVisible(false);
		actionLabel.setVisible(false);
	}

	private void loadRooms() {

		try {
			Json json = new Json();
			json.setElementType(Room.class, "actions", Action.class);
			json.setElementType(Action.class, "conditions", FactCondition.class);
			json.addClassTag("addFact", AddFactPostAction.class);
			json.addClassTag("changeRoom", ChangeRoomPostAction.class);
			json.addClassTag("changeDescription", ChangeDescriptionPostAction.class);

			roomManager = json.fromJson(RoomManager.class,
					Gdx.files.internal("rooms.json"));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void createUI() {
		stage.clear();
		
		actionLabel = new Label("What do you do?", Assets.skin);
		actionLabel.setColor(Color.WHITE);
		actionLabel.setVisible(false);
		
		actionField = new TextField("", Assets.skin);
		actionField.setVisible(false);

		titleImage = new Image(Assets.title);
		
		backgroundImage = new Image(Assets.background);
		backgroundImage.setColor(1.0f, 1.0f, 1.0f, 0.0f);
		
		actionImage = new Image(Assets.smallPanel);
		resultImage = new Image(Assets.smallPanel);
		descriptionImage = new Image(Assets.descriptionPanel);
		descriptionImage.setColor(1.0f, 1.0f, 1.0f, 0.0f);
		
		descriptionLabel = new TypeWriterLabel("", Assets.skin);
		descriptionLabel.setSize(800.0f, 300.0f);
		descriptionLabel.setWrap(true);
		descriptionLabel.setFontScale(1.2f);
		descriptionLabel.setCompletionListener(new TypeWriterListenerDescription());
		descriptionLabel.setColor(1.0f, 1.0f, 1.0f, 0.0f);
		
		creditLabel = new Label("A game created by David Saltares in 48h for Ludum Dare #29", Assets.skin);
		
		resultLabel = new TypeWriterLabel("", Assets.skin);
		resultLabel.setCompletionListener(new TypeWriterListener());
		
		thanksLabel = new Label("Thanks for playing!", Assets.skin);
		thanksLabel.setFontScale(2.5f);
		
		resultGroup = new WidgetGroup();
		descriptionGroup = new WidgetGroup();
		actionGroup = new WidgetGroup();
		
		Viewport viewport = Env.game.getViewport();
		globalGroup = new WidgetGroup();
		globalGroup.setSize(viewport.getWorldWidth(), viewport.getWorldHeight());
		
		resultGroup.addActor(resultImage);
		resultGroup.addActor(resultLabel);
		
		descriptionGroup.addActor(descriptionImage);
		descriptionGroup.addActor(descriptionLabel);
		
		actionGroup.addActor(actionImage);
		actionGroup.addActor(actionLabel);
		actionGroup.addActor(actionField);
		
		globalGroup.addActor(actionGroup);
		globalGroup.addActor(descriptionGroup);
		globalGroup.addActor(resultGroup);

		stage.addActor(backgroundImage);
		stage.addActor(titleImage);
		stage.addActor(globalGroup);
		stage.addActor(creditLabel);
		stage.addActor(thanksLabel);
		
		actionField.setTextFieldListener(new TextFieldListener() {

			@Override
			public void keyTyped(TextField textField, char c) {
				if (c == '\r') {
					String text = textField.getText();
					String[] parts = text.toLowerCase().split(" ");
					
					ActionResult result = new ActionResult(false);
					
					if (parts.length >= 2) {
						String verb = parts[0];
						String object = parts[1];
						
						result = roomManager.runAction(verb, object);
						
						actionField.setTouchable(Touchable.disabled);
						actionLabel.addAction(Actions.alpha(0.0f, 1.0f, Interpolation.pow4Out));
						actionField.addAction(Actions.alpha(0.0f, 1.0f, Interpolation.pow4Out));
						
						if (roomManager.isFinished()) {
							animatePanelsOut();
						}
					}
					
					resultLabel.setText(result.message);
					resultLabel.reset();
					
					textField.setText("");
				}
			}
			
		});
		
		positionUI();
	}

	private void positionUI() {
		titleImage.setX((stage.getWidth() - titleImage.getWidth()) * 0.5f);
		titleImage.setY(stage.getHeight() - titleImage.getHeight() - 40.0f);

		descriptionGroup.setSize(descriptionImage.getWidth(), descriptionImage.getHeight());
		descriptionGroup.setX((stage.getWidth() - descriptionGroup.getWidth()) * 0.5f);
		descriptionGroup.setY(200.0f);
		
		descriptionLabel.setAlignment(Align.left);
		descriptionLabel.setSize(descriptionGroup.getWidth() - 60.0f, descriptionGroup.getHeight() - 60.0f);
		descriptionLabel.setPosition(30.0f, 30.0f);
			
		actionGroup.setSize(actionImage.getWidth(), actionImage.getHeight());
		actionGroup.setPosition(descriptionGroup.getX(), descriptionGroup.getY() - actionGroup.getHeight() - 10.0f);
		
		actionLabel.setPosition(30.0f, (actionGroup.getHeight() - actionLabel.getHeight()) * 0.5f);
		actionField.setPosition(actionLabel.getRight() + 20.0f, (actionGroup.getHeight() - actionField.getHeight()) * 0.5f);
		actionField.setWidth(actionGroup.getWidth() - actionField.getX() - 30.0f);

		resultGroup.setSize(resultImage.getWidth(), resultImage.getHeight());
		resultGroup.setPosition(descriptionGroup.getX(), actionGroup.getY() - resultGroup.getHeight() - 15.0f);
		
		resultLabel.setPosition(30.0f, (resultGroup.getHeight() - resultLabel.getHeight()) * 0.5f);
		
		creditLabel.setPosition(Env.game.getViewport().getWorldWidth() - creditLabel.getWidth() - 40.0f, 20.0f);
		
		thanksLabel.setPosition(stage.getWidth(), (stage.getHeight() - thanksLabel.getHeight()) * 0.5f);
		
		animateIn();
	}
	
	private void animateIn() {
		Viewport viewport = Env.game.getViewport();
		float width = viewport.getWorldWidth();
		float height = viewport.getWorldHeight();
		
		TweenCallback callback = new TweenCallback() {

			@Override
			public void onEvent(int type, BaseTween<?> source) {
				if (type == TweenCallback.COMPLETE) {
					descriptionLabel.play();
				}
			}
			
		};
		
		if (timeline != null)
			Env.game.getTweenManager().killTarget(timeline);
		
		Timeline timeline = Timeline.createSequence();
		
		globalGroup.setPosition(0, -height);
		titleImage.setPosition((stage.getWidth() - titleImage.getWidth()) * 0.5f, height);
		creditLabel.setPosition(stage.getWidth(), 20.0f);
		
		timeline.beginSequence()
			.beginParallel()
					   
				// Animate in
				.push(Tween.to(titleImage, ActorTweener.Position, 1.5f)
						   .target((stage.getWidth() - titleImage.getWidth()) * 0.5f, stage.getHeight() - titleImage.getHeight() - 40.0f)
						   .ease(TweenEquations.easeInOutQuad))
				.push(Tween.to(globalGroup, ActorTweener.Position, 2.0f)
						   .target(0.0f, 0.0f)
						   .ease(TweenEquations.easeInOutQuad))
						   .delay(0.5f)
			.end()
			
			.push(Tween.to(creditLabel, ActorTweener.Position, 1.0f)
					   .target(stage.getWidth() - creditLabel.getWidth() - 40.0f, 20.0f)
					   .ease(TweenEquations.easeInOutQuad))
			
			.end()
			.setCallback(callback) 
			.start(Env.game.getTweenManager());
	}
	
	private void animatePanelsOut() {
		Timeline timeline = Timeline.createSequence();
		
		timeline.beginSequence()
				.push(Tween.to(resultGroup, ActorTweener.Position, 1.0f)
						   .target(resultGroup.getX(), -resultGroup.getHeight())
						   .ease(TweenEquations.easeInOutQuad))
				.push(Tween.to(actionGroup, ActorTweener.Position, 1.0f)
						   .target(resultGroup.getX(), -resultGroup.getHeight())
						   .ease(TweenEquations.easeInOutQuad))
				.end()
				.start(Env.game.getTweenManager());
	}
	
	private void animateDescriptionOut() {
		Timeline timeline = Timeline.createSequence();
		
		timeline.beginSequence()
		.push(Tween.to(descriptionGroup, ActorTweener.Position, 2.0f)
			   	   .target(-descriptionGroup.getWidth() - descriptionGroup.getX(), descriptionGroup.getY())
			   	   .ease(TweenEquations.easeInOutQuad))
	    .delay(1.0f)
		.push(Tween.to(thanksLabel, ActorTweener.Position, 1.0f)
				   .target((stage.getWidth() - thanksLabel.getWidth() * thanksLabel.getFontScaleX()) * 0.5f, thanksLabel.getY())
				   .ease(TweenEquations.easeInOutQuad))
		.end()
		.start(Env.game.getTweenManager());
	}

	private class TypeWriterListener implements TypeWriterLabel.CompletionListener {

		@Override
		public void onFinished(TypeWriterLabel label) {
			actionField.setTouchable(Touchable.enabled);

			actionField.setVisible(true);
			actionLabel.setVisible(true);

			Color labelColor = actionLabel.getColor();
			actionLabel.setColor(labelColor.r, labelColor.g, labelColor.b, 0.0f);
			actionLabel.addAction(Actions.alpha(1.0f, 2.5f, Interpolation.pow4Out));

			Color fieldColor = actionField.getColor();
			actionField.setColor(fieldColor.r, fieldColor.g, fieldColor.b, 0.0f);
			actionField.addAction(Actions.alpha(1.0f, 2.5f, Interpolation.pow4Out));
		}
	}
	
	private class TypeWriterListenerDescription implements TypeWriterLabel.CompletionListener {

		@Override
		public void onFinished(TypeWriterLabel label) {
			actionField.setTouchable(Touchable.enabled);

			actionField.setVisible(true);
			actionLabel.setVisible(true);

			Color labelColor = actionLabel.getColor();
			actionLabel.setColor(labelColor.r, labelColor.g, labelColor.b, 0.0f);
			actionLabel.addAction(Actions.alpha(1.0f, 2.5f, Interpolation.pow4Out));

			Color fieldColor = actionField.getColor();
			actionField.setColor(fieldColor.r, fieldColor.g, fieldColor.b, 0.0f);
			actionField.addAction(Actions.alpha(1.0f, 2.5f, Interpolation.pow4Out));
			
			if (roomManager.isFinished()) {
				animateDescriptionOut();
			}
		}
	}
	
}

