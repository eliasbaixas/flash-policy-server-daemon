package common.policyserver;

import java.io.IOException;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.log4j.Logger;

/**
 * void load(String[] arguments): Here open the configuration files, create the trace file, create the ServerSockets, the Threads
 * void start(): Start the Thread, accept incoming connections
 * void stop(): Inform the Thread to live the run(), close the ServerSockets
 * void destroy(): Destroy any object created in init()
 * 
 * @author elias
 *
 */
public class PolicyServerDaemon {
	
	private static final Logger log = Logger.getLogger(PolicyServerDaemon.class);

	private PolicyServer policyServer;

	/**
	 * Here open the configuration files, create the trace file, create the ServerSockets, the Threads
	 * @param arguments
	 */
	void load(String[] arguments){
		policyServer = new PolicyServer();
		try {
			policyServer.startListening();
		} catch (IOException e) {
			log.error(e.getMessage(),e);
		}
	}
	
	/**
	 * Start the Thread, accept incoming connections
	 */
	void start(){
		policyServer.start();
	}
	
	/**
	 * Inform the Thread to live the run(), close the ServerSockets
	 */
	void stop(){
		policyServer.stop();
	}

	/**
	 * Destroy any object created in init()
	 */
	void destroy(){
		
	}
}
