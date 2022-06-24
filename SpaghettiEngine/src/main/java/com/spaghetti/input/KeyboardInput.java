package com.spaghetti.input;

import com.spaghetti.interfaces.InputListener;
import com.spaghetti.utils.Utils;

public class KeyboardInput extends InputDevice implements InputListener {

	// Bind certain key / mouse events to commands
	protected Integer[] binding_kdown = new Integer[Keyboard.LAST];
	protected Integer[] binding_kup = new Integer[Keyboard.LAST];

	protected Integer[] binding_mdown = new Integer[Mouse.LAST];
	protected Integer[] binding_mup = new Integer[Mouse.LAST];

	protected Integer[] binding_mscroll = new Integer[5];
	protected Integer binding_mmove; // There's only one command for when the mouse moves

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
		for (Controller<?> controller : controllers) {
			Integer cmd = binding_kdown[key];
			if (cmd != null) {
				controller.execCommand(cmd);
			}
		}
	}

	@Override
	public void onKeyReleased(int key, int x, int y) {
		for (Controller<?> controller : controllers) {
			Integer cmd = binding_kup[key];
			if (cmd != null) {
				controller.execCommand(cmd);
			}
		}
	}

	@Override
	public void onMouseMove(int x, int y) {
		for (Controller<?> controller : controllers) {
			if (binding_mmove != null) {
				controller.execCommand(binding_mmove);
			}
		}
	}

	@Override
	public void onMouseScroll(float xscroll, float yscroll, int x, int y) {
		for (Controller<?> controller : controllers) {
			if (binding_mscroll[SCROLLANY] != null) {
				controller.execCommand(binding_mscroll[SCROLLANY]);
			}

			if (xscroll != 0) {
				Integer cmd = binding_mscroll[xscroll > 0 ? SCROLLLEFT : SCROLLRIGHT];
				if (cmd != null) {
					controller.execCommand(cmd);
				}
			}

			if (yscroll != 0) {
				Integer cmd = binding_mscroll[yscroll > 0 ? SCROLLUP : SCROLLDOWN];
				if (cmd != null) {
					controller.execCommand(cmd);
				}
			}
		}
	}

	@Override
	public void onMouseButtonPressed(int button, int x, int y) {
		for (Controller<?> controller : controllers) {
			Integer cmd = binding_mdown[button];
			if (cmd != null) {
				controller.execCommand(cmd);
			}
		}
	}

	@Override
	public void onMouseButtonReleased(int button, int x, int y) {
		for (Controller<?> controller : controllers) {
			Integer cmd = binding_mup[button];
			if (cmd != null) {
				controller.execCommand(cmd);
			}
		}
	}

	// Key-binding management

	public void bindKeydownCmd(int key, String command) {
		binding_kdown[key] = Utils.intHash(command);
	}

	public void unbindKeydownCmd(int key) {
		binding_kdown[key] = null;
	}

	public void bindKeyupCmd(int key, String command) {
		binding_kup[key] = Utils.intHash(command);
	}

	public void unbindKeyupCmd(int key) {
		binding_kup[key] = null;
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
		binding_mdown[button] = Utils.intHash(command);
	}

	public void unbindBtndownCmd(int button) {
		binding_mdown[button] = null;
	}

	public void bindBtnupCmd(int button, String command) {
		binding_mup[button] = Utils.intHash(command);
	}

	public void unbindBtnupCmd(int button) {
		binding_mup[button] = null;
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
		binding_mscroll[direction] = Utils.intHash(command);
	}

	public void unbindScrollCmd(int direction) {
		binding_mscroll[direction] = null;
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
		binding_mmove = Utils.intHash(command);
	}

	public void unbindMoveCmd() {
		binding_mmove = null;
	}

}