package com.spaghetti.assets;

import java.util.HashMap;

import com.spaghetti.audio.Sound;
import com.spaghetti.core.Game;
import com.spaghetti.events.Signals;
import com.spaghetti.interfaces.AssetLoader;
import com.spaghetti.render.Material;
import com.spaghetti.render.Model;
import com.spaghetti.render.Shader;
import com.spaghetti.render.ShaderProgram;
import com.spaghetti.render.Texture;
import com.spaghetti.utils.FunctionDispatcher;
import com.spaghetti.utils.Logger;
import com.spaghetti.utils.ResourceLoader;
import com.spaghetti.utils.Utils;

/**
 * A class that manages assets and their dependencies, including loading and
 * unloading them dynamically as needed<br>
 * No function (except a few exceptions) is guaranteed to work if
 * {@link #isReady()} returns false The exceptions are
 * {@link #loadAssetSheet(String)},
 * {@link #registerAsset(String, String, String[])} and
 * {@link #registerAsset(String, String, String[], Asset)} which will set the
 * flag
 * <p>
 * The only way to set the flag to {@code false} is through
 * {@link #destroy()}<br>
 * This class is still usable (by calling the three methods listed before) after
 * {@link #destroy()}
 *
 * @author bohdloss
 */
public class AssetManager {

	/**
	 * The internal assets are defined in this hard-coded string to which the rest
	 * of the asset sheet should be appended in {@link #loadAssetSheet(String)}
	 */
	protected static final String APPEND = "" + "shader defaultVS /internal/default.vs vertex\n"
			+ "shader defaultFS /internal/default.fs fragment\n" + "shaderprogram defaultSP defaultVS defaultFS\n"
			+ "shader rendererVS /internal/renderer.vs vertex\n" + "shader rendererFS /internal/renderer.fs fragment\n"
			+ "shaderprogram rendererSP rendererVS rendererFS\n" + "model square /internal/square.obj find square\n"
			+ "texture defaultTXT /internal/default.png nearest\n" + "material defaultMAT defaultTXT defaultSP\n";

	// Data

	/**
	 * The {@link Game} instance associated with this {@link AssetManager}
	 */
	protected final Game game;
	/**
	 * The {@link HashMap} containing the loaders for the different asset types
	 */
	protected HashMap<String, AssetLoader> loaders = new HashMap<>();

	/**
	 * The {@link HashMap} containing the asset entries
	 */
	protected HashMap<String, SheetEntry> assets = new HashMap<>();

	/**
	 * The ready flag
	 */
	protected boolean ready;

	/**
	 * Initializes the AssetManager with a link to the {@code game} instance it is
	 * to be associated with
	 * <p>
	 * Users that intend to modify the behavior of the asset manager in their game
	 * must include a constructor with the same parameters as this one
	 * <p>
	 * This class is not meant to be used in a stand alone way, therefore it has not
	 * been tested that way and no guarantee is to be made in that case neither by
	 * this class nor it's subclasses
	 * <p>
	 * Refer to the documentation of {@link #GameBuilder}
	 *
	 * @param game The game this asset manager is to be associated with
	 */
	public AssetManager(Game game) {
		this.game = game;

		registerAssetLoader("model", new ModelLoader());
		registerAssetLoader("shader", new ShaderLoader());
		registerAssetLoader("shaderprogram", new ShaderProgramLoader());
		registerAssetLoader("texture", new TextureLoader());
		registerAssetLoader("sound", new SoundLoader());
		registerAssetLoader("material", new MaterialLoader());
		registerAssetLoader("music", new MusicLoader());

		// Register shutdown hook
		game.getEventDispatcher().registerSignalHandler((isClient, signal) -> {
			if (signal == Signals.SIGSTOP) {
				if (game.isHeadless()) {
					destroy();
				} else {
					game.getRendererDispatcher().quickQueue(() -> {
						destroy();
						return null;
					});
				}
			}
		});
	}

