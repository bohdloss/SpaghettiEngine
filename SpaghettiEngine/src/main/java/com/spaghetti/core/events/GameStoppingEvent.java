package com.spaghetti.core.events;

import com.spaghetti.core.Game;
import com.spaghetti.events.GameEvent;

public class GameStoppingEvent extends GameEvent {

    protected final Game game;

    public GameStoppingEvent(Game game) {
        this.game = game;
    }

    public Game getGame() {
        return game;
    }

}
