package common.policyserver;

import java.io.IOException;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.log4j.Logger;

/**
 * void init(context): Here open the configuration files, create the trace file, create the ServerSockets, the Threads
 * void start(): Start the Thread, accept incoming connections
 * void stop(): Inform the Thread to live the run(), close the ServerSockets
 * void destroy(): Destroy any object created in init()
 * 
 * @author elias
 *
 */
public class PolicyServerDaemon implements Daemon{
	
	private static final Logger log = Logger.getLogger(PolicyServerDaemon.class);

	private PolicyServer policyServer;

	/**
	 * Start the Thread, accept incoming connections
	 */
	public void start(){
		policyServer.start();
	}
	
	/**
	 * Inform the Thread to live the run(), close the ServerSockets
	 */
	public void stop(){
		policyServer.stop();
	}

	/**
	 * Destroy any object created in init()
	 */
	public void destroy(){
		
	}

	/**
	 * Here open the configuration files, create the trace file, create the ServerSockets, the Threads
	 * @param arguments
	 */
	@Override
	public void init(DaemonContext arg0) throws Exception {
		policyServer = new PolicyServer();
		try {
			policyServer.startListening();
		} catch (IOException e) {
			log.error(e.getMessage(),e);
		}
	}
}
