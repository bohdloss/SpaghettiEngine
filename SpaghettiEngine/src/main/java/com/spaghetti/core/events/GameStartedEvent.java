package com.spaghetti.core.events;

import com.spaghetti.core.Game;
import com.spaghetti.events.GameEvent;

public class GameStartedEvent extends GameEvent {

    protected final Game game;

    public GameStartedEvent(Game game) {
        this.game = game;
    }

    public Game getGame() {
        return game;
    }

}
