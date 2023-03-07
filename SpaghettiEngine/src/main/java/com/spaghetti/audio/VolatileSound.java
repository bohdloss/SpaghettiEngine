package com.spaghetti.audio;

import java.nio.ByteBuffer;

import com.spaghetti.utils.ExceptionUtil;
import org.lwjgl.openal.AL10;

public class VolatileSound extends StaticSound {

	public void copyData() {
		if (!isValid()) {
			return;
		}
		AL10.alBufferData(id, format, data, frequency);
		ExceptionUtil.alError();
	}

	@Override
	protected void load0() {
		id = AL10.alGenBuffers();
		ExceptionUtil.alError();
	}

	public void setDataBuffer(ByteBuffer buffer) {
		this.data = buffer;
	}

}
