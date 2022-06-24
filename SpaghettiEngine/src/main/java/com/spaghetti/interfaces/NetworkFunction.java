package com.spaghetti.interfaces;

import com.spaghetti.networking.ConnectionManager;

public interface NetworkFunction {

	public abstract void execute(ConnectionManager client);

}
