package com.spaghetti.input;

import com.spaghetti.core.GameObject;
import com.spaghetti.interfaces.InputListener;

public class PlayerController<T extends GameObject> extends Controller<T> implements InputListener {
	
	// Bind certain key / mouse events to commands
	protected String[] binding_kdown = new String[Keyboard.LAST];
	protected String[] binding_kup = new String[Keyboard.LAST];
	
	protected String[] binding_mdown = new String[Mouse.LAST];
	protected String[] binding_mup = new String[Mouse.LAST];
	
	protected String[] binding_mscroll = new String[5];
	protected String binding_mmove; // There's only one command for when the mouse moves
	
	public static final int SCROLLUP = 0, SCROLLDOWN = 1, SCROLLRIGHT = 2, SCROLLLEFT = 3, SCROLLANY = 4;
	
	// Interface implementation
	
	@Override
	public void onBeginPlay() {
		getGame().getInputDispatcher().registerListener(this);
	}
	
	@Override
	public void onEndPlay() {
		getGame().getInputDispatcher().unregisterListener(this);
	}

	@Override
	public void onKeyPressed(int key, int x, int y) {
		String cmd = binding_kdown[key];
		if(cmd != null) {
			execCommandWithParams(cmd, x, y);
		}
	}

	@Override
	public void onKeyReleased(int key, int x, int y) {
		String cmd = binding_kup[key];
		if(cmd != null) {
			execCommandWithParams(cmd, x, y);
		}
	}

	@Override
	public void onMouseMove(int x, int y) {
		if(binding_mmove != null) {
			execCommandWithParams(binding_mmove, x, y);
		}
	}

	@Override
	public void onMouseScroll(float xscroll, float yscroll, int x, int y) {
		if(binding_mscroll[SCROLLANY] != null) {
			execCommandWithParams(binding_mscroll[SCROLLANY], xscroll, yscroll, x, y);
		}
		
		if(xscroll != 0) {
			String cmd = binding_mscroll[xscroll > 0 ? SCROLLLEFT : SCROLLRIGHT];
			if(cmd != null) {
				execCommandWithParams(cmd, xscroll, x, y);
			}
		}
		
		if(yscroll != 0) {
			String cmd = binding_mscroll[yscroll > 0 ? SCROLLUP : SCROLLDOWN];
			if(cmd != null) {
				execCommandWithParams(cmd, yscroll, x, y);
			}
		}
	}

	@Override
	public void onMouseButtonPressed(int button, int x, int y) {
		String cmd = binding_mdown[button];
		if(cmd != null) {
			execCommandWithParams(cmd, x, y);
		}
	}

	@Override
	public void onMouseButtonReleased(int button, int x, int y) {
		String cmd = binding_mup[button];
		if(cmd != null) {
			execCommandWithParams(cmd, x, y);
		}
	}

	// Key-binding management
	
	public void bindKeydownCmd(int key, String command) {
		binding_kdown[key] = command;
	}
	
	public void unbindKeydownCmd(int key) {
		binding_kdown[key] = null;
	}
	
	public String getKeydownCmd(int key) {
		return binding_kdown[key];
	}
	
	public void bindKeyupCmd(int key, String command) {
		binding_kup[key] = command;
	}
	
	public void unbindKeyupCmd(int key) {
		binding_kup[key] = null;
	}
	
	public String getKeyupCmd(int key) {
		return binding_kup[key];
	}
	
	// Fast key-bind
	
	public void bindKeyCmd(int key, String downCmd, String upCmd) {
		bindKeydownCmd(key, downCmd);
		bindKeyupCmd(key, upCmd);
	}
	
	public void unbindKeyCmd(int key) {
		unbindKeydownCmd(key);
		unbindKeyupCmd(key);
	}
	
	// Mouse-binding management
	
	public void bindBtndownCmd(int button, String command) {
		binding_mdown[button] = command;
	}
	
	public void unbindBtndownCmd(int button) {
		binding_mdown[button] = null;
	}
	
	public String getBtndownCmd(int button) {
		return binding_mdown[button];
	}
	
	public void bindBtnupCmd(int button, String command) {
		binding_mup[button] = command;
	}
	
	public void unbindBtnupCmd(int button) {
		binding_mup[button] = null;
	}
	
	public String getBtnupCmd(int button) {
		return binding_mup[button];
	}
	
	// Fast button-bind
	
	public void bindBtnCmd(int button, String downCmd, String upCmd) {
		bindBtndownCmd(button, downCmd);
		bindBtnupCmd(button, upCmd);
	}
	
	public void unbindBtnCmd(int button) {
		unbindBtndownCmd(button);
		unbindBtnupCmd(button);
	}
	
	// Move / Scroll -binding management
	
	public void bindScrollCmd(int direction, String command) {
		binding_mscroll[direction] = command;
	}
	
	public void unbindScrollCmd(int direction) {
		binding_mscroll[direction] = null;
	}
	
	public String getScrollCmd(int direction) {
		return binding_mscroll[direction];
	}
	
	// Fast scroll-bind
	
	public void bindScrollCmd(String upCmd, String downCmd) {
		bindScrollCmd(SCROLLUP, upCmd);
		bindScrollCmd(SCROLLDOWN, downCmd);
	}
	
	public void bindScrollCmd(String upCmd, String downCmd, String rightCmd, String leftCmd) {
		bindScrollCmd(SCROLLUP, upCmd);
		bindScrollCmd(SCROLLDOWN, downCmd);
		bindScrollCmd(SCROLLRIGHT, rightCmd);
		bindScrollCmd(SCROLLLEFT, leftCmd);
	}
	
	public void bindScrollCmd(String cmd) {
		bindScrollCmd(SCROLLANY, cmd);
	}
	
	public void unbindScrollCmd() {
		unbindScrollCmd(SCROLLUP);
		unbindScrollCmd(SCROLLDOWN);
		unbindScrollCmd(SCROLLRIGHT);
		unbindScrollCmd(SCROLLLEFT);
		unbindScrollCmd(SCROLLANY);
	}
	
	public void bindMoveCmd(String command) {
		binding_mmove = command;
	}
	
	public void unbindMoveCmd() {
		binding_mmove = null;
	}
	
	public String getMoveCmd() {
		return binding_mmove;
	}
	
}