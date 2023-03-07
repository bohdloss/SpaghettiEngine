package com.spaghetti.assets.loaders;

import com.spaghetti.assets.AssetLoader;
import com.spaghetti.assets.AssetManager;
import com.spaghetti.core.Game;
import com.spaghetti.render.ShaderProgram;

public class ShaderProgramLoader implements AssetLoader<ShaderProgram> {

	@Override
	public ShaderProgram instantiate() {
		return new ShaderProgram();
	}

	@Override
	public String[] getDependencies(String[] args) {
		return args;
	}

	@Override
	public Object[] load(String[] args) throws Throwable {
		Object[] shaders = new Object[args.length];
		AssetManager manager = Game.getInstance().getAssetManager();
		for (int i = 0; i < shaders.length; i++) {
			shaders[i] = manager.getAndInstantlyLoadAsset(args[i]);
		}
		return shaders;
	}

	@Override
	public String[] getDefaultArgs() {
		AssetManager manager = Game.getInstance().getAssetManager();
		return new String[] {manager.getDefaultAssetName("VertexShader"), manager.getDefaultAssetName("FragmentShader")};
	}

}
