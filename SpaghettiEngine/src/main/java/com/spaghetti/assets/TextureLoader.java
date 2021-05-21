package com.spaghetti.assets;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import com.spaghetti.interfaces.AssetLoader;
import com.spaghetti.render.Texture;
import com.spaghetti.utils.ResourceLoader;
import com.spaghetti.utils.Utils;

public class TextureLoader implements AssetLoader {

	@Override
	public void initializeAsset(SheetEntry asset) {
		asset.asset = new Texture();
	}

	@Override
	public String[] provideDependencies(SheetEntry asset) {
		return null;
	}

	@Override
	public Object[] loadAsset(SheetEntry asset) throws Throwable {
		BufferedImage image = ResourceLoader.loadImage(asset.args[0]);
		ByteBuffer image_data = Utils.parseImage(image);

		int width = image.getWidth();
		int height = image.getHeight();

		int type = Texture.COLOR;
		int mode = Texture.LINEAR;
		if (asset.args.length >= 2) {
			String mode_str = asset.args[1];
			switch (mode_str.toLowerCase()) {
			case "linear":
				mode = Texture.LINEAR;
				break;
			case "nearest":
				mode = Texture.NEAREST;
				break;
			}
		}

		return new Object[] { image_data, width, height, type, mode };
	}

	@Override
	public Object[] provideDefault(SheetEntry asset) {
		// TODO Auto-generated method stub
		return null;
	}

}
