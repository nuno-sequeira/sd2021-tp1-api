package tp1.impl.discovery;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * <p>A class to perform service discovery, based on periodic service contact endpoint 
 * announcements over multicast communication.</p>
 * 
 * <p>Servers announce their *name* and contact *uri* at regular intervals. The server actively
 * collects received announcements.</p>
 * 
 * <p>Service announcements have the following format:</p>
 * 
 * <p>&lt;service-name-string&gt;&lt;delimiter-char&gt;&lt;service-uri-string&gt;</p>
 */
public class Discovery {
	private static final Logger Log = Logger.getLogger(Discovery.class.getName());

	static {
		// addresses some multicast issues on some TCP/IP stacks
		System.setProperty("java.net.preferIPv4Stack", "true");
		// summarizes the logging format
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s");
	}
	
	
	// The pre-aggreed multicast endpoint assigned to perform discovery. 
	static final InetSocketAddress DISCOVERY_ADDR = new InetSocketAddress("226.226.226.226", 2266);
	static final int DISCOVERY_PERIOD = 1000;
	static final int DISCOVERY_TIMEOUT = 5000;
	//Default size for URIs array
	static final int DISCOVERY_URI_ARRAY_SIZE = 1000;

	static final long DURATION_LIMIT = 15000;

	// Used separate the two fields that make up a service announcement.
	private static final String DELIMITER = "\t";

	private final InetSocketAddress addr;
	private final String serviceName;
	private final String serviceURI;
	private final boolean sender;

	//Structure to save a list of URI's that contains each service
	private Map<String, Map<String,Long>> messageSenders;

	/**
	 * @param  serviceName the name of the service to announce
	 * @param  serviceURI an uri string - representing the contact endpoint of the service being announced
	 * @param  sender defines if this discovery only collects announcements or if it also sends messages
	 */
	public Discovery(InetSocketAddress addr, String serviceName, String serviceURI, boolean sender) {
		this.addr = addr;
		this.serviceName = serviceName;
		this.serviceURI  = serviceURI;
		this.messageSenders = new HashMap<>();
		this.sender = sender;
	}
	
	/**
	 * Starts sending service announcements at regular intervals... 
	 */
	public void start() {
		Log.info(String.format("Starting Discovery announcements on: %s for: %s -> %s", addr, serviceName, serviceURI));
		
		byte[] announceBytes = String.format("%s%s%s", serviceName, DELIMITER, serviceURI).getBytes();
		DatagramPacket announcePkt = new DatagramPacket(announceBytes, announceBytes.length, addr);

		try {
			MulticastSocket ms = new MulticastSocket( addr.getPort());
			ms.joinGroup(addr, NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));
			// start thread to send periodic announcements
			if(this.sender) {
				new Thread(() -> {
					for (; ; ) {
						try {
							ms.send(announcePkt);
							Thread.sleep(DISCOVERY_PERIOD);
						} catch (Exception e) {
							e.printStackTrace();
							// do nothing
						}
					}
				}).start();
			}
			// start thread to collect announcements
			new Thread(() -> {
				DatagramPacket pkt = new DatagramPacket(new byte[1024], 1024);

				for (;;) {
					try {
						//Calculates current timeStamp
						Long currentTimeStamp = Calendar.getInstance().getTimeInMillis();
						pkt.setLength(1024);
						ms.receive(pkt);
						String msg = new String( pkt.getData(), 0, pkt.getLength());
						String[] msgElems = msg.split(DELIMITER);
						if( msgElems.length == 2) {	//periodic announcement
							System.out.printf( "FROM %s (%s) : %s\n", pkt.getAddress().getCanonicalHostName(), 
									pkt.getAddress().getHostAddress(), msg);
							//Gets current message service
							Map<String, Long> currServiceMap = messageSenders.get(msgElems[0]);
							//If we haven't current service, we save msg info
							if( currServiceMap == null) {
								//Instantiates a new servicemap
								currServiceMap = new HashMap<>();
								//Adds current message's URI to the map
								currServiceMap.put(msgElems[1], currentTimeStamp);
								//Puts the new URI map in the services map
								messageSenders.put(msgElems[0], currServiceMap);
							}//Otherwise if the service's uri list doesn't contain such uri, we add it
							else if(!currServiceMap.containsKey(msgElems[1])) {
								//Adds current message's URI to the map
								currServiceMap.put(msgElems[1], currentTimeStamp);
								//Puts the updated URI map in the servicesMap
								messageSenders.put(msgElems[0], currServiceMap);
							}
						}
					} catch (IOException e) {
						// Prints message
						System.out.printf("New error in server - with service %s and URI %s - while receiving messages! Error description:\n %s",
								this.serviceName,this.serviceURI, e.getMessage());
					}
				}
			}).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Checks if there is elements in messageSenders that aren't responding for a given period
	 * and remove them from the messageSenders
	 */
	private void cleanUpSenderMessages(){
		//Instantiates current datetime in millis
		long currentTimeStamp = Calendar.getInstance().getTimeInMillis();
		//Gets the representation of the current messageSenders
		Map<String,Map<String,Long>> auxMessageSenders = this.messageSenders;
		//Gets the key set of the message Senders
		Set<String> serviceKeys = auxMessageSenders.keySet();
		//For each serviceName in the set
		for(String serviceName : serviceKeys){
			//Gets the respective uri/datetime Map
			Map<String,Long> uriMap = auxMessageSenders.get(serviceName);
			//Gets the respective key set of the uri map
			Set<String> uriKeys = uriMap.keySet();
			//For each uri Name
			for(String uriName : uriKeys ) {
				//Gets the respective datetime of its message
				Long auxTimeStamp = uriMap.get(uriName);
				//If the difference between current datetime and this URI obtained datetime is equal or greater than duration limit
				if (currentTimeStamp - auxTimeStamp >= DISCOVERY_TIMEOUT) {
					//Removes this uriname from the map
					uriMap.remove(uriName);
					//Updates the urimap in the high level map
					auxMessageSenders.put(serviceName, uriMap);
				}
			}
		}
		//Finally we update message Senders
		this.messageSenders = auxMessageSenders;
	}

	/**
	 * Returns the known servers for a service.
	 * 
	 * @param  serviceName the name of the service being discovered
	 * @return an array of URI with the service instances discovered. 
	 * 
	 */
	public URI[] knownUrisOf(String serviceName) {
		//Sanity check
		if(serviceName == null || serviceName.isEmpty())
			return null;
		//Cleans the unnecessary URIs
		cleanUpSenderMessages();

		//Instantiates uri array
		URI[] arrayURI = new URI[DISCOVERY_URI_ARRAY_SIZE];
		//Instantiates uri arrayList
		ArrayList<URI> uriArrayList = new ArrayList<>(DISCOVERY_URI_ARRAY_SIZE);
		//Gets URI set of current serviceName
		Set<String> serviceURIs = this.messageSenders.get(serviceName).keySet();
		//If serviceURI is empty, than we return empty array
		if(serviceURIs.size() == 0) return arrayURI;
		//For each service URI name, we create the respective URI and add it to the uri arrayList
		for (String serviceURI : serviceURIs)
			uriArrayList.add(URI.create(serviceURI));
		//Returns URI array with the previously obtained elements
		return uriArrayList.toArray(arrayURI);
	}	
	
	// Main just for testing purposes
	/*public static void main( String[] args) throws Exception {
		Discovery discovery = new Discovery( DISCOVERY_ADDR, "test", "http://" + InetAddress.getLocalHost().getHostAddress());
		discovery.start();
	}*/
}
