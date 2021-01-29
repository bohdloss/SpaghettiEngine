package com.spaghettiengine.assets;

import java.util.HashMap;

import com.spaghettiengine.render.Material;
import com.spaghettiengine.render.Model;
import com.spaghettiengine.render.RenderObject;
import com.spaghettiengine.render.Shader;
import com.spaghettiengine.render.ShaderProgram;
import com.spaghettiengine.render.Texture;
import com.spaghettiengine.utils.Logger;

import static com.spaghettiengine.assets.AssetType.*;

public final class AssetSheet {

	private AssetManager owner;
	
	protected AssetSheet(AssetManager owner) {
		this.owner = owner;
	}
	
	// Store sheet entries here
	public HashMap<String, SheetEntry> sheet;
	private static final String append = ""
			+ "shader defaultVS /internal/default.vs vertex\n"
			+ "shader defaultFS /internal/default.fs fragment\n"
			+ "shaderprogram defaultSP defaultVS defaultFS\n"
			+ "shader rendererVS /internal/renderer.vs vertex\n"
			+ "shader rendererFS /internal/renderer.fs fragment\n"
			+ "shaderprogram rendererSP rendererVS rendererFS\n"
			+ "model square /internal/square.obj\n"
			+ "texture defaultTXT /internal/default.png\n"
			+ "material defaultMAT defaultTXT defaultSP";

	public void clear() {
		sheet.clear();
	}

	public void loadAssetSheet(String sheetSource) throws ClassNotFoundException {
		
		sheetSource = append + "\n" + sheetSource;
		
		HashMap<String, SheetEntry> result = new HashMap<>();

		// Split into lines
		String[] lines = sheetSource.split("\\R");

		for (String line : lines) {

			// If the line is empty or begins with // then ignore it
			String trim = line.trim();
			if (trim.equals("") || trim.startsWith("//")) {
				continue;
			}

			// Split into tokens
			String[] words = line.split(" ");

			// The first token defines the asset type
			// The second the name
			// The third one and following ones are the actual paths and parameters
			String type = words[0];
			String name = words[1];
			String[] args = new String[words.length - 2];
			for (int i = 2; i < words.length; i++) {
				args[i - 2] = words[i];
			}

			// Cannot have one asset override another without
			// at least notifying the user

			boolean contains = false;
			SheetEntry containsT = null;

			if (result.get(name) != null) {
				contains = true;
				containsT = result.get(name);
			}

			if (contains) {
				Logger.warning(owner.source,
						"Asset (type: " + type + " name: " + name + ") is overriding previously defined asset (type: "
								+ containsT.customType + " name: " + containsT.name + ")");
			}

			SheetEntry entry = new SheetEntry();
			entry.name = name;

			switch (type.toLowerCase()) {
			case MODEL:

				// Model location

				entry.location = args[0];
				entry.customType = _PREFIX + _MODEL;

				break;
			case SHADER:

				// Shader location and type

				entry.location = args[0];
				entry.args = new String[1];

				switch (args[1]) {
				case "vertex":
					entry.args[0] = "" + Shader.VERTEX_SHADER;
					break;
				case "fragment":
					entry.args[0] = "" + Shader.FRAGMENT_SHADER;
					break;
				case "geometry":
					entry.args[0] = "" + Shader.GEOMETRY_SHADER;
					break;
				case "tess_control":
					entry.args[0] = "" + Shader.TESS_CONTROL_SHADER;
					break;
				case "tess_evaluation":
					entry.args[0] = "" + Shader.TESS_EVALUATION_SHADER;
					break;
				default:
					throw new IllegalArgumentException("Invalid shader type");
				}

				entry.customType = _PREFIX + _SHADER;

				break;
			case SHADERPROGRAM:

				// List of names of shader objects

				entry.args = args;
				entry.customType = _PREFIX + _SHADERPROGRAM;

				break;
			case TEXTURE:

				// Texture location

				entry.location = args[0];
				entry.customType = _PREFIX + _TEXTURE;

				break;
			case MATERIAL:

				// Texture name and shader program name

				entry.args = args;
				entry.customType = _PREFIX + _MATERIAL;

				break;
			default:

				// Determine the validity of the custom class type

				Class<?> ctype = Class.forName(type);

				entry.isCustom = true;
				entry.customType = ctype.getName();

				boolean valid = false;

				if (Model.class.isAssignableFrom(ctype)) {
					valid = true;
				} else if (Shader.class.isAssignableFrom(ctype)) {
					valid = true;
				} else if (ShaderProgram.class.isAssignableFrom(ctype)) {
					valid = true;
				} else if (Texture.class.isAssignableFrom(ctype)) {
					valid = true;
				} else if (Material.class.isAssignableFrom(ctype)) {
					valid = true;
				} else if (RenderObject.class.isAssignableFrom(ctype)) {
					valid = true;
				}

				if (!valid) {
					throw new IllegalArgumentException("Invalid custom type");
				}

				// Save the list of arguments

				entry.args = args;

			}

			result.put(name, entry);

		}

		sheet = result;
	}

}
