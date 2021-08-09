package com.spaghetti.assets;

import java.nio.ByteBuffer;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;

import com.spaghetti.interfaces.AssetLoader;
import com.spaghetti.render.Texture;
import com.spaghetti.utils.ResourceLoader;

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
		ByteBuffer buffer = MemoryUtil.memAlloc(ResourceLoader.getFileSize(asset.args[0]));
		ResourceLoader.loadBinaryToBuffer(asset.args[0], buffer);
		buffer.clear();

		int[] width = new int[1];
		int[] height = new int[1];
		int[] components = new int[1];

		ByteBuffer image_data = STBImage.stbi_load_from_memory(buffer, width, height, components, 4);
		MemoryUtil.memFree(buffer);
		ByteBuffer image_data_copy = MemoryUtil.memAlloc(image_data.capacity()); // Asset's responsibility to dispose of
																					// this
		MemoryUtil.memCopy(image_data, image_data_copy);
		;
		STBImage.stbi_image_free(image_data);

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

		return new Object[] { image_data_copy, width[0], height[0], type, mode };
	}

	@Override
	public Object[] provideDefault(SheetEntry asset) {
		// TODO Auto-generated method stub
		return null;
	}

}
