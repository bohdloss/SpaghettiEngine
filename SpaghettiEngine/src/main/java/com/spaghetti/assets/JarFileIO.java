package com.spaghetti.assets;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryUtil;
import com.spaghetti.utils.Logger;
import com.spaghetti.utils.ResourceLoader;

public class JarFileIO {

	private static final String PREFIX = "[Assimp JAR Loader] ";

	public static synchronized AIFileIO getInstance() {
		AIFileIO inst = AIFileIO.create();
		if (inst == null || inst.address() == 0) {
			return null;
		}

		// Define file system functions
		inst.OpenProc((pFileIO, fileName, openMode) -> {
			String filename_str = MemoryUtil.memUTF8(fileName);
			final ByteBuffer raw_buffer;
			try {
				raw_buffer = BufferUtils.createByteBuffer(ResourceLoader.getFileSize(filename_str));
				ResourceLoader.loadBinaryToBuffer(filename_str, raw_buffer);
			} catch (Throwable t) {
				Logger.error(PREFIX + "Couldn't load file " + filename_str, t);
				return 0;
			}
			raw_buffer.clear();

			AIFile file_inst = AIFile.create();
			if (file_inst == null || file_inst.address() == 0) {
				return 0;
			}
			// Define file functions
			file_inst.ReadProc((pFile, pBuffer, size, count) -> {
				long rem = raw_buffer.remaining();
				long mul = size * count;
				long max = rem < mul ? rem : mul;
				MemoryUtil.memCopy(MemoryUtil.memAddress(raw_buffer), pBuffer, max);
				raw_buffer.position(raw_buffer.position() + (int) max);
				return max;
			});

			file_inst.WriteProc((pFile, pBuffer, memB, count) -> 0);

			file_inst.FlushProc(pFile -> {
				// Nothing, writing is not allowed
			});

			file_inst.FileSizeProc(pFile -> raw_buffer.limit());

			file_inst.SeekProc((pFile, offset, origin) -> {
				switch (origin) {
				case Assimp.aiOrigin_SET:
					raw_buffer.position((int) offset);
					break;
				case Assimp.aiOrigin_CUR:
					raw_buffer.position(raw_buffer.position() + (int) offset);
					break;
				case Assimp.aiOrigin_END:
					raw_buffer.position(raw_buffer.limit() + (int) offset);
					break;
				}
				return 0;
			});

			file_inst.TellProc(pFile -> raw_buffer.position());

			return file_inst.address();
		});

		inst.CloseProc((pFileIO, pFile) -> {
			AIFile file = AIFile.create(pFile);
			file.FileSizeProc().free();
			file.FlushProc().free();
			file.ReadProc().free();
			file.WriteProc().free();
			file.TellProc().free();
			file.SeekProc().free();
		});

		return inst;
	}

}
