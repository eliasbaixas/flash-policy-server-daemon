package common.policyserver;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.Properties;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;


/**
 * Class PolicyServer
 * Starts a PolicyServer on the specified port.
 * Can be started as main class, passing the port number as the first command line argument
 *
 * @Author Syed (www.flash-resources.net)
 */

public class PolicyServer  {

	private static final Logger log = Logger.getLogger(PolicyServer.class);
	
	// If param is passed from the config, the server will listen on this port for connections
	// 843 is the default port by flash player for security policy
	private static final int DEFAULT_POLICY_PORT = 843;
	
	private static final String DEFAULT_POLICY_FILE = "default_policy.xml";
	private static final String CONFIG_FILE = "config.properties";
	private static final int DEFAULT_THREAD_COUNT = 10;
	private static final int DEFAULT_THREAD_IDLE_PERIOD=10;
	private static final int DEFAULT_POOL_LOG_INTERVAL=60;
	// Read timeout at 10 secs (10000 ms)
	private static final int DEFAULT_READ_TIMEOUT = 10;
	// Maximum queue length for incoming connection indications (a request to connect)
	private static final int MAX_QUEUE_LENGTH = 50;
	
	// Sleep delay to avoid flooding at 10 ms
	private static final int SLEEP_DELAY = 10;
	
	// Pool Logger Timer delay 
	private static final long TIMER_DELAY = 100;
	
	// PolicyServer instance variables	
	private int policyServerPort;
	private String policyServerRequest;
	private boolean listening;
	private ServerSocket socketServer;
	private String policy;
    private final ThreadPoolExecutor threadPool;
    private String policyFile;
    private int threadPoolSizeMin;
    private int threadPoolSizeMax;
    private int threadMaxIdleSeconds;
    private int poolStatusLogInterval;
    private int socketTimeout;
    private Timer timer;
    private PoolStatusLogger poolStatusLogger;
	
	// The character sequence sent by the Flash Player to request a policy file
	private static final String DEFAULT_POLICY_REQUEST = "<policy-file-request/>";
	
	private static final int BUFFER_LENGTH = 100;
	
	/**
	 * @param args	Use the first command line argument to set the port the server 
	 * will listen on for connections
	 */
	public static void main(String[] args) {		
		// Start the PolicyServer
		PolicyServer policyServer = new PolicyServer();
		try {
			policyServer.startListening();
			policyServer.start();
		} catch (IOException e) {
			log.error(e.getMessage(),e);
		}
	}

