package com.spaghettiengine.interfaces;

import java.nio.ByteBuffer;

public interface Replicable {

	public abstract void getReplicateData(ByteBuffer dataBuffer);
	public abstract void setReplicateData(ByteBuffer dataBuffer);
	
}
