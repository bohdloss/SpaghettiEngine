package com.spaghetti.networking;

import java.io.IOException;
import java.net.*;

import com.spaghetti.core.*;
import com.spaghetti.utils.*;

public class Client extends CoreComponent {

	protected NetworkWorker worker;
	protected long clientId;

	protected boolean justConnected;

	@Override
	protected void initialize0() throws Throwable {
		worker = new NetworkWorker(this);
		internal_connect("localhost", 9018);
	}

	@Override
	protected void terminate0() throws Throwable {
		internal_disconnect();
		worker.destroy();
	}

	
	long last = 0;
	@Override
	protected void loopEvents(double delta) throws Throwable {
		Utils.sleep(25);
		if (isConnected()) {

			worker.writeSocket();
			worker.readSocket();

//			if(System.currentTimeMillis() > last + 1000) {
//				worker.readData();
//				last = System.currentTimeMillis();
//			}
			worker.readData();
		}
	}

	@Override
	protected final CoreComponent provideSelf() {
		return getGame().getClient();
	}

	public boolean connect(String ip, int port) {
		return (boolean) getDispatcher().quickQueue(() -> internal_connect(ip, port));
	}

	protected boolean internal_connect(String ip, int port) {
		if (isConnected()) {
			Logger.warning("Already connected, disconnecting first");
			internal_disconnect();
		}
		try {
			worker.provideSocket(new Socket(ip, port));
			justConnected = true;
		} catch (UnknownHostException e) {
			Logger.error("Host could not be resolved: " + ip);
			return false;
		} catch (IOException e) {
			Logger.error("I/O error occurred while connecting", e);
			return false;
		}
		Logger.info("Connected to " + ip + " at port " + port);
		return true;
	}

	public boolean disconnect() {
		return (boolean) getDispatcher().quickQueue(() -> internal_disconnect());
	}

	protected boolean internal_disconnect() {
		if (!isConnected()) {
			Logger.warning("Already disconnected");
			return false;
		}
		String remoteIp = getRemoteIp();
		int remotePort = getRemotePort();
		try {
			worker.resetSocket();
		} catch (IOException e) {
			Logger.error("I/O error occurred while disconnecting, ignoring");
			return true;
		} catch (Throwable e) {
			Logger.error("Unknown error occurred while disconnecting, ignoring", e);
			return true;
		}
		Logger.info("Disconnected from " + remoteIp + " from port " + remotePort);
		return true;
	}

	// Getters and setters

	public boolean isConnected() {
		return worker.isConnected();
	}

	public String getRemoteIp() {
		return worker.getRemoteIp();
	}

	public int getRemotePort() {
		return worker.getRemotePort();
	}

	public String getLocalIp() {
		return worker.getLocalIp();
	}

	public int getLocalPort() {
		return worker.getLocalPort();
	}

}