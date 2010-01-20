package test.policyserver;

import java.net.Socket;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintWriter;

import org.apache.log4j.Logger;

/**  Test Client
*
* @Author Syed (www.flash-resources.net)
*/


public class Client extends Thread{
	
	private static Logger log = Logger.getLogger(Client.class);
	
	private static final String POLICY_REQUEST = "<policy-file-request/>";

	String name;
	String host;
	int port;
	int interval;
	long repeatTimes;
	
	public static void main(String[] args) {
		
		Client client = new Client("Test","localhost",843,10,100);
		client.start();
	}
	
	public Client(String name,String host,int port,long repeatTimes,int interval) {
		this.name = name;
		this.host = host;
		this.port = port;
		this.interval = interval;
		this.repeatTimes = repeatTimes;
		
		log.info("---------------------");
		log.info("Client:" + name);
		log.info("interval:" + interval);
		log.info("repeatTimes:" + repeatTimes);
		log.info("---------------------");
	}

	public void run() {

		long curRepeat = 0;
		
		log.debug("[" +name+"] started");
		
		do {
			
			requestPolicyFromServer();
			
			if ( interval > 0) {
				try {
						Thread.sleep(interval);
				} catch(Exception e) {
					
				}
			}
			
			curRepeat++;
			log.debug("[" +name+"]Current repeat:" + curRepeat);
			
		}while(repeatTimes==-1 || curRepeat < repeatTimes);
		
		log.debug("[" +name+"]Finished repeats:" + curRepeat);
	}
	
	private void requestPolicyFromServer() {
		
		BufferedReader socketIn;
		PrintWriter socketOut;
		
		try {
			
			log.debug("[" +name+"]connecting to " + host + " at " + port);
			Socket socket = new Socket(host,port);
			log.debug("[" +name+"]connected!");
			socket.setSoTimeout(10000);
			
			socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			socketOut = new PrintWriter(socket.getOutputStream(), true);
			
			log.debug("[" +name+"]sending request for policy xml");
			writePolicyRequest(socketOut);
			
			String policyXml = read(socketIn);
			
			if (!policyXml.startsWith("<?xml")) 
				log.error("[" +name+"]Invalid response from Policy Server:" + policyXml);
			else
				log.debug("[" +name+"]Successfully received policy xml!");
			
		}catch(Exception e) {
			e.printStackTrace();
			log.error(e.getMessage(),e);
		}
	}
	
	private void writePolicyRequest(PrintWriter socketOut) {
		socketOut.println(POLICY_REQUEST+"\u0000");
		socketOut.flush();
		log.debug("[" +name+"]Wrote Policy Request:" + POLICY_REQUEST);
	}
	
	private String read( BufferedReader socketIn) throws IOException, EOFException, InterruptedIOException {
		StringBuffer buffer = new StringBuffer();
		int codePoint;
		boolean zeroByteRead=false;
		
		log.debug("[" +name+"]Reading response from server...");
		
		do {
			codePoint=socketIn.read();
			if (codePoint==0) zeroByteRead=true;
			else buffer.appendCodePoint( codePoint );
		} while (!zeroByteRead);
		
		log.debug("[" +name+"]Received Policy Xml: "+buffer.toString());
		
		return buffer.toString().trim();
	}
}
