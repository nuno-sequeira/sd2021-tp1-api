package tp1.impl.clients.userClient;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.glassfish.jersey.client.ClientConfig;
import tp1.api.User;
import tp1.api.service.rest.RestUsers;
import tp1.impl.discovery.Discovery;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class CreateUserClient {

	public static final String SERVICE = "UsersService";
	public static final String CREATEUSERCLIENT = "CreateUserClient";
	static final InetSocketAddress DISCOVERY_ADDR = new InetSocketAddress("226.226.226.226", 2266);
	public static final boolean SENDER = false;
	public static final int PORT = 8080;

	public static void main(String[] args) throws IOException {

		if( args.length != 5) {
			System.err.println( "Use: java sd2021.aula2.clients.CreateUserClient url userId fullName email password");
			return;
		}

		String serverUrl = args[0];
		String userId = args[1];
		String fullName = args[2];
		String email = args[3];
		String password = args[4];

		User u = new User( userId, fullName, email, password);

		String ip = InetAddress.getLocalHost().getHostAddress();
		String serverURI = String.format("http://%s:%s/rest", ip, PORT);

		//Instantiates discovery
		Discovery discovery = new Discovery(DISCOVERY_ADDR, CREATEUSERCLIENT, serverURI, SENDER);
		//Starts discovery threads
		discovery.start();
		//Instantiates serverIsActive flag
		boolean serverIsActive = false;

		//While there is no active server to provide requested service, we wait
		while (!serverIsActive){
			if (discovery.knownUrisOf(SERVICE).length > 0)
				serverIsActive = true;
		}

		System.out.println("Sending request to server.");

		ClientConfig config = new ClientConfig();
		Client client = ClientBuilder.newClient(config);

		WebTarget target = client.target( serverUrl ).path( RestUsers.PATH );


		Response r = target.request()
				.accept(MediaType.APPLICATION_JSON)
				.post(Entity.entity(u, MediaType.APPLICATION_JSON));

		if( r.getStatus() == Status.OK.getStatusCode() && r.hasEntity() )
			System.out.println("Success, created user with id: " + r.readEntity(String.class) );
		else
			System.out.println("Error, HTTP error status: " + r.getStatus() );



	}

}
