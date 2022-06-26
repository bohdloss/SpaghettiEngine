package com.spaghetti.demo;

import org.joml.Vector3f;

import com.spaghetti.audio.Sound;
import com.spaghetti.audio.SoundSource;
import com.spaghetti.core.Game;
import com.spaghetti.core.GameObject;
import com.spaghetti.networking.RemoteProcedure;
import com.spaghetti.objects.Mesh;
import com.spaghetti.render.Material;
import com.spaghetti.render.Model;

public class RPlaySound extends RemoteProcedure {

	@Override
	protected Class<?>[] getArgumentTypes() {
		return new Class[] { String.class, Vector3f.class };
	}

	@Override
	protected Class<?> getReturnType() {
		return null;
	}

	@Override
	protected Object onCall(Object[] args, GameObject player) throws Throwable {
		String asset_name = (String) args[0];
		Sound asset = Sound.get(asset_name);
		Vector3f pos = (Vector3f) args[1];
		SoundSource source = new SoundSource(asset);
		source.setWorldPosition(pos);
		source.play();
		Mesh mesh = new Mesh(Model.get("square"), Material.get("defaultMAT"));
		mesh.setWorldPosition(pos);
		Game game = Game.getGame();
		player.getLevel().addObject(source);
		player.getLevel().addObject(mesh);
		game.getNetworkManager().queueWriteObjectTree(mesh);
		game.getNetworkManager().queueWriteObject(mesh);
		return null;
	}

}
