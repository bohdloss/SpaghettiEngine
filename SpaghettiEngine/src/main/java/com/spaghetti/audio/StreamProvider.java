package com.spaghetti.audio;

import java.io.InputStream;

public interface StreamProvider {

	public InputStream provideStream() throws Throwable;

}
