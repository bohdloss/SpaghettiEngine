package com.spaghetti.assets.loaders;

import java.nio.ByteBuffer;

import com.spaghetti.assets.AssetLoader;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;

import com.spaghetti.render.Texture;
import com.spaghetti.utils.ResourceLoader;

public class TextureLoader implements AssetLoader<Texture> {

	@Override
	public Texture instantiate() {
		return new Texture();
	}

	@Override
	public String[] getDependencies(String[] args) {
		return null;
	}

	@Override
	public Object[] load(String[] args) throws Throwable {
		ByteBuffer buffer = MemoryUtil.memAlloc(ResourceLoader.getFileSize(args[0]));
		ResourceLoader.loadBinaryToBuffer(args[0], buffer);
		buffer.clear();

		int[] width = new int[1];
		int[] height = new int[1];
		int[] components = new int[1];

		ByteBuffer image_data = STBImage.stbi_load_from_memory(buffer, width, height, components, 4);
		MemoryUtil.memFree(buffer);
		ByteBuffer image_data_copy = MemoryUtil.memAlloc(image_data.capacity()); // Asset's responsibility to dispose of this
		MemoryUtil.memCopy(image_data, image_data_copy);

		STBImage.stbi_image_free(image_data);

		int type = Texture.COLOR;
		int mode = Texture.LINEAR;
		if (args.length >= 2) {
			String mode_str = args[1];
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
	public String[] getDefaultArgs() {
		return new String[] {"/internal/default.png", "nearest"};
	}

}
