package com.spaghetti.demo.player;

import com.spaghetti.input.Controller;

public class PlayerController extends Controller<Player> {

	public PlayerController() {
		registerCommands("jump", Player::jump, Player::jump_stop);
		registerCommands("right", Player::right, Player::right_stop);
		registerCommands("left", Player::left, Player::left_stop);
		registerCommands("up", Player::up, Player::up_stop);
		registerCommands("down", Player::down, Player::down_stop);
	}

}
