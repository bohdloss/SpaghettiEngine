package com.spaghetti.assets;

import com.spaghetti.interfaces.AssetLoader;
import com.spaghetti.render.Shader;
import com.spaghetti.utils.ResourceLoader;

public class ShaderLoader implements AssetLoader {

	@Override
	public void initializeAsset(SheetEntry asset) {
		asset.asset = new Shader();
	}

	@Override
	public String[] provideDependencies(SheetEntry asset) {
		return null;
	}

	@Override
	public Object[] loadAsset(SheetEntry asset) throws Throwable {
		String shader_source = ResourceLoader.loadText(asset.args[0]);
		String shader_type = asset.args[1];
		int gl_type;
		switch (shader_type.toLowerCase()) {
		case "vertex":
			gl_type = Shader.VERTEX_SHADER;
			break;
		case "fragment":
			gl_type = Shader.FRAGMENT_SHADER;
			break;
		case "geometry":
			gl_type = Shader.GEOMETRY_SHADER;
			break;
		case "tess_control":
			gl_type = Shader.TESS_CONTROL_SHADER;
			break;
		case "tess_evaluation":
			gl_type = Shader.TESS_EVALUATION_SHADER;
			break;
		default:
			throw new IllegalArgumentException("Unrecognized shader type " + shader_type);
		}
		return new Object[] { shader_source, gl_type };
	}

	@Override
	public Object[] provideDefault(SheetEntry asset) {
		// TODO Auto-generated method stub
		return null;
	}

}