	/**
	 * This method attempts to fill an asset with the data associated with it, and
	 * if it fails, it tries to fill it with its default data. If both operations
	 * fail, it must throw a {@link RuntimeException}
	 *
	 * @param asset The asset to fill
	 */
	protected void fillAsset(SheetEntry asset) {
		// Get asset loader
		AssetLoader loader = getAssetLoader(asset.type);
		if (loader == null) {
			throw new RuntimeException("No loader registered for asset type " + asset.type);
		}

		// Attempt to load data
		Object[] data;
		try {
			data = loader.loadAsset(asset);
			// Attempt to setData in order to check for errors
			asset.asset.setData(data);
			if (!asset.asset.isFilled()) {
				throw new IllegalStateException("Asset didn't accept arguments provided by loader");
			}
		} catch (Throwable t0) {
			Logger.error("Couldn't load " + asset.type + " " + asset.name + ", trying to use default version instead",
					t0);
			try {
				data = loader.provideDefault(asset);
				// Doing another check
				asset.asset.setData(data);
				if (!asset.asset.isFilled()) {
					throw new IllegalStateException(
							"Attempt to use default version of asset type " + asset.type + " failed for " + asset.name);
				}
			} catch (Throwable t1) {
				throw new RuntimeException("Every attempt to load " + asset.type + " " + asset.name + " failed", t1);
			}
		}
	}

	protected void loadNative(SheetEntry asset) {
		asset.asset.load();
		asset.loading = false;
	}

	protected void unloadNative(SheetEntry asset) {
		asset.asset.unload();
		asset.unloading = false;
	}

	// Initialization / finalization of this object

	/**
	 * Imports an asset listing from the file at the specified relative location in
	 * the jar file
	 * <p>
	 * On success, this method must call destroy first, and then register all the
	 * assets, and after that the {@code ready} flag must be set
	 *
	 * @param sheetLocation The relative location to read the file from
	 * @return Whether or not the operation succeeded
	 */
	public boolean loadAssetSheet(String sheetLocation) {
		try {
			// Load the sheet file
			String sheetSource = ResourceLoader.loadText(sheetLocation);

			// Prepare for parsing
			HashMap<String, SheetEntry> map = new HashMap<>();

			// Split into lines
			sheetSource = APPEND + sheetSource;
			String[] lines = sheetSource.split("\\R");
			for (String line : lines) {
				// Ignore empty lines or comments
				String line_trimmed = line.trim();
				if (line_trimmed.equals("") || line_trimmed.startsWith("//")) {
					continue;
				}

				// Split into space separated tokens
				String[] tokens = line_trimmed.split(" ");

				// Fill entry information
				SheetEntry entry = new SheetEntry(this);
				entry.type = tokens[0];
				entry.name = tokens[1];
				entry.args = new String[tokens.length - 2];
				for (int i = 0; i < entry.args.length; i++) {
					entry.args[i] = tokens[i + 2];
				}

				// Save entry
				map.put(entry.name, entry);
			}

			// Destroy in case an asset sheet is already loaded
			destroy();

			// Finally apply the sheet
			assets.clear();
			assets.putAll(map);

			// Initialize all dummy assets
			for (SheetEntry entry : assets.values()) {
				getAssetLoader(entry.type).initializeAsset(entry);
				entry.asset.setName(entry.name);
			}

			// We're ready!
			ready = true;
			Logger.loading(game, "Successfully loaded asset sheet " + sheetLocation);
			return true;
		} catch (Throwable t) {
			Logger.error(game, "Could not load asset sheet " + sheetLocation, t);
			return false;
		}
	}

	/**
	 * Resets this asset manager to its state upon creation by unloading all assets
	 * with {@link #unloadAll()} and unregistering all imported assets, then
	 * changing the {@code ready} flag to {@code false}
	 */
	public void destroy() {
		if (!ready) {
			return;
		}
		unloadAll();
		assets.clear();
		ready = false;
	}

	// Load / Unload methods

	/**
	 * Custom functional interface to specify the function used to load an asset
	 * when invoking {@link AssetManager#meetDependencies(SheetEntry, LoadFunction)}
	 */
	private static interface LoadFunction {
		/**
		 * The only function provided by this interface is similar to
		 * {@link AssetManager}'s load operations so as to allow them being passed as
		 * lambda and converted into a {@link LoadFunction} easily
		 *
		 * @param name The name of the asset to load
		 * @return The value returned by the load operation, representing failure or
		 *         success
		 */
		public abstract boolean load(String name);
	}

