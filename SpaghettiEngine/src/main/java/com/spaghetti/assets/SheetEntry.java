package com.spaghetti.assets;

public class SheetEntry {

	public String name;
	public String location;
	public String[] args;
	public boolean isCustom;
	public String customType;

	public String location() {
		return location == null ? args[0] : location;
	}

}
