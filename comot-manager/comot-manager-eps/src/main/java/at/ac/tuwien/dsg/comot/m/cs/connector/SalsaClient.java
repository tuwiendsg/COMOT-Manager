/*******************************************************************************
 * Copyright 2014 Technische Universitat Wien (TUW), Distributed Systems Group E184
 *
 * This work was partially supported by the European Commission in terms of the
 * CELAR FP7 project (FP7-ICT-2011-8 \#317790)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package at.ac.tuwien.dsg.comot.m.cs.connector;

import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.oasis.tosca.Definitions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.tuwien.dsg.cloud.salsa.common.cloudservice.model.CloudService;
import at.ac.tuwien.dsg.cloud.salsa.tosca.extension.SalsaInstanceDescription_VM;
import at.ac.tuwien.dsg.cloud.salsa.tosca.extension.SalsaMappingProperties;
import at.ac.tuwien.dsg.comot.m.common.ServiceClient;
import at.ac.tuwien.dsg.comot.m.common.exception.ComotException;
import at.ac.tuwien.dsg.comot.m.common.exception.EpsException;
import at.ac.tuwien.dsg.csdg.inputProcessing.multiLevelModel.deploymentDescription.DeploymentDescription;

public class SalsaClient extends ServiceClient {

	private static final Logger LOG = LoggerFactory.getLogger(SalsaClient.class);

	protected static final String DEF_BASE_PATH = "http://127.0.0.1:8380/salsa-engine/rest";

	protected static final String DEPLOY_PATH = "services/xml";
	protected static final String UNDEPLOY_PATH = "services/{serviceId}";
	protected static final String SPAWN_PATH = "services/{serviceId}/topologies/{topologyId}/nodes/{nodeId}/instance-count/{instanceCount}";
	protected static final String DESTROY_PATH = "services/{serviceId}/topologies/{topologyId}/nodes/{nodeId}/instances/{instanceId}";
	protected static final String STATUS_PATH = "services/{serviceId}";
	protected static final String TOSCA_PATH = "services/tosca/{serviceId}";
	protected static final String DEPLOYMENT_INFO_PATH = "services/tosca/{serviceId}/sybl";
	protected static final String SERVICES_LIST = "viewgenerator/cloudservice/json/list";

	public SalsaClient() throws URISyntaxException {
		this(new URI(DEF_BASE_PATH));
	}

	public SalsaClient(URI baseUri) {
		super("SALSA", baseUri);
	}

	public String deploy(String toscaDescriptionXml) throws EpsException {

		LOG.debug(ln + "Deploying cloud application: {}", toscaDescriptionXml);

		Response response = client.target(getBaseUri())
				.path(DEPLOY_PATH)
				.request(MediaType.APPLICATION_XML)
				.put(Entity.xml(toscaDescriptionXml));

		processResponseStatus(response);

		String serviceId = response.readEntity(String.class);

		LOG.debug(ln + "deployed service. Response: '{}'",
				serviceId);

		return serviceId;

	}

	public void undeploy(String serviceId) throws EpsException {

		LOG.trace(ln + "Undeploying service with serviceId '{}'", serviceId);

		Response response = client.target(getBaseUri())
				.path(UNDEPLOY_PATH)
				.resolveTemplate("serviceId", serviceId)
				.request(MediaType.TEXT_XML)
				.delete();

		processResponseStatus(response);

		String msg = response.readEntity(String.class);

		LOG.debug(ln + "undeployed '{}'. Response: '{}'", serviceId, msg);

	}

	public void spawn(String serviceId, String topologyId, String nodeId, int instanceCount)
			throws EpsException {

		LOG.trace(ln +
				"Spawning additional instances (+{}) for serviceId={}, topologyId={}, nodeId={}",
				instanceCount, serviceId, topologyId, nodeId);

		Response response = client.target(getBaseUri())
				.path(SPAWN_PATH)
				.resolveTemplate("serviceId", serviceId)
				.resolveTemplate("topologyId", topologyId)
				.resolveTemplate("nodeId", nodeId)
				.resolveTemplate("instanceCount", instanceCount)
				.request(MediaType.TEXT_XML)
				.post(Entity.text(""));

		processResponseStatus(response);

		String msg = response.readEntity(String.class);

		LOG.debug(
				name + "Spawned additional instances (+{}) for serviceId={}, topologyId={}, nodeId={}. Response: '{}'",
				instanceCount, serviceId, topologyId, nodeId, msg);

	}

	public void destroy(String serviceId, String topologyId, String nodeId, int instanceId) throws EpsException {

		LOG.trace(ln + "Destroying instance with id {} (service: {} topology: {} node: {})",
				instanceId, serviceId, topologyId, nodeId);

		Response response = client.target(getBaseUri())
				.path(DESTROY_PATH)
				.resolveTemplate("serviceId", serviceId)
				.resolveTemplate("topologyId", topologyId)
				.resolveTemplate("nodeId", nodeId)
				.resolveTemplate("instanceId", instanceId)
				.request(MediaType.TEXT_XML)
				.delete();

		processResponseStatus(response);

		String msg = response.readEntity(String.class);

		LOG.debug(ln + "Sestroyed instance with id {} (service={}, topology={}, node={}). Response: '{}'",
				instanceId, serviceId, topologyId, nodeId, msg);

	}

	public CloudService getStatus(String serviceId) throws ComotException {

		LOG.trace(ln + "Checking status for serviceId {}", serviceId);

		Response response = client.target(getBaseUri())
				.path(STATUS_PATH)
				.resolveTemplate("serviceId", serviceId)
				.request(MediaType.TEXT_XML)
				.get();

		processResponseStatus(response);

		String msg = response.readEntity(String.class);

		try (StringReader reader = new StringReader(msg)) {

			JAXBContext jaxbContext = JAXBContext.newInstance(CloudService.class, SalsaInstanceDescription_VM.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			CloudService service = (CloudService) jaxbUnmarshaller.unmarshal(reader);

			LOG.debug(ln + "Checked status for serviceId '{}'. Response: '{}'", serviceId, service);

			return service;

		} catch (JAXBException e) {
			throw new ComotException("Failed to unmarshall response into JAXB status CloudService", e);
		}

	}

	public Definitions getTosca(String serviceId) throws ComotException {

		LOG.trace(ln + "Getting tosca for serviceId {}", serviceId);

		Response response = client.target(getBaseUri())
				.path(TOSCA_PATH)
				.resolveTemplate("serviceId", serviceId)
				.request(MediaType.TEXT_XML)
				.get();

		processResponseStatus(response);

		String msg = response.readEntity(String.class);

		try (StringReader reader = new StringReader(msg)) {

			JAXBContext jaxbContext = JAXBContext.newInstance(Definitions.class, SalsaMappingProperties.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			Definitions service = (Definitions) jaxbUnmarshaller.unmarshal(reader);

			LOG.debug(ln + "Tosca for serviceId '{}'. Response: '{}'", serviceId, service);

			return service;

		} catch (JAXBException e) {
			throw new ComotException("Failed to unmarshall response into JAXB TOSCA", e);
		}

	}

	/**
	 * Use {@link #getStatus(String) getStatus} instead
	 * 
	 * @param serviceId
	 * @return
	 * @throws EpsException
	 */
	@Deprecated
	public DeploymentDescription getServiceDeploymentInfo(String serviceId) throws EpsException {

		LOG.trace(ln + "Getting DeploymentInfo for serviceId {}", serviceId);

		Response response = client.target(getBaseUri())
				.path(DEPLOYMENT_INFO_PATH)
				.resolveTemplate("serviceId", serviceId)
				.request(MediaType.TEXT_XML)
				.get();

		processResponseStatus(response);

		DeploymentDescription description = response.readEntity(DeploymentDescription.class);

		LOG.debug(ln + "DeploymentInfo for serviceId '{}'. Response: '{}'", serviceId, description);

		return description;
	}

	public String getServices() throws EpsException {

		LOG.trace(ln + "Getting list of all services {}");

		Response response = client.target(getBaseUri())
				.path(SERVICES_LIST)
				.request(MediaType.TEXT_PLAIN)
				.get();

		processResponseStatus(response);

		String msg = response.readEntity(String.class);

		LOG.debug(ln + "List of all services. Response: '{}'", msg);

		return msg;
	}

}