	/**
	 * This methods retrieves and possibly loads the dependencies of a given
	 * {@code asset} using the provided {@code loadFunc} when loading is considered
	 * necessary
	 *
	 * @param asset    The asset whose dependencies to load
	 * @param loadFunc The function to load the dependencies with
	 * @return Whether or not at least one dependency had to be loaded
	 */
	private boolean meetDependencies(SheetEntry asset, LoadFunction loadFunc) {
		// Retrieve dependencies
		String[] dependencies = null;
		try {
			dependencies = getAssetLoader(asset.type).provideDependencies(asset);
		} catch (Throwable t) {
		}

		// Load with specified method
		boolean found = false;
		if (dependencies != null) {

			// Iterate through dependencies
			for (String assetName : dependencies) {

				// Retrieve the object
				SheetEntry dependency = assets.get(assetName);

				// Perform some checks
				if (dependency == null) {
					Logger.warning(asset.name + " depends on undefined dependency " + assetName);
					return true;
				} else if (!dependency.asset.isLoaded()) {
					found = true;
					if (loadFunc != null) {
						Logger.loading("[meetDependencies] Loading dependency " + assetName + " of " + asset.name);
						loadFunc.load(assetName);
					}
				}
			}
		}
		return found;
	}

	/**
	 * Loads the asset denoted by the given name in another thread
	 * <p>
	 * Any dependency will be loaded right away instead, to avoid synchronization
	 * issues, using {@link #loadAssetNow(String)}
	 * <p>
	 * No guarantee is made as to when or if the asset will succeed or fail to load
	 *
	 * @param name The name of the asset to load
	 * @return Returns {@code true} if the asset is being loaded, {@code false} if
	 *         the request has been discarded
	 */
	public boolean loadAssetLazy(String name) {
		Logger.debug("[loadAssetLazy] " + name);
		if (!checkAsset(name) || game.isHeadless()) {
			return false;
		}
		// Check if the asset is ready to be processed
		SheetEntry asset = assets.get(name);
		synchronized (asset) {
			// The asset is busy but not loading, forward it to the queue function
			if (asset.asset.isLoaded() || asset.loading || asset.isBusy()) {
				return false;
			}

			// Instantly load dependencies
			meetDependencies(asset, this::loadAssetNow);

			// Initialize load thread
			Thread loadThread = new Thread() {
				@Override
				public void run() {
					if (!ready) {
						return;
					}

					try {
						// Lock the asset
						synchronized (asset) {
							// Fill asset
							fillAsset(asset);
						}

						// Queue native loading
						FunctionDispatcher dispatcher = game.getRendererDispatcher();
						long func = dispatcher.queue(() -> {
							loadNative(asset);
							return null;
						});
						dispatcher.waitReturnValue(func);
						Logger.info("[loadAssetLazy] Loaded " + name);
					} catch (Throwable t) {
						Logger.error("[loadAssetLazy] Error loading " + asset.type + " " + asset.name, t);
					}

					// Unregister this thread upon completion
					game.unregisterThread(this);
				}
			};

			// Register thread and start it
			loadThread.setName("LOADER (" + asset.type + ", " + asset.name + ")");
			game.registerThread(loadThread);
			loadThread.start();
		}

		return true;
	}

	/**
	 * Loads the asset denoted by the given name and then returns
	 * <p>
	 * Any dependency will be loaded recursively
	 * <p>
	 * It is guaranteed that when this function returns, the asset will have either
	 * succeeded or failed in loading
	 *
	 * @param name The name of the asset to load
	 * @return Returns {@code true} if the asset has been successfully loaded,
	 *         {@code false} otherwise
	 */
	public boolean loadAssetNow(String name) {
		Logger.loading("[loadAssetNow] " + name);
		if (!checkAsset(name) || game.isHeadless()) {
			return false;
		}

		// Check if the asset is ready to be processed
		SheetEntry asset = assets.get(name);
		synchronized (asset) {
			if (asset.asset.isLoaded() || asset.loading) {
				return true;
			}

			// The asset is busy doing something else, wait for it to be ready
			while (asset.isBusy()) {
				Utils.sleep(1);
			}

			meetDependencies(asset, this::loadAssetNow);

			// Begin loading process
			asset.loading = true;

			try {
				// Fill asset
				fillAsset(asset);

				// Queue native loading
				FunctionDispatcher dispatcher = game.getRendererDispatcher();
				long func = dispatcher.queue(() -> {
					loadNative(asset);
					return null;
				});
				dispatcher.waitReturnValue(func);
				Logger.loading("[loadAssetNow] Asset loaded " + name);
			} catch (Throwable t) {
				Logger.error("[loadAssetNow] Error loading " + asset.type + " " + asset.name, t);
				return false;
			}
		}

		return true;
	}

