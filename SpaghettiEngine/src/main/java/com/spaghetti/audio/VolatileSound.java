package com.spaghetti.audio;

import java.nio.ByteBuffer;

import org.lwjgl.openal.AL10;
import com.spaghetti.utils.Utils;

public class VolatileSound extends StaticSound {

	public void copyData() {
		if (!valid()) {
			return;
		}
		AL10.alBufferData(id, format, data, frequency);
		Utils.alError();
	}

	@Override
	protected void load0() {
		id = AL10.alGenBuffers();
		Utils.alError();
	}

	public void setDataBuffer(ByteBuffer buffer) {
		this.data = buffer;
	}

}
