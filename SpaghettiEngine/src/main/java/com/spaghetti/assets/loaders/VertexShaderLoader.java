package com.spaghetti.assets.loaders;

import com.spaghetti.assets.AssetLoader;
import com.spaghetti.render.VertexShader;
import com.spaghetti.utils.ResourceLoader;

public class VertexShaderLoader implements AssetLoader<VertexShader> {

	@Override
	public VertexShader instantiate() {
		return new VertexShader();
	}

	@Override
	public String[] getDependencies(String[] args) {
		return null;
	}

	@Override
	public Object[] load(String[] args) throws Throwable {
		String shader_source = ResourceLoader.loadText(args[0]);
		return new Object[] { shader_source };
	}

	@Override
	public String[] getDefaultArgs() {
		return new String[] {"/internal/default.vs"};
	}

}