	/**
	 * Loads the asset denoted by the given name and then returns, starting from the
	 * assumption that the current thread is the RENDERER thread
	 * <p>
	 * Any dependency will be loaded recursively
	 * <p>
	 * If the current thread isn't the RENDERER thread, the function will return
	 * without doing anything
	 * <p>
	 * It is guaranteed that when this function returns, the asset will have either
	 * succeeded or failed in loading, unless the current thread is not the RENDERER
	 * thread
	 *
	 * @param name The name of the asset to load
	 * @return Returns {@code true} if the asset has been successfully loaded,
	 *         {@code false} otherwise or if the current thread is not the RENDERER
	 *         thread
	 */
	public boolean loadAssetDirect(String name) {
		Logger.loading("[loadAssetDirect] " + name);

		if ((Thread.currentThread() != game.getRenderer()) || !checkAsset(name) || game.isHeadless()) {
			return false;
		}

		// Check if the asset is ready to be processed
		SheetEntry asset = assets.get(name);
		synchronized (asset) {
			if (asset.asset.isLoaded() || asset.loading) {
				return true;
			}

			// The asset is busy doing something else, wait for it to be ready
			while (asset.isBusy()) {
				Utils.sleep(1);
			}

			meetDependencies(asset, this::loadAssetDirect);

			// Begin loading process
			asset.loading = true;

			try {
				// Fill asset
				fillAsset(asset);

				// Perform native loading directly
				loadNative(asset);
				Logger.loading("[loadAssetDirect] Asset loaded " + name);
			} catch (Throwable t) {
				Logger.error("[loadAssetDirect] Error loading " + asset.type + " " + asset.name, t);
				return false;
			}
		}

		return true;
	}

	/**
	 * Loads the asset denoted by the given name by choosing a function
	 * <p>
	 * The function chosen will be influenced by the current thread and by the lazy
	 * parameter
	 * <p>
	 * If the lazy parameter is true, {@link #loadAssetLazy(String)} will be called
	 * with {@code name} as the parameter<br>
	 * Else<br>
	 * If the current thread is the RENDERER thread,
	 * {@link #loadAssetDirect(String)} will be called with {@code name} as the
	 * parameter<br>
	 * Else {@link #loadAssetNow(String)} will be called with {@code name} as the
	 * parameter
	 *
	 * @param name The name of the asset to load
	 * @param lazy Whether or not the user prefers a lazy load method
	 * @return Returns the value returned by the chosen function
	 */
	public boolean loadAssetAuto(String name, boolean lazy) {
		if (lazy) {
			return loadAssetLazy(name);
		} else {
			if (Thread.currentThread() == game.getRenderer()) {
				return loadAssetDirect(name);
			} else {
				return loadAssetNow(name);
			}
		}
	}

	/**
	 * Loads every asset currently registered and not already loaded
	 * <p>
	 * When this method returns, an attempt will be made to load each asset
	 * <p>
	 * The implementation of this method makes use of
	 * {@link #loadAssetAuto(String, boolean)}
	 *
	 * @param lazy The {@code lazy} parameter that will be forwarded to
	 *             {@link #loadAssetAuto(String, boolean)}
	 * @return This function performs a bitwise AND between each call to
	 *         {@link #loadAssetAuto(String, boolean)}, so if any of the calls to
	 *         said function fails, {@code false} will be returned at the end of the
	 *         process
	 */
	public boolean loadAll(boolean lazy) {
		if (!ready || game.isHeadless()) {
			return false;
		}

		boolean value = true;
		for (SheetEntry entry : assets.values()) {
			value &= loadAssetAuto(entry.name, lazy);
		}
		return value;
	}

	/**
	 * Unloads the asset denoted by the given name and then returns
	 * <p>
	 * Any asset that depends on this asset will be unloaded recursively
	 * <p>
	 * It is guaranteed that when this function returns, the asset will have either
	 * succeeded or failed in unloading
	 * <p>
	 * If the unloading process fails, the asset may still be flagged as unloaded,
	 * as defined by {@link Asset#unload()}
	 *
	 * @param name The name of the asset to unload
	 * @return Returns {@code true} if the asset has been successfully unloaded,
	 *         {@code false} otherwise
	 */
	public boolean unloadAssetNow(String name) {
		if (!checkAsset(name) || game.isHeadless()) {
			return false;
		}

		// Check if the asset is ready to be processed
		SheetEntry asset = assets.get(name);
		synchronized (asset) {
			if (asset.asset.isUnloaded() || asset.unloading) {
				return true;
			}

			// The asset is busy, wait for it to be ready
			while (asset.isBusy()) {
				Utils.sleep(1);
			}

			// Begin unloading process
			asset.unloading = true;

			try {
				// Queue native unloading
				FunctionDispatcher dispatcher = game.getRendererDispatcher();
				long func = dispatcher.queue(() -> {
					unloadNative(asset);
					return null;
				});

				dispatcher.waitReturnValue(func);
			} catch (Throwable t) {
				Logger.error("Error unloading " + asset.type + " " + asset.name, t);
				return false;
			}
		}

		return true;
	}

