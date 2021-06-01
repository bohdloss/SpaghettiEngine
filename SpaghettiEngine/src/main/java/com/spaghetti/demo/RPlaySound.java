package com.spaghetti.demo;

import org.joml.Vector3f;

import com.spaghetti.audio.Sound;
import com.spaghetti.audio.SoundSource;
import com.spaghetti.core.Game;
import com.spaghetti.interfaces.ClassInterpreter;
import com.spaghetti.interfaces.ToServer;
import com.spaghetti.networking.DefaultInterpreters;
import com.spaghetti.networking.NetworkWorker;
import com.spaghetti.networking.RPC;

@ToServer
public class RPlaySound extends RPC {

	@Override
	protected ClassInterpreter<?>[] getArgInterpreters() {
		return new ClassInterpreter<?>[] {DefaultInterpreters.interpreters.get("java.lang.String"), DefaultInterpreters.interpreters.get("org.joml.Vector3f")};
	}

	@Override
	protected ClassInterpreter<?> getReturnInterpreter() {
		return null;
	}

	@Override
	protected boolean hasResponse() {
		return false;
	}

	@Override
	protected Object execute0(boolean isClient, NetworkWorker worker, Object[] args) throws Throwable {
		String asset_name = (String) args[0];
		Sound asset = Sound.get(asset_name);
		Vector3f pos = (Vector3f) args[1];
		SoundSource source = new SoundSource(asset);
		source.setWorldPosition(pos);
		source.play();
		Game.getGame().getActiveLevel().addObject(source);
		return null;
	}

}
