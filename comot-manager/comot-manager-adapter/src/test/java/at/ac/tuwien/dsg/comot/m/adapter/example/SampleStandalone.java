package at.ac.tuwien.dsg.comot.m.adapter.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import at.ac.tuwien.dsg.comot.m.adapter.general.EpsAdapterManager;
import at.ac.tuwien.dsg.comot.m.adapter.general.Manager;

@Component
public class SampleStandalone {

	@Autowired
	protected SampleProcessor processor;
	@Autowired
	protected EpsAdapterManager manager;

	@Autowired
	protected ApplicationContext context;

	public static String SOME_CUSTOM_EVENT = "SOME_CUSTOM_EVENT";

	public void startAdapter(String epsInstanceId) throws Exception {

		Manager manager = context.getBean(EpsAdapterManager.class);

		// start adapter
		manager.start(epsInstanceId, processor);
	}

	// public void startAdapter() {
	//
	// String baseUri = "http://localhost:8580/comot/rest/";
	//
	// // describe the new EPS
	// OfferedServiceUnit eps = new OfferedServiceUnit("OurCustomEPSid", "Our Custom EPS Name",
	// OsuType.EPS.toString(),
	// new String[] { Constants.ROLE_OBSERVER });
	// eps.hasPrimitiveOperation( // optionally define custom event
	// new ComotCustomEvent("Do something", SOME_CUSTOM_EVENT, false));
	//
	// Client client = ClientBuilder.newClient();
	//
	// // insert the description of the EPS into the management system
	// Response response = client.target(baseUri)
	// .path(MngPath.EPS_EXTERNAL)
	// .request(MediaType.WILDCARD)
	// .post(Entity.xml(eps));
	// String epsInstanceId = response.readEntity(String.class);
	// client.close();
	//
	// // start adapter
	// manager.start(epsInstanceId, processor);
	// }
}