	/**
	 * Unloads the asset denoted by the given name and then returns
	 * <p>
	 * Any asset that depends on this asset will be unloaded recursively
	 * <p>
	 * If the current thread isn't the RENDERER thread, the function will return
	 * without doing anything
	 * <p>
	 * It is guaranteed that when this function returns, the asset will have either
	 * succeeded or failed in unloading
	 * <p>
	 * If the unloading process fails, the asset may still be flagged as unloaded,
	 * as defined by {@link Asset#unload()}
	 *
	 * @param name The name of the asset to unload
	 * @return Returns {@code true} if the asset has been successfully unloaded,
	 *         {@code false} otherwise or if the current thread is not the RENDERER
	 *         thread
	 */
	public boolean unloadAssetDirect(String name) {
		if ((Thread.currentThread() != game.getRenderer()) || !checkAsset(name) || game.isHeadless()) {
			return false;
		}

		// Check if the asset is ready to be processed
		SheetEntry asset = assets.get(name);
		synchronized (asset) {
			if (asset.asset.isUnloaded() || asset.unloading) {
				return true;
			}

			// The asset is busy, wait for it to be ready
			while (asset.isBusy()) {
				Utils.sleep(1);
			}

			// Begin unloading process
			asset.unloading = true;

			try {
				// Perform native unloading directly
				unloadNative(asset);
			} catch (Throwable t) {
				Logger.error("Error unloading " + asset.type + " " + asset.name, t);
				return false;
			}
		}

		return true;
	}

	/**
	 * Unloads the asset denoted by the given name by choosing a function
	 * <p>
	 * The function chosen will be influenced by the current thread
	 * <p>
	 * If the current thread is the RENDERER thread,
	 * {@link #unloadAssetDirect(String)} will be called with {@code name} as the
	 * parameter<br>
	 * Else {@link #unloadAssetNow(String)} will be called with {@code name} as the
	 * parameter
	 *
	 * @param name The name of the asset to unload
	 * @return Returns the value returned by the chosen function
	 */
	public boolean unloadAssetAuto(String name) {
		if (Thread.currentThread() == game.getRenderer()) {
			return unloadAssetDirect(name);
		} else {
			return unloadAssetNow(name);
		}
	}

	/**
	 * Unloads every asset currently registered and not already loaded
	 * <p>
	 * When this method returns, an attempt will be made to unload each asset
	 * <p>
	 * The implementation of this method makes use of
	 * {@link #unloadAssetAuto(String)}
	 *
	 * @return This function performs a bitwise AND between each call to
	 *         {@link #unloadAssetAuto(String)}, so if any of the calls to said
	 *         function fails, {@code false} will be returned at the end of the
	 *         process
	 */
	public boolean unloadAll() {
		if (!ready || game.isHeadless()) {
			return false;
		}
		boolean value = true;
		boolean deletedOne = assets.size() > 0;
		for (SheetEntry entry : assets.values()) {
			value &= unloadAssetAuto(entry.name);
		}
		if (deletedOne) {
			Logger.info(game, "Finished unloading assets");
		}
		return value;
	}

	// Utility methods

	/**
	 * Retrieves the type of the asset denoted by {@code name} and returns it
	 *
	 * @param name The name of the asset
	 * @return The type of the asset
	 * @throws NullPointerException If the asset is not present
	 */
	public String getAssetType(String name) {
		if (!ready) {
			return null;
		}
		return assets.get(name).type;
	}

	/**
	 * Registers a custom {@code loader} for the given {@code assetType}<br>
	 * This method can override existing loaders and default loaders
	 *
	 * @param assetType The asset type to register the loader for
	 * @param loader    The {@link AssetLoader}
	 * @throws IllegalArgumentException If either {@code assetType} or
	 *                                  {@code loader} are null
	 */
	public void registerAssetLoader(String assetType, AssetLoader loader) {
		if (loader == null || assetType == null) {
			throw new IllegalArgumentException();
		}
		loaders.put(assetType, loader);
	}

