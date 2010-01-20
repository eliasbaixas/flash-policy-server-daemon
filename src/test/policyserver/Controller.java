package test.policyserver;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/**  Test Controller
*
* @Author Syed (www.flash-resources.net)
*/

public class Controller {

	private static Logger log = Logger.getLogger(Controller.class);
	
	private static String CONFIG_XML = "test-config.xml";
	
	public static void main(String[] args) {

		Controller controller = new Controller();
		controller.start();
	}
	
	private List<ClientGroup> clientGroups = new ArrayList<ClientGroup>();
	private String host="localhost";
	private int port=843;
	
	public void start() {
		loadConfiguration();
		
		createClients();
		
		startClients();
	}
	
	private void startClients() {
		
		for (ClientGroup clientGroup:clientGroups) {
			boolean first = true;
			for (Client client:clientGroup.clientList) {
				
				// if concurrent start immediately
				if ( ! clientGroup.concurrent) {
					// if not concurrent, start first client immediately
					// but delay for interval period before starting other clients
					if (first) {
						first = false;
					}
					else {
						 try {
							 Thread.sleep(client.interval);
						 } catch(Exception e) {}
					}
				}
				client.start();
			}
		}
	}
	
	private void createClients() {
		int interval=0;
		for (ClientGroup clientGroup:clientGroups) {
			for (int i=0;i<clientGroup.clients;i++) {
				
				if (!clientGroup.concurrent) 
					interval= (int) (Math.random() * clientGroup.interval);
				else
					interval = clientGroup.interval;
				
				Client client = new Client(clientGroup.name  + "-" + i ,host,port,clientGroup.repeatTimes,interval);
				
				clientGroup.clientList.add(client);
			}
		}
	}
	
	@SuppressWarnings({ "unchecked"})
	private void loadConfiguration(){
		SAXBuilder builder = new SAXBuilder();
		
		try {
			
			Document doc = builder.build(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(CONFIG_XML)));
			Element root = doc.getRootElement();
			
			host = root.getAttributeValue("host");
			port = Integer.parseInt(root.getAttributeValue("port"));
			
			List<Element> children = root.getChildren("ClientGroup");
			for(Element child:children) {
				
				ClientGroup clientGroup = new ClientGroup();
				clientGroup.name = child.getAttributeValue("name");
				clientGroup.concurrent = Boolean.parseBoolean(child.getAttributeValue("concurrent"));
				clientGroup.clients = Integer.parseInt(child.getAttributeValue("clients"));
				clientGroup.interval = Integer.parseInt(child.getAttributeValue("interval"));
				clientGroup.repeatTimes = Long.parseLong(child.getAttributeValue("repeat_times"));
				
				log.info("Client Group:" + clientGroup.name);
				log.info("concurrent:" + clientGroup.concurrent);
				
				clientGroups.add(clientGroup);
			}
			
			log.info("Number of client groups:" + clientGroups.size());
		
		} catch (Exception e){
			log.error("Error loading configuration from " +CONFIG_XML );
		}
		
	}
	
	class ClientGroup {
		public String name = null;
		public boolean concurrent = false;
		public int clients = 0;
		public int interval = 0;
		public long repeatTimes = 0;
		public List<Client> clientList = new ArrayList<Client>();
	}

}
