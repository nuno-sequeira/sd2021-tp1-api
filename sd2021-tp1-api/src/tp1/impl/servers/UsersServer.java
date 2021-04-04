package sd2021.aula2.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.logging.Logger;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import sd2021.aula2.discovery.Discovery;
import sd2021.aula2.server.resources.UsersResource;

public class UsersServer {

	private static final Logger Log = Logger.getLogger(UsersServer.class.getName());

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
	}

	public static final int PORT = 8080;
	public static final String SERVICE = "UsersService";
	static final InetSocketAddress DISCOVERY_ADDR = new InetSocketAddress("226.226.226.226", 2266);
	public static final boolean SENDER = true;

	public static void main(String[] args) {
		try {
			String ip = InetAddress.getLocalHost().getHostAddress();

			ResourceConfig config = new ResourceConfig();
			config.register(UsersResource.class);

			String serverURI = String.format("http://%s:%s/rest", ip, PORT);
			JdkHttpServerFactory.createHttpServer( URI.create(serverURI), config);

			Log.info(String.format("%s Server ready @ %s\n",  SERVICE, serverURI));

			//More code can be executed here...
			Discovery discovery = new Discovery(DISCOVERY_ADDR, SERVICE, serverURI, SENDER);
			discovery.start();

		} catch(Exception e) {
			Log.severe(e.getMessage());
		}
	}

}