	/**
	 * Unregisters a loader for the given {@code assetType}<br>
	 * This method can unregister existing loaders and default loaders
	 *
	 * @param assetType The asset type to unregister the loader for
	 * @throws IllegalArgumentException If {@code assetType} is null
	 */
	public void unregisterAssetLoader(String assetType) {
		if (assetType == null) {
			throw new IllegalArgumentException();
		}
		loaders.remove(assetType);
	}

	/**
	 * Registers an asset entry just like it was imported with
	 * {@link #loadAssetSheet(String)}<br>
	 * This method will use the loader associated with the provided
	 * {@code assetType} to initialize an instance of the asset<br>
	 * You can use this method to override existing assets, even internal ones
	 * <p>
	 * The ready flag will be set after this operation succeeds
	 *
	 * @param assetName      The name of the asset to register
	 * @param assetType      The type of the asset
	 * @param assetArguments The arguments that will be used when loading it
	 * @throws IllegalArgumentException If {@code assetName}, {@code assetType},
	 *                                  {@code assetArguments} or any of the entries
	 *                                  in {@code assetArguments} is null
	 */
	public void registerAsset(String assetName, String assetType, String[] assetArguments) {
		// Sanity checks
		if (assetName == null || assetType == null || assetArguments == null) {
			throw new IllegalArgumentException();
		}

		for (String assetArg : assetArguments) {
			if (assetArg == null) {
				throw new IllegalArgumentException();
			}
		}

		// Initialize asset struct
		SheetEntry asset = new SheetEntry(this);
		asset.name = assetName;
		asset.type = assetType;
		asset.args = assetArguments;

		// Initialize asset object
		getAssetLoader(assetType).initializeAsset(asset);
		asset.asset.setName(assetName);

		// Add to map
		assets.put(assetName, asset);

		ready = true;
	}

	/**
	 * Registers an asset entry just like it was imported with
	 * {@link #loadAssetSheet(String)}<br>
	 * This method will use the provided {@code template} instead of allocating a
	 * new asset of that type You can use this method to override existing assets,
	 * even internal ones
	 * <p>
	 * The ready flag will be set after this operation succeeds
	 *
	 * @param assetName      The name of the asset to register
	 * @param assetType      The type of the asset
	 * @param assetArguments The arguments that will be used when loading it
	 * @throws IllegalArgumentException If {@code assetName}, {@code assetType},
	 *                                  {@code assetArguments}, any of the entries
	 *                                  in {@code assetArguments} or
	 *                                  {@code template} is null<br>
	 *                                  The exception will also be thrown if
	 *                                  {@code template} already has a name
	 *                                  associated with it AND it differs from
	 *                                  {@code assetName}
	 */
	public void registerAsset(String assetName, String assetType, String[] assetArguments, Asset template) {
		// Sanity checks
		if (assetName == null || assetType == null || assetArguments == null
				|| (template.getName() != null && !template.getName().equals(assetName))) {
			throw new IllegalArgumentException();
		}

		for (String assetArg : assetArguments) {
			if (assetArg == null) {
				throw new IllegalArgumentException();
			}
		}

		// Initialize asset struct
		SheetEntry asset = new SheetEntry(this);
		asset.name = assetName;
		asset.type = assetType;
		asset.args = assetArguments;
		asset.asset = template;
		asset.asset.setName(assetName);

		// Add to map
		assets.put(assetName, asset);
	}

	/**
	 * Unregisters an asset entry<br>
	 * You can use this method to unregister existing assets, even internal ones
	 * (though it is not recommended)
	 *
	 * @param assetName The name of the asset to unregister
	 * @throws IllegalArgumentException If {@code assetName} is null
	 */
	public void unregisterAsset(String assetName) {
		if (assetName == null) {
			throw new IllegalArgumentException("");
		}

		assets.remove(assetName);
	}

	/**
	 * This method retrieves and returns the {@link AssetLoader} registered for
	 * {@code  assetType}
	 *
	 * @param assetType The asset type to retrieve the loader for
	 * @return The {@link AssetLoader}
	 * @throws IllegalArgumentException If {@code assetType} is null
	 * @throws NullPointerException     If the asset is not present
	 */
	public AssetLoader getAssetLoader(String assetType) {
		if (assetType == null) {
			throw new IllegalArgumentException();
		}
		return loaders.get(assetType);
	}

