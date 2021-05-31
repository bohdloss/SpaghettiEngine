package com.spaghetti.interfaces;

import java.io.InputStream;

public interface StreamProvider {

	public InputStream provideStream() throws Throwable;
	
}
