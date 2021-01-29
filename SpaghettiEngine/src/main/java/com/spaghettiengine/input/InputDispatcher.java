package com.spaghettiengine.input;

import java.util.ArrayList;

import org.joml.Vector2d;
import org.lwjgl.glfw.GLFW;

import com.spaghettiengine.core.*;
import com.spaghettiengine.utils.*;
import com.spaghettiengine.interfaces.*;

public final class InputDispatcher {
	
	private static enum MouseEvent{
		BUTTONCHANGE,
		MOVE,
		SCROLL
	}
	
	private GameWindow window;
	public double scroll;
	private Vector2d mousePosition;
	private boolean[] mouseButtons;
	private boolean[] keyboardButtons;
	private ArrayList<Controllable> listeners;
	
	public InputDispatcher(GameWindow window) {
		this.window = window;
		
		// Initialize variables
		scroll = 0;
		mousePosition = new Vector2d();
		mouseButtons = new boolean[GLFW.GLFW_MOUSE_BUTTON_LAST];
		keyboardButtons = new boolean[GLFW.GLFW_KEY_LAST];
		listeners = new ArrayList<>();
	}
	
	public synchronized void update() {
		for(int i = 0; i < mouseButtons.length; i++) {
			
			// Update mouse buttons
			if(i < mouseButtons.length) {
				boolean current = window.mouseDown(i);
				
				if(current != mouseButtons[i]) {
					fireMouseEvent(MouseEvent.BUTTONCHANGE, i, current, 0, 0, 0);
				}
				if(current) {
					fireContinuousMouseEvent(i);
				}
				
				mouseButtons[i] = current;
			}
			
		}
		
		for(int i = 0; i < keyboardButtons.length; i++) {
			
			// Update keyboard keys
			if(i < keyboardButtons.length) {
				boolean current = window.keyDown(i);
				if(current != keyboardButtons[i]) {
					fireKeyEvent(i, current);
				}
				if(current) {
					fireContinuousKeyEvent(i);
				}
				
				keyboardButtons[i] = current;
			}
			
		}
		
		// Update mouse movement
		int lastx = (int) mousePosition.x;
		int lasty = (int) mousePosition.y;
		window.getMousePosition(mousePosition);
		
		if((int) mousePosition.x != lastx || (int) mousePosition.y != lasty) {
			if(CMath.inrange(mousePosition.x, 0, window.getWidth()) || CMath.inrange(mousePosition.y, 0, window.getHeight())) {
				fireMouseEvent(MouseEvent.MOVE, 0, false, (int) mousePosition.x, (int) mousePosition.y, 0);
			}
		}
		
		// Fire mouse scroll events
		// the scroll variable is set by the window itself
		fireMouseEvent(MouseEvent.SCROLL, 0, false, 0, 0, scroll);
		scroll = 0;
	}

	// Fire events to listeners
	
	private void fireMouseEvent(MouseEvent event, int button, boolean pressed, int mousex, int mousey, double scroll) {
		switch(event) {
		case BUTTONCHANGE:
			if(pressed) {
				listeners.forEach(listener -> listener.onMouseButtonPressed(button));
			} else {
				listeners.forEach(listener -> listener.onMouseButtonReleased(button));
			}
			break;
		case MOVE:
			listeners.forEach(listener -> listener.onMouseMove(mousex, mousey));
			break;
		case SCROLL:
			listeners.forEach(listener -> listener.onMouseScroll(scroll));
			break;
		}
	}
	
	private void fireKeyEvent(int key, boolean pressed) {
		if(pressed) {
			listeners.forEach(listener -> listener.onKeyPressed(key));
		} else {
			listeners.forEach(listener -> listener.onKeyReleased(key));
		}
	}
	
	private void fireContinuousMouseEvent(int button) {
		listeners.forEach(listener -> listener.ifButtonDown(button));
	}
	
	private void fireContinuousKeyEvent(int key) {
		listeners.forEach(listener -> listener.ifKeyDown(key));
	}
	
	// Register listeners
	
	public synchronized void registerListener(Controllable listener) {
		if(!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}
	
	public synchronized void unregisterListener(Controllable listener) {
		listeners.remove(listener);
	}
	
}