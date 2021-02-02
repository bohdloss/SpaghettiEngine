package com.spaghettiengine.interfaces;

import com.spaghettiengine.utils.NetworkBuffer;

public interface Replicable {

	public abstract void getReplicateData(NetworkBuffer dataBuffer);

	public abstract void setReplicateData(NetworkBuffer dataBuffer);

}
