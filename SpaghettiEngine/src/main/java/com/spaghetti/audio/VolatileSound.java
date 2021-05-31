package com.spaghetti.audio;

import java.nio.ByteBuffer;

import org.lwjgl.openal.AL11;

import com.spaghetti.utils.Utils;

public class VolatileSound extends StaticSound {
	
	public void copyData() {
		if(!valid()) {
			return;
		}
		AL11.alBufferData(id, format, data, frequency);
		Utils.alError();
	}

	@Override
	protected void load0() {
		id = AL11.alGenBuffers();
		Utils.alError();
	}
	
	public void setDataBuffer(ByteBuffer buffer) {
		this.data = buffer;
	}

}
