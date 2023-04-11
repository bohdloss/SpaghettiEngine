package com.spaghetti.render;

import com.spaghetti.core.Game;

public class VertexShader extends Shader {

    public static VertexShader getDefault() {
        return Game.getInstance().getAssetManager().getDefaultAsset("VertexShader");
    }

    public VertexShader() {
    }

    public VertexShader(String source) {
        super(source, Shader.VERTEX_SHADER);
    }

    @Override
    public void setData(Object[] data) {
        super.setData(new Object[] {data[0], Shader.VERTEX_SHADER});
    }

    @Override
    public int getId() {
        return isLoaded() ? id : getDefault().id;
    }

}
