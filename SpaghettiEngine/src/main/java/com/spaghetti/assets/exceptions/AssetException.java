package com.spaghetti.assets.exceptions;

import com.spaghetti.assets.Asset;

public class AssetException extends RuntimeException {

    protected final Asset asset;

    public AssetException(Asset asset, String message) {
        super(message);
        this.asset = asset;
    }

    public AssetException(Asset asset, String message, Throwable cause) {
        super(message, cause);
        this.asset = asset;
    }

    public Asset getAsset() {
        return asset;
    }

}
