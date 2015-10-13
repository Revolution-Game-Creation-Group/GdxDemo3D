package com.mygdx.game.input;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.settings.GameSettings;

/**
 * Created by user on 8/24/15.
 */
public class InputSystem extends EntitySystem {

	public static final String tag = "InputSystem";
	public final IntIntMap keys = new IntIntMap();
	private final IntentBroadcast intent;
	private final Vector2 keyPanDirection = new Vector2();
	private final ArrayMap<Integer, TouchData> touchMap = new ArrayMap<Integer, TouchData>();
	public InputProcessor inputProcessor;
	private float zoom;
	private Viewport viewport;
	private boolean killKeyBroadcasted = false;

	public InputSystem(Viewport viewport, IntentBroadcast intent) {
		zoom = GameSettings.CAMERA_MAX_ZOOM;
		this.intent = intent;
		inputProcessor = new MyInputListener();
		this.viewport = viewport;
	}

	@Override
	public void update(float deltaTime) {
		keyPanDirection.setZero();
		if (keys.containsKey(GameSettings.KEY_PAN_FORWARD)) {
			keyPanDirection.y += 1;
		}
		if (keys.containsKey(GameSettings.KEY_PAN_BACKWARD)) {
			keyPanDirection.y -= 1;
		}
		if (keys.containsKey(GameSettings.KEY_PAN_LEFT)) {
			keyPanDirection.x -= 1;
		}
		if (keys.containsKey(GameSettings.KEY_PAN_RIGHT)) {
			keyPanDirection.x += 1;
		}
		keyPanDirection.nor();

		if (zoom > GameSettings.CAMERA_MAX_ZOOM) {
			zoom = GameSettings.CAMERA_MAX_ZOOM;
		} else if (zoom < GameSettings.CAMERA_MIN_ZOOM) {
			zoom = GameSettings.CAMERA_MIN_ZOOM;
		}

		intent.keysMoveDirection.set(keyPanDirection);
		intent.zoom = zoom;

		if (keyPanDirection.isZero() && touchMap.containsKey(0)) {
			TouchData data = touchMap.get(0);
			if (data.isDragging) {
				intent.dragStart.set(data.down);
				intent.dragCurrent.set(data.lastDrag);
				intent.isDragging = true;
				intent.pan = false;
				intent.rotate = false;
				switch (data.button) {
					case Input.Buttons.LEFT:
						intent.pan = true;
						break;
					case Input.Buttons.RIGHT:
						intent.rotate = true;
						break;
					default:
						break;
				}
			}
			data.isDragging = false;
		}

		if (keys.containsKey(GameSettings.KEY_KILL_SELECTED)) {
			if (!killKeyBroadcasted) {
				intent.killSelected = true;
				killKeyBroadcasted = true;
			} else {
				intent.killSelected = false;
			}
		} else {
			intent.killSelected = false;
			killKeyBroadcasted = false;
		}

	}

	public class MyInputListener extends InputAdapter {

		@Override
		public boolean keyDown(int keycode) {
			keys.put(keycode, keycode);
			if (keycode == GameSettings.KEY_PAUSE) {
				if (GameSettings.GAME_SPEED == 1) {
					GameSettings.GAME_SPEED = 0;
				} else if (GameSettings.GAME_SPEED == 0) {
					GameSettings.GAME_SPEED = 0.05f;
				} else if (GameSettings.GAME_SPEED == 0.05f) {
					GameSettings.GAME_SPEED = 1;
				}
			}
			return true;
		}

		@Override
		public boolean keyUp(int keycode) {
			keys.remove(keycode, 0);
			return true;
		}

		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			if (!touchMap.containsKey(pointer)) {
				touchMap.put(pointer, new TouchData());
			}
			TouchData data = touchMap.get(pointer);
			if (data.button == -1) {
				data.down.set(screenX, screenY);
				data.button = button;
				data.isDragging = false;
			}
			intent.pickRay = viewport.getPickRay(screenX, screenY);
			return true;
		}

		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			if (!intent.isDragging) {
				intent.click.set(screenX, screenY);
			}
			intent.dragStart.setZero();
			intent.dragCurrent.setZero();
			intent.isDragging = false;
			intent.pan = false;
			intent.rotate = false;

			touchMap.get(pointer).reset();

			return true;
		}

		@Override
		public boolean touchDragged(int screenX, int screenY, int pointer) {
			TouchData data = touchMap.get(pointer);
			data.lastDrag.set(screenX, screenY);
			data.isDragging = true;
			return true;
		}

		@Override
		public boolean scrolled(int amount) {
			zoom += GameSettings.CAMERA_ZOOM_STEP * amount;
			return true;
		}
	}

	private class TouchData {
		int dragHistoryCursor = 0;
		Vector2 down = new Vector2();
		Vector2 lastDrag = new Vector2();
		int button;
		boolean isDragging = false;

		public TouchData() {
			reset();
		}

		public void reset() {
			dragHistoryCursor = 0;
			down.setZero();
			lastDrag.setZero();
			button = -1;
			isDragging = false;
		}
	}

}