	/**
	 * PolicyServer constructor
	 * @param port	Sets the port that the PolicyServer listens on
	 */
	public PolicyServer() {
		
		listening = false;
		
		loadConfiguration();
		
		log.debug("policy file is:" + policyFile);
		log.debug("thread_pool_size_min is:" + threadPoolSizeMin);
		log.debug("thread_pool_size_max is:" + threadPoolSizeMax);
		log.debug("thread_max_idle_seconds is:" + threadMaxIdleSeconds);
		log.debug("pool_status _log_interval_seconds is:" + poolStatusLogInterval);
		log.debug("policy server port is:  " + policyServerPort);
		log.debug("policy server request is:  " + policyServerRequest);
		
		policy = readPolicyFromFile(policyFile);
		
		log.info("Policy is :\n" + policy);
		
		threadPool = new ThreadPoolExecutor(threadPoolSizeMin, threadPoolSizeMax, 
				threadMaxIdleSeconds, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		
		timer = new Timer ("PoolStatusLogger") ;
		poolStatusLogger = new PoolStatusLogger();
	    timer.schedule ( poolStatusLogger , TIMER_DELAY, poolStatusLogInterval*1000 ) ;
	}
	
	/**
	 * load config properties file and set instance variables
	 */
	private void loadConfiguration() {
			
		Properties config = new Properties();
		
		try {
		
			log.info("Loading configuration from " + CONFIG_FILE);
			
			config.load(this.getClass().getClassLoader().getResourceAsStream(CONFIG_FILE));
			
			policyFile = config.getProperty("policy_file", DEFAULT_POLICY_FILE);
			
			policyServerPort =  Integer.parseInt(config.getProperty("policy_server_port", 
					new Integer(DEFAULT_POLICY_PORT).toString()));
			
			policyServerRequest =  config.getProperty("policy_server_request", DEFAULT_POLICY_REQUEST);
			
			threadPoolSizeMin = Integer.parseInt(config.getProperty("thread_pool_size_min", 
					new Integer(DEFAULT_THREAD_COUNT).toString()));
			
			threadPoolSizeMax = Integer.parseInt(config.getProperty("thread_pool_size_max", 
					new Integer(DEFAULT_THREAD_COUNT).toString()));
			
			threadMaxIdleSeconds =  Integer.parseInt(config.getProperty("thread_max_idle_seconds", 
					new Integer(DEFAULT_THREAD_IDLE_PERIOD).toString()));
			
			poolStatusLogInterval =  Integer.parseInt(config.getProperty("pool_status_log_interval_seconds", 
					new Integer(DEFAULT_POOL_LOG_INTERVAL).toString()));
			
			socketTimeout =  Integer.parseInt(config.getProperty("socket_timeout_seconds", 
					new Integer(DEFAULT_READ_TIMEOUT).toString())) * 1000;
			
		} catch (IOException e) {
			log.error("Error loading configuation file:" + CONFIG_FILE, e);
		}
	}
	
	/**
	 * Read policy 
	 * @param filename
	 * @return policy as a String
	 */
	private String readPolicyFromFile(String filename) {
	    
		StringBuffer contents = new StringBuffer();
	    
	    try {
			  //use buffering, reading one line at a time
			  //FileReader always assumes default encoding is OK!
			  BufferedReader input =  new BufferedReader(new InputStreamReader(
					  this.getClass().getClassLoader().getResourceAsStream(filename)));
			  try {
				    String line = null; //not declared within while loop
				    while (( line = input.readLine()) != null){
				      contents.append(line);
				      contents.append(System.getProperty("line.separator"));
				    }
			  } finally {
			    input.close();
			  }
	    } catch (IOException ex){
	      ex.printStackTrace();
	    }
	    
	    return contents.toString();	
	}
	
	public void startListening() throws IOException{
		
		// Start listening for connections
		socketServer = new ServerSocket(policyServerPort, MAX_QUEUE_LENGTH);
		listening = true;
		
		log.info("PolicyServer listening on port " + policyServerPort);
		
	}

	/**
	 * Start listening on specified port and instantiate new SocketConnection 
	 * thread to serve incoming request
	 */
	public void start() {
		
		try {
			
			while(listening) {
				
				// Wait for a connection and accept it
				Socket socket = socketServer.accept();
				
				try {
					
					if (log.isDebugEnabled())
						log.debug("PolicyServer got a connection on port " + policyServerPort + " from " 
								+ socket.getRemoteSocketAddress().toString());
					
					// Execute the request handler using thread pool
					threadPool.execute(new SocketConnection(socket));
					
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
				try {
					// Wait until a new connection is accepted to avoid flooding
					Thread.sleep(SLEEP_DELAY);
				} catch (InterruptedException e) {
					
				}
				// for testing... exit after one connection
				//listening = false;
			}
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		} finally {
			timer.cancel();
			threadPool.shutdownNow();
		}
	}
	
	public void stop(){
		try {
			socketServer.close();
			threadPool.shutdown();
			threadPool.awaitTermination(3, TimeUnit.SECONDS);
			System.exit(0);
		} catch (IOException e) {
			log.error(e.getMessage(),e);
		} catch (InterruptedException e) {
			log.error(e.getMessage(),e);
		}
	}
	
	/**
	 * Local class SocketConnection
	 * For every accepted connection one SocketConnection is created.
	 * It waits for the policy file request, returns the policy file and closes the connection immediately
	 *
	 */
	class SocketConnection implements Runnable {
		
		private Socket socket;
		private BufferedReader socketIn;
		private PrintWriter socketOut;
		
		/**
		 * Constructor takes the Socket object for this connection
		 * @param socket	Socket connection to a client created by the PolicyServer main thread
		 */
		public SocketConnection(Socket socket) {
			this.socket = socket;
		}
		
		/**
		 * Thread run method waits for the policy request, 
		 * returns the poilcy file and closes the connection
		 */
		public void run() {
			try {
				// initialize socket and readers/writers
				// set a read timeout of 10 secs
				socket.setSoTimeout(socketTimeout);
				socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				socketOut = new PrintWriter(socket.getOutputStream(), true);
				readPolicyRequest();
			} catch (IOException e) {
				log.error("SocketConnection:run", e);
			} finally {
				close();
			}
			
		}
		
		/**
		 * Wait for and read the policy request sent by the Flash Player
		 * Return the policy file and close the Socket connection
		 */
		private void readPolicyRequest() {
			try {
				// Read the request and compare it to the request string defined in the constants.
				// If the proper _policy request has been sent write out the _policy file
				if (read().startsWith(policyServerRequest)) 
					write(policy);
			} catch (Exception e) {
				log.error("SocketConnection:readPolicyRequest", e);
			}
		}
		
		/**
		 * Read until a zero character is sent or a maximum of 100 character
		 * @return The character sequence read
		 * @throws IOException
		 * @throws EOFException
		 * @throws InterruptedIOException
		 */
		private String read() throws IOException, EOFException, InterruptedIOException {
			StringBuffer buffer = new StringBuffer();
			int codePoint;
			boolean zeroByteRead = false;
			
			if (log.isDebugEnabled()) 
				log.debug("Reading...");
			
			do {
				codePoint = socketIn.read();
				if (codePoint == 0 || codePoint == -1) 
					zeroByteRead = true;
				else 
					buffer.appendCodePoint( codePoint );
			} while (!zeroByteRead && buffer.length() < PolicyServer.BUFFER_LENGTH);
			
			if (log.isDebugEnabled()) 
				log.debug("Read: " + buffer.toString());
			
			return buffer.toString().trim();
		}
		
		/**
		 * Writes a String to the client
		 * @param msg	Text to be sent to the client (policy file)
		 */
		public void write(String msg) {
			socketOut.println(msg + "\u0000");
			socketOut.flush();
			if (log.isDebugEnabled()) 
				log.debug("Wrote: " + msg);
		}
		
		/**
		 * Close the Socket connection an set everything to null. Prepared for garbage collection
		 */
		public void close() {
			try {
				if (socketOut != null) 
					socketOut.close();
			} catch (Exception e) {
				
			}
			try {
				if (socketIn != null) 
					socketIn.close();
			} catch (Exception e) {
				
			}
			try {
				if (socket != null) 
					socket.close();
			} catch (Exception e) {
				
			}

			socketIn = null;
			socketOut = null;
			socket = null;
		}
		
	}
	
	class PoolStatusLogger extends TimerTask {
		public void run() {
			
			log.info("-----------------THREAD POOL STATUS----------------------");
			log.info("Total received task count since startup: " + threadPool.getTaskCount() );
			log.info("Total completed task count since startup: " + threadPool.getCompletedTaskCount() );
			log.info("Current active task count:" + threadPool.getActiveCount() );
			log.info("Largest Pool Size Reached:" + threadPool.getLargestPoolSize() );
			log.info("Current Pool Size:" + threadPool.getPoolSize() );
			log.info("---------------------------------------------------------");
		}
	}

}
