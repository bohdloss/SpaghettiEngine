package com.spaghettiengine.interfaces;

import com.spaghettiengine.utils.SpaghettiBuffer;

public interface Replicable {

	public abstract void getReplicateData(SpaghettiBuffer dataBuffer);

	public abstract void setReplicateData(SpaghettiBuffer dataBuffer);

}
