package com.spaghetti.assets;

import java.util.*;

import com.spaghetti.assets.exceptions.AssetFillException;
import com.spaghetti.core.Game;
import com.spaghetti.core.GameBuilder;
import com.spaghetti.core.events.GameStoppingEvent;
import com.spaghetti.utils.*;

/**
 * A class that manages assets and their dependencies, including loading and
 * unloading them dynamically as needed
 *
 * @author bohdloss
 */
public class AssetManager {

	// Reference to the game instance
	protected final Game game;

	// Final fields
	protected final HashMap<String, AssetLoader<?>> loaders = new HashMap<>();
	protected final HashMap<String, AssetEntry> assets = new HashMap<>();
	protected final LoadQueue queue;

	/**
	 * Initializes the AssetManager with a link to the {@code game} instance it is
	 * to be associated with
	 * <p>
	 * Users that intend to modify the behavior of the asset manager in their game
	 * must include a constructor with the same parameters as this one
	 * <p>
	 * This class is not meant to be used in a standalone way, therefore it has not
	 * been tested that way and no guarantee is to be made in that case neither by
	 * this class nor it's subclasses
	 * <p>
	 * Refer to the documentation of {@link GameBuilder}
	 *
	 * @param game The game this asset manager is to be associated with
	 */
	public AssetManager(Game game) {
		this.game = game;

		// Register shutdown hook
		game.getEventDispatcher().registerEventListener(GameStoppingEvent.class, (isClient, event) -> {
			try {
				destroy();
			} catch(Throwable t) {
				Logger.error("Error destroying asset manager", t);
			}
		});

		// Initialize asset load queue
		queue = new LoadQueue(this);
	}

	protected void fillAsset(AssetEntry asset) {
		AssetLoader<?> loader = getAssetLoader(asset.type);
		if (loader == null) {
			throw new AssetFillException(asset, "No loader registered for asset type " + asset.type);
		}

		Object[] data;
		try {
			// Load data and feed it to the asset
			data = loader.load(asset.args);
			asset.asset.setData(data);

			// The data was incorrect
			if (!asset.asset.isFilled()) {
				throw new AssetFillException(asset, "Asset didn't accept arguments provided by loader");
			}
		} catch (Throwable t0) {
			Logger.error("Couldn't load " + asset.type + " " + asset.name +
					", trying to use default version instead", t0);

			try {
				// Second attempt at loading data, using the default arguments this time
				data = loader.load(loader.getDefaultArgs());
				asset.asset.setData(data);

				// The data is still incorrect, giving up
				if (!asset.asset.isFilled()) {
					throw new AssetFillException(asset,
							"Attempt to use default version of asset type " + asset.type + " failed for " + asset.name);
				}
			} catch (Throwable t1) {
				throw new AssetFillException(asset,
						"Every attempt to load " + asset.type + " " + asset.name + " failed", t1);
			}
		}
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
	 * @return Whether the operation succeeded
	 */
	public boolean loadAssetSheet(String sheetLocation) {
		try {
			// Load the sheet file
			String sheetSource = ResourceLoader.loadText(sheetLocation);

			// Split into lines
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
				String name = tokens[1];
				String type = tokens[0];
				String[] args = new String[tokens.length - 2];
				for (int i = 0; i < args.length; i++) {
					args[i] = tokens[i + 2];
				}
				registerAsset(name, type, args);
			}

			Logger.loading(game, "Successfully loaded asset sheet " + sheetLocation);
			return true;
		} catch (Throwable t) {
			Logger.error(game, "Could not load asset sheet " + sheetLocation, t);
			return false;
		}
	}

	/**
	 * Resets this asset manager to its idle state by unloading all assets
	 * with {@link #unloadAll()} and unregistering all imported assets, then
	 * changing the {@code ready} flag to {@code false}
	 */
	public void destroy() {
		queue.killThread();

		for (Object assetName : assets.keySet().toArray()) {
			if(!isDefaultAsset((String) assetName)) {
				unregisterAsset((String) assetName);
			}
		}
		for (Object assetType : loaders.keySet().toArray()) {
			unregisterAssetLoader((String) assetType);
		}
	}

	// Load / Unload methods
	protected static interface LoadFunction {

		public abstract void load(AssetEntry entry);

	}

