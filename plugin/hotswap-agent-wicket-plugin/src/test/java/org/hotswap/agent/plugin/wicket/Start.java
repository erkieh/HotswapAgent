package org.hotswap.agent.plugin.wicket;

import org.apache.wicket.util.time.Duration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.webapp.WebAppContext;

public class Start {
	public static void main(String[] args) throws Exception {
		Server server = createServer();
		startServer(server);
	}
	
	public static void startServer(Server server) {
		try {
			System.out.println(">>> STARTING EMBEDDED JETTY SERVER, PRESS ANY KEY TO STOP");
			server.start();
			System.in.read();
			System.out.println(">>> STOPPING EMBEDDED JETTY SERVER");
			server.stop();
			server.join();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public static Server createServer() {
		Server server = new Server();
		SocketConnector connector = new SocketConnector();
		
		// Set some timeout options to make debugging easier.
		connector.setMaxIdleTime((int) Duration.ONE_HOUR.getMilliseconds());
		connector.setSoLingerTime(-1);
		connector.setPort(8080);
		server.addConnector(connector);
		
		WebAppContext bb = new WebAppContext();
		bb.setServer(server);
		bb.setWar("src/test/webapp");
		bb.setParentLoaderPriority(false);
		
		server.setHandler(bb);
		return server;
	}
}
