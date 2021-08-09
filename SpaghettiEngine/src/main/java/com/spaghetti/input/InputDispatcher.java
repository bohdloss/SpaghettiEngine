package com.spaghetti.input;

import java.util.ArrayList;

import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import com.spaghetti.core.*;
import com.spaghetti.interfaces.*;
import com.spaghetti.utils.*;

public class InputDispatcher {

	protected static enum MouseEvent {
		BUTTONCHANGE, MOVE, SCROLL
	}

	protected final Game game;

	protected GameWindow window;
	public float xscroll, yscroll;
	protected int x, y;
	protected boolean[] mouseButtons;
	protected boolean[] keyboardButtons;
	protected ArrayList<InputListener> listeners;

	public InputDispatcher(Game game) {
		this.game = game;
		this.window = game.getRenderer() == null ? null : game.getWindow();

		// Initialize variables
		mouseButtons = new boolean[GLFW.GLFW_MOUSE_BUTTON_LAST];
		keyboardButtons = new boolean[GLFW.GLFW_KEY_LAST];
		listeners = new ArrayList<>();
	}

	public void update() {

		// Fire mouse movement events
		Vector2i pointer = new Vector2i();
		window.getMousePosition(pointer);

		if (pointer.x != x || pointer.y != y) {
			// If the cursor is outside the bounds
			// of the window, ignore the event
			x = pointer.x;
			y = pointer.y;
			if (CMath.inrange(pointer.x, 0, window.getWidth()) && CMath.inrange(pointer.y, 0, window.getHeight())) {
				fireMouseEvent(MouseEvent.MOVE, 0, false);
			}
		}

		// Fire mouse scroll events
		// the scroll variable is set by the window itself
		if (xscroll != 0 || yscroll != 0) {
			fireMouseEvent(MouseEvent.SCROLL, 0, false);
			xscroll = 0;
			yscroll = 0;
		}

		for (int i = Mouse.FIRST; i < mouseButtons.length; i++) {

			// Update mouse buttons
			if (i < mouseButtons.length) {
				boolean current = window.mouseDown(i);

				if (current != mouseButtons[i]) {
					fireMouseEvent(MouseEvent.BUTTONCHANGE, i, current);
				}

				mouseButtons[i] = current;
			}

		}

		for (int i = Keyboard.FIRST; i < keyboardButtons.length; i++) {

			// Update keyboard keys
			if (i < keyboardButtons.length) {
				boolean current = window.keyDown(i);
				if (current != keyboardButtons[i]) {
					fireKeyEvent(i, current);
				}

				keyboardButtons[i] = current;
			}

		}

	}

	// Fire events to listeners

	protected void fireMouseEvent(MouseEvent event, int button, boolean pressed) {
		try {
			switch (event) {
			case BUTTONCHANGE:
				if (pressed) {
					for (InputListener c : listeners) {
						c.onMouseButtonPressed(button, x, y);
					}
				} else {
					for (InputListener c : listeners) {
						c.onMouseButtonReleased(button, x, y);
					}
				}
				break;
			case MOVE:
				for (InputListener c : listeners) {
					c.onMouseMove(x, y);
				}
				break;
			case SCROLL:
				for (InputListener c : listeners) {
					c.onMouseScroll(xscroll, yscroll, x, y);
				}
				break;
			}
		} catch (Throwable t) {
			Logger.error("Error dispatching mouse event", t);
		}
	}

	protected void fireKeyEvent(int key, boolean pressed) {
		try {
			if (pressed) {
				for (InputListener c : listeners) {
					c.onKeyPressed(key, x, y);
				}
			} else {
				for (InputListener c : listeners) {
					c.onKeyReleased(key, x, y);
				}
			}
		} catch (Throwable t) {
			Logger.error("Error dispatching key event", t);
		}
	}

	// Register listeners

	public synchronized void registerListener(InputListener listener) {
		if (listener == null) {
			Logger.warning("Attempted to register null input listener");
			return;
		}
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public synchronized void unregisterListener(InputListener listener) {
		listeners.remove(listener);
	}

}