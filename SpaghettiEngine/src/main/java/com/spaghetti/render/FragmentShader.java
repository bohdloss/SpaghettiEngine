package com.spaghetti.render;

import com.spaghetti.core.Game;

public class FragmentShader extends Shader {

    public static FragmentShader getDefault() {
        return Game.getInstance().getAssetManager().getDefaultAsset("FragmentShader");
    }

    public FragmentShader() {
    }

    public FragmentShader(String source) {
        super(source, Shader.FRAGMENT_SHADER);
    }

    @Override
    public void setData(Object[] data) {
        super.setData(new Object[] {data[0], Shader.FRAGMENT_SHADER});
    }

    @Override
    public int getId() {
        return isLoaded() ? id : getDefault().id;
    }

}
