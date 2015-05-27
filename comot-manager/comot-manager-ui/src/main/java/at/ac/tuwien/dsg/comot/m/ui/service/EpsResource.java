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
package at.ac.tuwien.dsg.comot.m.ui.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import at.ac.tuwien.dsg.comot.m.common.InfoClient;
import at.ac.tuwien.dsg.comot.m.common.InfoServiceUtils;
import at.ac.tuwien.dsg.comot.m.common.MngPath;
import at.ac.tuwien.dsg.comot.m.common.exception.ComotException;
import at.ac.tuwien.dsg.comot.m.common.exception.EpsException;
import at.ac.tuwien.dsg.comot.m.core.Coordinator;
import at.ac.tuwien.dsg.comot.m.core.lifecycle.LifeCycleManager;
import at.ac.tuwien.dsg.comot.model.provider.OfferedServiceUnit;
import at.ac.tuwien.dsg.comot.model.provider.OsuInstance;
import at.ac.tuwien.dsg.comot.model.type.State;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

// WADL http://localhost:8380/comot/rest/application.wadl
@Service
@Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
@Path(MngPath.EPS)
@Api(value = MngPath.EPS, description = "Management of Elastic Platform Services (EPSs)")
public class EpsResource {

	private static final Logger LOG = LoggerFactory.getLogger(EpsResource.class);

	@Autowired
	protected Coordinator coordinator;
	@Autowired
	protected InfoClient infoServ;
	@Autowired
	protected LifeCycleManager lcManager;

	@javax.annotation.Resource
	public Environment env;

	private static final String UPLOADS_DIR = "uploads/";

	@POST
	@Path(MngPath.INIT_EPS_EXTERNAL)
	@Produces(MediaType.TEXT_PLAIN)
	@ApiOperation(
			value = "Add new external EPS. Returns the participantId/epsInstanceId.",
			response = String.class)
	public Response addEps(
			@ApiParam(value = "Description of the EPS", required = true) OfferedServiceUnit osu)
			throws Exception {

		String epsInstanceId = coordinator.addStaticEps(osu);
		return Response.ok(epsInstanceId).build();
	}

	// @POST
	// @Path("/{epsId}/instances")
	// @Consumes(MediaType.MULTIPART_FORM_DATA)
	// @Produces(MediaType.TEXT_PLAIN)
	// @ApiOperation(
	// value = "Create a new instance of the user-managed EPS",
	// response = String.class)
	// public Response createDynamicEpsInstance(
	// @ApiParam(value = "ID of the EPS", required = true) @PathParam("epsId") String epsId,
	// @ApiParam(value = "Configuration file", required = false) FormDataMultiPart form)
	// throws ComotException, ClassNotFoundException, IOException, JAXBException {
	//
	// // get file
	// FormDataBodyPart filePart = form.getField("file");
	// ContentDisposition headerOfFilePart = filePart.getContentDisposition();
	// InputStream fileInputStream = filePart.getValueAs(InputStream.class);
	// String filePath = UPLOADS_DIR + headerOfFilePart.getFileName();
	//
	// // save file
	// File newFile = new File(filePath);
	// newFile.getParentFile().mkdirs();
	// FileUtils.copyInputStreamToFile(fileInputStream, newFile);
	//
	// String serviceInstanceId = "aaa " + filePath;

	// LOG.info(serviceInstanceId);
	//
	// return Response.ok(serviceInstanceId).build();
	// }

