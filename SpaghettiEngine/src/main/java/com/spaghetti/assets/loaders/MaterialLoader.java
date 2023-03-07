package com.spaghetti.assets.loaders;

import com.spaghetti.assets.AssetLoader;
import com.spaghetti.assets.AssetManager;
import com.spaghetti.core.Game;
import com.spaghetti.render.Material;
import com.spaghetti.render.ShaderProgram;
import com.spaghetti.render.Texture;

public class MaterialLoader implements AssetLoader<Material> {

	@Override
	public Material instantiate() {
		return new Material();
	}

	@Override
	public String[] getDependencies(String[] args) {
		return new String[] { args[0], args[1] };
	}

	@Override
	public Object[] load(String[] args) throws Throwable {
		return new Object[] { Texture.get(args[0]), ShaderProgram.get(args[1]) };
	}

	@Override
	public String[] getDefaultArgs() {
		AssetManager manager = Game.getInstance().getAssetManager();
		return new String[] {manager.getDefaultAssetName("texture"), manager.getDefaultAssetName("shaderprogram")};
	}

}