	/**
	 * Checks for the ready flag and if the asset denoted by {@code name} exists and
	 * returns {@code true} if both checks succeed
	 *
	 * @param name The name of the asset to perform the check on
	 * @return {@code true} if the checks succeeded, {@code false} otherwise
	 */
	protected boolean checkAsset(String name) {
		if (!ready) {
			Logger.warning("Can't check asset " + name + ", asset manager is not ready");
			return false;
		}
		if (assets.get(name) == null) {
			Logger.warning("Non existant asset requested: " + name);
			return false;
		}
		return true;
	}

	// Lazy-loading getters

	/**
	 * Requests the custom asset denoted by {@code name}, check if it exists and
	 * returns null if it doesn't<br>
	 * Otherwise checks if the asset is loaded, and if not it is lazy loaded, then
	 * it is returned in both cases
	 *
	 * @param name The name of the asset
	 * @return The {@link Asset}
	 */
	public Asset custom(String name) {
		if (!checkAsset(name)) {
			return null;
		}
		SheetEntry asset = assets.get(name);
		Asset ret = asset.asset;
		if (!ret.valid()) {
			loadAssetLazy(ret.getName());
		}
		return ret;
	}

	/**
	 * Requests the model denoted by {@code name}, check if it exists and returns
	 * null if it doesn't<br>
	 * Otherwise checks if the asset is loaded, and if not it is lazy loaded, then
	 * it is returned in both cases
	 *
	 * @param name The name of the asset
	 * @return The {@link Asset}
	 * @throws ClassCastException If the requested asset is not actually of type
	 *                            model
	 */
	public Model model(String name) {
		return (Model) custom(name);
	}

	/**
	 * Requests the shader denoted by {@code name}, check if it exists and returns
	 * null if it doesn't<br>
	 * Otherwise checks if the asset is loaded, and if not it is lazy loaded, then
	 * it is returned in both cases
	 *
	 * @param name The name of the asset
	 * @return The {@link Asset}
	 * @throws ClassCastException If the requested asset is not actually of type
	 *                            shader
	 */
	public Shader shader(String name) {
		return (Shader) custom(name);
	}

	/**
	 * Requests the shader program denoted by {@code name}, check if it exists and
	 * returns null if it doesn't<br>
	 * Otherwise checks if the asset is loaded, and if not it is lazy loaded, then
	 * it is returned in both cases
	 *
	 * @param name The name of the asset
	 * @return The {@link Asset}
	 * @throws ClassCastException If the requested asset is not actually of type
	 *                            shader program
	 */
	public ShaderProgram shaderProgram(String name) {
		return (ShaderProgram) custom(name);
	}

	/**
	 * Requests the texture denoted by {@code name}, check if it exists and returns
	 * null if it doesn't<br>
	 * Otherwise checks if the asset is loaded, and if not it is lazy loaded, then
	 * it is returned in both cases
	 *
	 * @param name The name of the asset
	 * @return The {@link Asset}
	 * @throws ClassCastException If the requested asset is not actually of type
	 *                            texture
	 */
	public Texture texture(String name) {
		return (Texture) custom(name);
	}

	/**
	 * Requests the material denoted by {@code name}, check if it exists and returns
	 * null if it doesn't<br>
	 * Otherwise checks if the asset is loaded, and if not it is lazy loaded, then
	 * it is returned in both cases
	 *
	 * @param name The name of the asset
	 * @return The {@link Asset}
	 * @throws ClassCastException If the requested asset is not actually of type
	 *                            material
	 */
	public Material material(String name) {
		return (Material) custom(name);
	}

	/**
	 * Requests the sound denoted by {@code name}, check if it exists and returns
	 * null if it doesn't<br>
	 * Otherwise checks if the asset is loaded, and if not it is lazy loaded, then
	 * it is returned in both cases
	 *
	 * @param name The name of the asset
	 * @return The {@link Asset}
	 * @throws ClassCastException If the requested asset is not actually of type
	 *                            sound
	 */
	public Sound sound(String name) {
		return (Sound) custom(name);
	}

	// Instant-loading getters

