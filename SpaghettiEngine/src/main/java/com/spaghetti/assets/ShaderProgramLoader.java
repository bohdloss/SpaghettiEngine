package com.spaghetti.assets;

import com.spaghetti.interfaces.AssetLoader;
import com.spaghetti.render.ShaderProgram;

public class ShaderProgramLoader implements AssetLoader {

	@Override
	public void initializeAsset(SheetEntry asset) {
		asset.asset = new ShaderProgram();
	}

	@Override
	public String[] provideDependencies(SheetEntry asset) {
		return asset.args;
	}

	@Override
	public Object[] loadAsset(SheetEntry asset) throws Throwable {
		Object[] shaders = new Object[asset.args.length];
		for (int i = 0; i < shaders.length; i++) {
			shaders[i] = asset.owner.shader(asset.args[i]);
		}
		return shaders;
	}

	@Override
	public Object[] provideDefault(SheetEntry asset) {
		// TODO Auto-generated method stub
		return null;
	}

}
