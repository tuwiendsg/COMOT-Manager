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
package at.ac.tuwien.dsg.comot.m.ui;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;

import at.ac.tuwien.dsg.comot.m.ui.service.EpsResource;
import at.ac.tuwien.dsg.comot.m.ui.service.RevisionResource;
import at.ac.tuwien.dsg.comot.m.ui.service.ServicesResource;
import at.ac.tuwien.dsg.comot.m.ui.service.TemplatesResource;

/**
 * https://jersey.java.net/documentation/latest/media.html#json.moxy
 * 
 * @author Juraj
 *
 */
public class JerseyMoxyConfig extends ResourceConfig {

	public JerseyMoxyConfig() {
		// REST RESOURCES
		register(ServicesResource.class);
		register(RevisionResource.class);
		register(EpsResource.class);
		register(TemplatesResource.class);
		// CONFIGURATION
		register(RequestContextFilter.class);
		register(DefinitionsContextResolver.class);
		register(ComotExceptionMapper.class);
		register(MultiPartFeature.class);
		// register(ToscaValidatingReader.class);
	}
}
