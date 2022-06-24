package com.spaghetti.core;

import com.spaghetti.input.Controller;
import com.spaghetti.networking.ConnectionEndpoint;

public abstract class GameMode {

	protected final Game game;
	
	public GameMode(Game game) {
		this.game = game;
	}
	
	/**
	 * Called when the game is started, this method is responsible
	 * for initializing all of the game's worlds and preparing it before it is
	 * accessed by other clients (or by the local player)
	 */
	public void initializeGame() {
		// Get a reference to the gamestate class
		GameState state = game.getGameState();
		
		// Add necessary levels
		state.addLevel("world");
		
		// Activate
		state.activateAllLevels();
	}
	
	/**
	 * Called when a client attempts a connection, this method is responsible
	 * for creating a controller for the client or refusing the connection by
	 * returning null
	 * 
	 * @param endpoint The connection endpoint is provided to allow a low level exchange with the client
	 * @return The {@link Controller} to be associated with this client, or null to refuse the connection
	 */
	public Controller<?> playerJoined(ConnectionEndpoint endpoint) {
		return new Controller<GameObject>();
	}
	
	/**
	 * Called when a client fully disconnects, this method is responsible
	 * for handling player leave logic (such as notifying other players of this event)
	 */
	public void playerLeft() {
	}
	
	/**
	 * Called when a client joins a world, this method is responsible for
	 * creating the player object.
	 * <p>
	 * This method is called whenever the player's current world is updated, meaning
	 * it will happen after every travel and also after {@link #playerJoined(ConnectionEndpoint)}
	 * <p>
	 * Remember to remove the player controller from the player object before destroying it,
	 * or it will be destroyed too. Failing to preserve the player controller will result in
	 * undefined behaviour
	 * 
	 * @param playerController The controller associated with the client
	 * @param from The world the player is about to leave. May be null (the first time the method is called)
	 * @param to The world the player is heading towards. Shouldn't be null
	 */
	public void playerTravel(Controller<?> playerController, Level from, Level to) {
		
	}
	
}