	@POST
	@Path("/{epsId}/instances")
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.TEXT_PLAIN)
	@ApiOperation(
			value = "Create a new instance of the user-managed EPS",
			response = String.class)
	public Response createDynamicEpsInstance(
			@ApiParam(value = "ID of the EPS", required = true) @PathParam("epsId") String epsId)
			throws ComotException, ClassNotFoundException, IOException, JAXBException {

		String serviceInstanceId = coordinator.createDynamicService(epsId);

		LOG.info(serviceInstanceId);

		return Response.ok(serviceInstanceId).build();
	}

	@DELETE
	@Path("/{epsId}/instances/{epsInstanceId}")
	@ApiOperation(
			value = "Delete the instance of the user-managed EPS")
	public Response removeDynamicEpsInstance(
			@ApiParam(value = "ID of the EPS instance", required = true) @PathParam("epsInstanceId") String epsInstanceId,
			@ApiParam(value = "ID of the EPS", required = true) @PathParam("epsId") String epsId)
			throws ComotException, ClassNotFoundException, IOException, JAXBException {

		coordinator.removeDynamicService(epsId, epsInstanceId);
		return Response.ok().build();
	}

	@GET
	@Consumes(MediaType.WILDCARD)
	@ApiOperation(
			value = "Get EPSs",
			response = OfferedServiceUnit.class,
			responseContainer = "List")
	public Response getElasticPlatformServices(
			@ApiParam(value = "Type of EPSs to filter", required = false, allowableValues = InfoClient.ALL
					+ ", " + InfoClient.EXTERNAL + ", " + InfoClient.USER_MANAGED) @DefaultValue(InfoClient.ALL) @QueryParam("type") String type)
			throws EpsException {

		List<OfferedServiceUnit> allEps = new ArrayList<>(infoServ.getOsus());

		if (InfoClient.ALL.equals(type)) {

		} else if (InfoClient.EXTERNAL.equals(type)) {
			for (Iterator<OfferedServiceUnit> iterator = allEps.iterator(); iterator.hasNext();) {
				OfferedServiceUnit osu = iterator.next();
				if (InfoServiceUtils.isDynamicEps(osu)) {
					iterator.remove();
				}
			}

		} else if (InfoClient.USER_MANAGED.equals(type)) {
			for (Iterator<OfferedServiceUnit> iterator = allEps.iterator(); iterator.hasNext();) {
				OfferedServiceUnit osu = iterator.next();
				if (!InfoServiceUtils.isDynamicEps(osu)) {
					iterator.remove();
				}
			}

		} else {
			allEps = new ArrayList<OfferedServiceUnit>();
		}

		return Response.ok(allEps.toArray(new OfferedServiceUnit[allEps.size()])).build();
	}

	@GET
	@Consumes(MediaType.WILDCARD)
	@Path("/{epsId}/instances")
	@ApiOperation(
			value = "Get EPS instances of a single EPS",
			response = OsuInstance.class)
	public Response getEpsInstancesOfOneEps(
			@ApiParam(value = "ID of the EPS", required = true) @PathParam("epsId") String epsId)
			throws EpsException {

		List<OsuInstance> epsInstances = infoServ.getOsuInstancesForOsu(epsId);

		return Response.ok(epsInstances.toArray(new OsuInstance[epsInstances.size()])).build();
	}

	@GET
	@Consumes(MediaType.WILDCARD)
	@Path("/instances")
	@ApiOperation(
			value = "Get EPS instances",
			response = OsuInstance.class,
			responseContainer = "List")
	public Response getEpsInstances(
			@ApiParam(value = "Type of EPS instances to filter", required = false, allowableValues = InfoClient.ALL
					+ ", " + InfoClient.EXTERNAL + ", " + InfoClient.USER_MANAGED + ", " + InfoClient.ACTIVE) @DefaultValue(InfoClient.ALL) @QueryParam("type") String type)
			throws EpsException {

		List<OsuInstance> allEpsInstances;
		State state;
		if (InfoClient.ACTIVE.equals(type)) {

			allEpsInstances = infoServ.getEpsInstances(InfoClient.EXTERNAL);

			for (OsuInstance oneInstance : infoServ.getEpsInstances(InfoClient.USER_MANAGED)) {
				state = lcManager.getCurrentStateService(oneInstance.getService().getId());

				LOG.info("state {}", state);

				if (State.RUNNING.equals(state)) {
					allEpsInstances.add(oneInstance);
				}
			}

		} else {
			allEpsInstances = infoServ.getEpsInstances(type);
		}

		return Response.ok(allEpsInstances.toArray(new OsuInstance[allEpsInstances.size()])).build();
	}
}
