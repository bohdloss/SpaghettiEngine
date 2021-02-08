package com.spaghettiengine.input;

import java.util.ArrayList;

import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import com.spaghettiengine.core.*;
import com.spaghettiengine.utils.*;
import com.spaghettiengine.interfaces.*;

public final class InputDispatcher {

	private static enum MouseEvent {
		BUTTONCHANGE, MOVE, SCROLL
	}

	private GameWindow window;
	public double scroll;
	private int x, y;
	private boolean[] mouseButtons;
	private boolean[] keyboardButtons;
	private ArrayList<Controllable> listeners;

	public InputDispatcher(GameWindow window) {
		this.window = window;

		// Initialize variables
		scroll = 0;
		x = 0;
		y = 0;
		mouseButtons = new boolean[GLFW.GLFW_MOUSE_BUTTON_LAST];
		keyboardButtons = new boolean[GLFW.GLFW_KEY_LAST];
		listeners = new ArrayList<>();
	}

	public synchronized void update() {

		// Fire mouse movement events
		Vector2i pointer = new Vector2i();
		window.getMousePosition(pointer);

		if (pointer.x != x || pointer.y != y) {
			// If the cursor is outside the bounds
			// of the window, ignore the event
			if (CMath.inrange(pointer.x, 0, window.getWidth()) && CMath.inrange(pointer.y, 0, window.getHeight())) {
				fireMouseEvent(MouseEvent.MOVE, 0, false);
			}
			x = pointer.x;
			y = pointer.y;
		}

		// Fire mouse scroll events
		// the scroll variable is set by the window itself
		if (scroll != 0) {
			fireMouseEvent(MouseEvent.SCROLL, 0, false);
			scroll = 0;
		}

		for (int i = 0; i < mouseButtons.length; i++) {

			// Update mouse buttons
			if (i < mouseButtons.length) {
				boolean current = window.mouseDown(i);

				if (current != mouseButtons[i]) {
					fireMouseEvent(MouseEvent.BUTTONCHANGE, i, current);
				}

				mouseButtons[i] = current;
			}

		}

		for (int i = 0; i < keyboardButtons.length; i++) {

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

	private void fireMouseEvent(MouseEvent event, int button, boolean pressed) {
		switch (event) {
		case BUTTONCHANGE:
			if (pressed) {
				listeners.forEach(listener -> listener.onMouseButtonPressed(button, x, y));
			} else {
				listeners.forEach(listener -> listener.onMouseButtonReleased(button, x, y));
			}
			break;
		case MOVE:
			listeners.forEach(listener -> listener.onMouseMove(x, y));
			break;
		case SCROLL:
			listeners.forEach(listener -> listener.onMouseScroll(scroll, x, y));
			break;
		}
	}

	private void fireKeyEvent(int key, boolean pressed) {
		if (pressed) {
			listeners.forEach(listener -> listener.onKeyPressed(key, x, y));
		} else {
			listeners.forEach(listener -> listener.onKeyReleased(key, x, y));
		}
	}

	// Register listeners

	public synchronized void registerListener(Controllable listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public synchronized void unregisterListener(Controllable listener) {
		listeners.remove(listener);
	}

}