	protected boolean meetDependencies(AssetEntry asset, LoadFunction loadFunc) {
		// Retrieve dependencies
		String[] dependencies = null;
		try {
			dependencies = getAssetLoader(asset.type).getDependencies(asset.args);
		} catch (Throwable t) {
		}

		// Load with specified method
		boolean found = false;
		if (dependencies != null) {

			// Iterate through dependencies
			for (String assetName : dependencies) {

				// Retrieve the object
				AssetEntry dependency = assets.get(assetName);

				// Perform some checks
				if (dependency == null) {
					Logger.error("[meetDependencies] " + asset.name + " depends on undefined dependency " + assetName);
					return true;
				} else if (!dependency.asset.isLoaded()) {
					found = true;
					if (loadFunc != null) {
						Logger.debug("[meetDependencies] Loading dependency " + assetName + " of " + asset.name);
						loadFunc.load(dependency);
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
		if (!assetExists(name) || game.isHeadless()) {
			return false;
		}
		// Check if the asset is ready to be processed
		AssetEntry asset = assets.get(name);
		synchronized (asset) {
			// The asset is busy but not loading, forward it to the queue function
			if (asset.asset.isLoaded() || asset.loading || asset.isBusy()) {
				return false;
			}

			asset.loading = true;

			// Queue dependencies
			meetDependencies(asset, dependency -> loadAssetLazy(dependency.name));

			// Send asset to load queue
			queue.queueAsset(asset);

		}

		return true;
	}

	/**
	 * Loads the asset denoted by the given name in the renderer thread
	 * <p>
	 * Any dependency will be loaded recursively
	 *
	 * @param name The name of the asset to load
	 * @return Returns {@code true} if the asset has been successfully loaded,
	 *         {@code false} otherwise
	 */
	public boolean loadAssetNow(String name) {
		if(!game.isHeadless()) {
			if (Thread.currentThread().getId() == game.getRendererId()) {
				return loadAssetDirect(name);
			} else {
				FunctionDispatcher dispatcher = game.getRendererDispatcher();
				return (Boolean) dispatcher.quickQueue(() -> loadAssetDirect(name));
			}
		}
		return false;
	}

	protected boolean loadAssetDirect(String assetName) {
		Logger.debug("[loadAssetDirect] " + assetName);

		if (game.isHeadless() || !assetExists(assetName)) {
			return false;
		}

		// Check if the asset is ready to be processed
		AssetEntry asset = assets.get(assetName);
		synchronized (asset) {
			if (asset.asset.isLoaded() || asset.loading) {
				return true;
			}

			// The asset is busy doing something else, wait for it to be ready
			while (asset.isBusy()) {
				ThreadUtil.sleep(1);
			}

			meetDependencies(asset, dependency -> loadAssetDirect(dependency.name));

			// Begin loading process
			asset.loading = true;

			try {
				// Fill asset
				fillAsset(asset);

				// Perform native loading
				asset.asset.load();
				Logger.debug("[loadAssetDirect] Asset loaded " + assetName);
			} catch (Throwable t) {
				Logger.error("[loadAssetDirect] Error loading " + asset.type + " " + asset.name, t);
				return false;
			} finally {
				asset.loading = false;
			}
		}

		return true;
	}

	/**
	 * Loads every asset that is currently registered amd unloaded
	 *
	 * @param lazy Whether to load the assets on a separate thread or not
	 * @return Returns true if all the assets were loaded with no errors
	 */
	public boolean loadAll(boolean lazy) {
		if (game.isHeadless()) {
			return false;
		}

		boolean value = true;
		for (AssetEntry entry : assets.values()) {
			value &= lazy ? loadAssetLazy(entry.name) : loadAssetNow(entry.name);
		}
		return value;
	}

	/**
	 * Unloads the asset denoted by the given name and then returns
	 * <p>
	 * Any asset that depends on this asset will be unloaded recursively
	 *
	 * @param name The name of the asset to unload
	 * @return Returns {@code true} if the asset has been successfully unloaded,
	 *         {@code false} otherwise
	 */
	public boolean unloadAssetNow(String name) {
		if(!game.isHeadless()) {
			if (Thread.currentThread().getId() == game.getRendererId()) {
				return unloadAssetDirect(name);
			} else {
				FunctionDispatcher dispatcher = game.getRendererDispatcher();
				return (Boolean) dispatcher.quickQueue(() -> unloadAssetDirect(name));
			}
		}
		return false;
	}

	protected boolean unloadAssetDirect(String name) {
		if ((Thread.currentThread() != game.getRenderer()) || !assetExists(name) || game.isHeadless()) {
			return false;
		}

		// Check if the asset is ready to be processed
		AssetEntry asset = assets.get(name);
		synchronized (asset) {
			if (asset.asset.isUnloaded() || asset.unloading) {
				return true;
			}

			// The asset is busy, wait for it to be ready
			while (asset.isBusy()) {
				ThreadUtil.sleep(1);
			}

			// Discover and unload all assets that depend on this
			for(AssetEntry entry : assets.values()) {
				if(loaders.get(entry.type) == null) {
					Logger.info(entry.type);
				}
				String[] entryDependencies = loaders.get(entry.type).getDependencies(entry.args);

				// Iterate through dependencies and check if asset.name is present
				if(entryDependencies != null) {
					for (String dependency : entryDependencies) {

						// It's present, unload entry.name
						if (dependency.equals(asset.name)) {
							unloadAssetDirect(entry.name);
						}
					}
				}
			}

			// Begin unloading process
			asset.unloading = true;

			try {
				// Perform native unloading directly
				asset.asset.unload();
			} catch (Throwable t) {
				Logger.error("Error unloading " + asset.type + " " + asset.name, t);
				return false;
			} finally {
				asset.unloading = false;
			}
		}

		return true;
	}

	/**
	 * Unloads every asset currently registered and loaded
	 *
	 * @return This function returns true only if all the
	 */
	public boolean unloadAll() {
		if (game.isHeadless()) {
			return false;
		}
		boolean value = true;
		boolean deletedOne = assets.size() > 0;
		for (AssetEntry entry : assets.values()) {
			value &= unloadAssetNow(entry.name);
		}
		if (deletedOne) {
			Logger.info(game, "Finished unloading assets");
		}
		return value;
	}

	// Utility methods

	/**
	 * Retrieves the type of the asset denoted by {@code assetName} and returns it
	 *
	 * @param assetName The assetName of the asset
	 * @return The type of the asset
	 * @throws NullPointerException If the asset is not present
	 */
	public String getAssetType(String assetName) {
		return assets.get(assetName).type;
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
		assetType = assetType.toLowerCase();
		if (loader == null || assetType == null || loaders.containsKey(assetType)) {
			throw new IllegalArgumentException();
		}
		loaders.put(assetType, loader);

		if(!game.isHeadless()) {
			try {
				String assetName = getDefaultAssetName(assetType);
				registerAsset(assetName, assetType, loader.getDefaultArgs(), loader.instantiate(), true);
				loadAssetNow(assetName);
			} catch (Throwable t1) {
				Logger.error("Every attempt to load the default version of asset type " + assetType, t1);
			}
		}

	}

	/**
	 * Unregisters a loader for the given {@code assetType}<br>
	 * This method can unregister existing loaders and default loaders
	 *
	 * @param assetType The asset type to unregister the loader for
	 * @throws IllegalArgumentException If {@code assetType} is null
	 */
	public void unregisterAssetLoader(String assetType) {
		assetType = assetType.toLowerCase();
		if (assetType == null || !loaders.containsKey(assetType)) {
			throw new IllegalArgumentException();
		}

		if(!game.isHeadless()) {
				try {
					unregisterAsset(getDefaultAssetName(assetType), true);
				} catch (Throwable t) {
					Logger.error("Error unloading the default asset for " + assetType, t);
				}
		}
		loaders.remove(assetType);
	}

	/**
	 * Registers an asset entry just like it was imported with
	 * {@link #loadAssetSheet(String)}<br>
	 *
	 * @param assetName      The name of the asset to register
	 * @param assetType      The type of the asset
	 * @param assetArguments The arguments that will be used when loading it
	 * @throws IllegalArgumentException If {@code assetName}, {@code assetType},
	 *                                  {@code assetArguments} or any of the entries
	 *                                  in {@code assetArguments} is null
	 */
	public void registerAsset(String assetName, String assetType, String[] assetArguments) {
		assetType = assetType.toLowerCase();
		registerAsset(assetName, assetType, assetArguments, getAssetLoader(assetType).instantiate());
	}

	/**
	 * Returns the name of the default asset automatically generated
	 * for each asset type
	 *
	 * @param assetType The asset type
	 * @return Its corresponding asset
	 */
	public String getDefaultAssetName(String assetType) {
		return "%" + assetType.toLowerCase() + "%";
	}

	/**
	 * Returns true if the given assets is a default asset
	 *
	 * @param assetName The asset name
	 * @return Whether the asset is a default asset
	 */
	public boolean isDefaultAsset(String assetName) {
		AssetEntry entry = assets.get(assetName);
		return entry.name.equals(getDefaultAssetName(entry.type));
	}

	/**
	 * Registers an asset entry just like it if it was imported with
	 * {@link #loadAssetSheet(String)}<br>
	 * This method will use the provided {@code template} instead of allocating a
	 * new asset of that type
	 * <p>
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
		registerAsset(assetName, assetType, assetArguments, template, false);
	}

	protected void registerAsset(String assetName, String assetType, String[] assetArguments, Asset template, boolean bypassCharacters) {
		// Sanity checks
		assetType = assetType.toLowerCase();
		if (assetName == null || assetType == null || assetArguments == null) {
			throw new IllegalArgumentException("Null asset name, type or arguments");
		}
		if(template.getName() != null && !template.getName().equals(assetName)) {
			throw new IllegalArgumentException("Mismatching asset name");
		}
		if(!assetNameValid(assetName) && !bypassCharacters) {
			throw new IllegalArgumentException("Invalid characters found in asset name: " + assetName);
		}
		if(assets.containsKey(assetName)) {
			throw new IllegalArgumentException("Asset " + assetName + " is already registered, unregister it first");
		}

		for (String assetArg : assetArguments) {
			if (assetArg == null) {
				throw new IllegalArgumentException("Null asset arguments");
			}
		}

		// Initialize asset struct
		AssetEntry asset = new AssetEntry(this);
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
	 * Using this method to unregister internal assets is possible
	 * but will cause instability
	 *
	 * @param assetName The name of the asset to unregister
	 * @throws IllegalArgumentException If {@code assetName} is null
	 */
	public void unregisterAsset(String assetName) {
		unregisterAsset(assetName, false);
	}

	protected void unregisterAsset(String assetName, boolean bypassCharacter) {
		if(!assetNameValid(assetName) && !bypassCharacter) {
			throw new IllegalArgumentException("Invalid characters found in asset name: " + assetName);
		}
		if (assetName == null || !assets.containsKey(assetName)) {
			throw new IllegalArgumentException("Cannot unregister unknown asset: " + assetName);
		}
		if(assets.get(assetName).asset.isLoaded()) {
			unloadAssetNow(assetName);
		}
		assets.remove(assetName);
	}

	protected boolean assetNameValid(String name) {
		return !name.equals("") &&
				!name.contains("%") &&
				!name.contains("/") &&
				!name.contains("\\") &&
				!name.contains(" ") &&
				!name.contains("\"") &&
				!name.contains("'") &&
				!name.contains(" ");
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
	 * Checks for the ready flag and if the asset denoted by {@code assetName} exists, and
	 * returns {@code true} if both checks succeed
	 *
	 * @param assetName The assetName of the asset to perform the check on
	 * @return {@code true} if the checks succeeded, {@code false} otherwise
	 */
	protected boolean assetExists(String assetName) {
		if (assets.get(assetName) == null) {
			Logger.warning("Non existant asset requested: " + assetName);
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
	public <T extends Asset> T getAndLazyLoadAsset(String name) {
		if (!assetExists(name)) {
			return null;
		}
		AssetEntry asset = assets.get(name);
		Asset ret = asset.asset;
		if (!ret.isValid()) {
			loadAssetLazy(ret.getName());
		}
		return (T) ret;
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
	public <T extends Asset> T getAndInstantlyLoadAsset(String name) {
		if (!assetExists(name)) {
			return null;
		}
		AssetEntry asset = assets.get(name);
		Asset ret = asset.asset;
		if (!ret.isValid()) {
			loadAssetNow(name);
		}
		return (T) ret;
	}

	// Getters

	/**
	 * Checks if the asset denoted by {@code name} is currently busy (being loaded,
	 * unloaded, etc)
	 *
	 * @param name The name of the asset to check
	 * @return Whether the asset is busy
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
	public <T extends Asset> T getAsset(String name) {
		AssetEntry entry = assets.get(name);
		return entry == null ? null : (T) entry.asset;
	}

	public <T extends Asset> T getDefaultAsset(String assetType) {
		AssetEntry entry = assets.get(getDefaultAssetName(assetType));
		return entry == null ? null : (T) entry.asset;
	}

	/**
	 * Returns the {@link Game} instance associated with this object
	 *
	 * @return The {@link Game} instance
	 */
	public final Game getGame() {
		return game;
	}

}