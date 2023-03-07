package com.spaghetti.assets.exceptions;

import com.spaghetti.assets.AssetEntry;

public class AssetManagerException extends RuntimeException {

    protected final AssetEntry sheet;

    public AssetManagerException(AssetEntry sheet, String message) {
        super(message);
        this.sheet = sheet;
    }

    public AssetManagerException(AssetEntry sheet, String message, Throwable cause) {
        super(message, cause);
        this.sheet = sheet;
    }

    public AssetEntry getAssetSheet() {
        return sheet;
    }

}
