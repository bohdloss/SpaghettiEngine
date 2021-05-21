package com.spaghetti.interfaces;

import com.spaghetti.assets.SheetEntry;

public interface AssetLoader {

	public void initializeAsset(SheetEntry asset);

	public String[] provideDependencies(SheetEntry asset);

	public Object[] loadAsset(SheetEntry asset) throws Throwable;

	public Object[] provideDefault(SheetEntry asset);

}
