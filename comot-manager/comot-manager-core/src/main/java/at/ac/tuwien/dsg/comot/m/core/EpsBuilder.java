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
package at.ac.tuwien.dsg.comot.m.core;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import at.ac.tuwien.dsg.comot.m.adapter.general.Bindings;
import at.ac.tuwien.dsg.comot.m.adapter.general.Processor;
import at.ac.tuwien.dsg.comot.m.common.InformationClient;
import at.ac.tuwien.dsg.comot.m.common.enums.Action;
import at.ac.tuwien.dsg.comot.m.common.enums.EpsEvent;
import at.ac.tuwien.dsg.comot.m.common.enums.Type;
import at.ac.tuwien.dsg.comot.m.common.event.CustomEvent;
import at.ac.tuwien.dsg.comot.m.common.event.LifeCycleEvent;
import at.ac.tuwien.dsg.comot.m.common.event.state.ExceptionMessage;
import at.ac.tuwien.dsg.comot.m.common.event.state.Transition;
import at.ac.tuwien.dsg.comot.model.devel.structure.CloudService;

@Component
public class EpsBuilder extends Processor {

	private static final Logger LOG = LoggerFactory.getLogger(EpsBuilder.class);

	@Autowired
	protected ApplicationContext context;
	@javax.annotation.Resource
	public Environment env;
	@Autowired
	protected InformationClient infoService;

	@Override
	public Bindings getBindings(String instanceId) {

		return new Bindings()
				.addLifecycle("*." + Action.CREATED + "." + Type.SERVICE + ".#")
				.addLifecycle("*." + Action.UNDEPLOYED + "." + Type.SERVICE + ".#")

				.addCustom("*." + EpsEvent.EPS_DYNAMIC_REQUESTED + "." + Type.SERVICE + ".*")
				.addCustom("*." + EpsEvent.EPS_DYNAMIC_REMOVED + "." + Type.SERVICE + ".*")
				.addCustom("*." + EpsEvent.EPS_SUPPORT_ASSIGNED + "." + Type.SERVICE + ".*");
	}

	@Override
	public void onLifecycleEvent(String serviceId, String groupId, Action action, CloudService service,
			Map<String, Transition> transitions, LifeCycleEvent event) throws Exception {

		if (infoService.isServiceOfDynamicEps(serviceId)) {

			if (action == Action.CREATED) {
				String staticDeplId = infoService.instanceIdOfStaticEps(env.getProperty("eps.deployment.central"));

				manager.sendCustomEvent(serviceId, serviceId, EpsEvent.EPS_SUPPORT_REQUESTED.toString(), staticDeplId,
						null);

			} else if (action == Action.UNDEPLOYED) {

				manager.sendLifeCycleEvent(serviceId, serviceId, Action.REMOVED);

			}
		}
	}

	@Override
	public void onCustomEvent(String serviceId, String groupId, String eventName, String epsId, String optionalMessage,
			Map<String, Transition> transitions, CustomEvent event) throws Exception {

		EpsEvent action = EpsEvent.valueOf(eventName);

		if (action == EpsEvent.EPS_DYNAMIC_REQUESTED && !event.getOrigin().equals(getId())) {

			String newServiceId = infoService.getOsuInstance(optionalMessage).getService().getId();

			manager.sendLifeCycleEvent(newServiceId, newServiceId, Action.CREATED);

		} else if (action == EpsEvent.EPS_SUPPORT_ASSIGNED && infoService.isServiceOfDynamicEps(serviceId)) {

			manager.sendLifeCycleEvent(serviceId, serviceId, Action.START);

		} else if (action == EpsEvent.EPS_DYNAMIC_REMOVED) {

			manager.sendLifeCycleEvent(serviceId, serviceId, Action.STOP);
		}
	}

	@Override
	public void onExceptionEvent(ExceptionMessage msg) throws Exception {
		// not needed
	}

}