	/**
	 * Requests the custom asset denoted by {@code name}, check if it exists and
	 * returns null if it doesn't<br>
	 * Otherwise checks if the asset is loaded, and if not it is loaded on the fly,
	 * then it is returned in both cases
	 *
	 * @param name The name of the asset
	 * @return The {@link Asset}
	 */
	public Asset requireCustom(String name) {
		if (!checkAsset(name)) {
			return null;
		}
		SheetEntry asset = assets.get(name);
		Asset ret = asset.asset;
		if (!ret.valid()) {
			loadAssetAuto(name, false);
		}
		return ret;
	}

	/**
	 * Requests the model denoted by {@code name}, check if it exists and returns
	 * null if it doesn't<br>
	 * Otherwise checks if the asset is loaded, and if not it is loaded on the fly,
	 * then it is returned in both cases
	 *
	 * @param name The name of the asset
	 * @return The {@link Asset}
	 * @throws ClassCastException If the requested asset is not actually of type
	 *                            model
	 */
	public Model requireModel(String name) {
		return (Model) requireCustom(name);
	}

	/**
	 * Requests the shader denoted by {@code name}, check if it exists and returns
	 * null if it doesn't<br>
	 * Otherwise checks if the asset is loaded, and if not it is loaded on the fly,
	 * then it is returned in both cases
	 *
	 * @param name The name of the asset
	 * @return The {@link Asset}
	 * @throws ClassCastException If the requested asset is not actually of type
	 *                            shader
	 */
	public Shader requireShader(String name) {
		return (Shader) requireCustom(name);
	}

	/**
	 * Requests the shader program denoted by {@code name}, check if it exists and
	 * returns null if it doesn't<br>
	 * Otherwise checks if the asset is loaded, and if not it is loaded on the fly,
	 * then it is returned in both cases
	 *
	 * @param name The name of the asset
	 * @return The {@link Asset}
	 * @throws ClassCastException If the requested asset is not actually of type
	 *                            shader program
	 */
	public ShaderProgram requireShaderProgram(String name) {
		return (ShaderProgram) requireCustom(name);
	}

	/**
	 * Requests the texture denoted by {@code name}, check if it exists and returns
	 * null if it doesn't<br>
	 * Otherwise checks if the asset is loaded, and if not it is loaded on the fly,
	 * then it is returned in both cases
	 *
	 * @param name The name of the asset
	 * @return The {@link Asset}
	 * @throws ClassCastException If the requested asset is not actually of type
	 *                            texture
	 */
	public Texture requireTexture(String name) {
		return (Texture) requireCustom(name);
	}

	/**
	 * Requests the material denoted by {@code name}, check if it exists and returns
	 * null if it doesn't<br>
	 * Otherwise checks if the asset is loaded, and if not it is loaded on the fly,
	 * then it is returned in both cases
	 *
	 * @param name The name of the asset
	 * @return The {@link Asset}
	 * @throws ClassCastException If the requested asset is not actually of type
	 *                            material
	 */
	public Material requireMaterial(String name) {
		return (Material) requireCustom(name);
	}

	/**
	 * Requests the sound denoted by {@code name}, check if it exists and returns
	 * null if it doesn't<br>
	 * Otherwise checks if the asset is loaded, and if not it is loaded on the fly,
	 * then it is returned in both cases
	 *
	 * @param name The name of the asset
	 * @return The {@link Asset}
	 * @throws ClassCastException If the requested asset is not actually of type
	 *                            sound
	 */
	public Sound requireSound(String name) {
		return (Sound) requireCustom(name);
	}

	// Getters

	/**
	 * Checks if the asset denoted by {@code name} is currently busy (being loaded,
	 * unloaded, etc)
	 *
	 * @param name The name of the asset to check
	 * @return Whether or not the asset is busy
	 * @throws NullPointerException if the asset is not present
	 */
	public boolean isAssetBusy(String name) {
		return assets.get(name).isBusy();
	}

	/**
	 * Retrieves the asset denoted by {@code name}
	 *
	 * @param name The name of the asset
	 * @return The asset or {@code null} if if it doesn't exist
	 */
	public Asset getAsset(String name) {
		SheetEntry entry = assets.get(name);
		return entry == null ? null : entry.asset;
	}

	/**
	 * Returns the {@link Game} instance associated with this object
	 *
	 * @return The {@link Game} instance
	 */
	public final Game getGame() {
		return game;
	}

	/**
	 * Retrieves the ready flag
	 *
	 * @return The ready flag
	 */
	public final boolean isReady() {
		return ready;
	}

}