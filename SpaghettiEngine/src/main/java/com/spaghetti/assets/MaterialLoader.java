package com.spaghetti.assets;

import com.spaghetti.interfaces.AssetLoader;
import com.spaghetti.render.Material;
import com.spaghetti.render.ShaderProgram;
import com.spaghetti.render.Texture;

public class MaterialLoader implements AssetLoader {

	@Override
	public void initializeAsset(SheetEntry asset) {
		asset.asset = new Material();
	}

	@Override
	public String[] provideDependencies(SheetEntry asset) {
		return new String[] { asset.args[0], asset.args[1] };
	}

	@Override
	public Object[] loadAsset(SheetEntry asset) throws Throwable {
		Texture texture = asset.owner.texture(asset.args[0]);
		ShaderProgram shaderProgram = asset.owner.shaderProgram(asset.args[1]);
		return new Object[] { texture, shaderProgram };
	}

	@Override
	public Object[] provideDefault(SheetEntry asset) {
		// TODO Auto-generated method stub
		return null;
	}

}
