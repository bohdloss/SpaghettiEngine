package com.spaghetti.assets.exceptions;

import com.spaghetti.assets.AssetEntry;

public class AssetFillException extends AssetManagerException {

    public AssetFillException(AssetEntry sheet, String message) {
        super(sheet, message);
    }

    public AssetFillException(AssetEntry sheet, String message, Throwable cause) {
        super(sheet, message, cause);
    }

}
