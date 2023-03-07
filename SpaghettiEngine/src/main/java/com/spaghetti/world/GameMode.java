package com.spaghetti.world;

import com.spaghetti.core.Game;
import com.spaghetti.input.Controller;
import com.spaghetti.input.Updatable;
import com.spaghetti.networking.ConnectionEndpoint;

/**
 * GameMode is responsible for handling some of the game's aspects
 * such as loading the appropriate set of maps and / or making
 * modifications to them, and handling certain events like
 * when a client tries to connect, when it leaves or when it travels
 * through worlds
 *
 * @author bohdloss
 *
 */
public abstract class GameMode implements Updatable {

	protected final Game game;
	protected final GameState gameState;
	private boolean initialized;

	public GameMode(GameState gameState) {
		this.gameState = gameState;
		this.game = gameState.getGame();
	}

	public final void initialize() {
		if(!initialized) {
			onBeginPlay();
			initialized = true;
		}
	}

	public final void destroy() {
		if(initialized) {
			initialized = false;
			onEndPlay();
		}
	}

	public final boolean isInitialized() {
		return initialized;
	}

	/**
	 * Called when the game is started or when the game mode is changed, this method is responsible
	 * for initializing all the game's worlds and preparing it before it is
	 * accessed by other clients (or by the local player)
	 */
	protected void onBeginPlay() {
	}

	/**
	 * Called when the game is closing or when the game mode is changed, this can be used to
	 * perform a quick clean up
	 */
	protected void onEndPlay() {
	}

	/**
	 * Called when a client attempts a connection, this method is responsible
	 * for creating a controller for the client or refusing the connection by
	 * returning null
	 *
	 * @param endpoint The connection endpoint is provided to allow a low level exchange with the client
	 * @return The {@link Controller} to be associated with this client, or null to refuse the connection
	 */
	public Controller<?> onClientJoin(ConnectionEndpoint endpoint, boolean isClient) {
		return new Controller<>();
	}

	/**
	 * Called when a client fully disconnects, this method is responsible
	 * for handling player leave logic (such as notifying other players of this event)
	 *
	 * @param endpoint The connection endpoint is provided to retrieve information about the client that left
	 */
	public void onClientLeave(ConnectionEndpoint endpoint, boolean isClient) {
	}

	/**
	 * Called when a client joins a world, this method is responsible for
	 * creating the player object.
	 * <p>
	 * This method is called whenever the player's current world is updated, meaning
	 * it will happen after every travel and also after {@link #onClientJoin(ConnectionEndpoint, boolean)}
	 * <p>
	 * Remember to remove the player controller from the player object before destroying it,
	 * or it will be destroyed too. Failing to preserve the player controller will result in
	 * the client losing connection
	 *
	 * @param playerController The controller associated with the client
	 * @param from The world the player is about to leave. May be null (the first time the method is called)
	 * @param to The world the player is heading towards. Shouldn't be null
	 */
	public void onPlayerTravel(Controller<?> playerController, Level from, Level to, boolean isClient) {

	}

	/**
	 * This method will be called every frame updater frame
	 *
	 * @param delta the time, in milliseconds, since the last call to this methodS
	 */
	public void update(float delta) {

	}

}
