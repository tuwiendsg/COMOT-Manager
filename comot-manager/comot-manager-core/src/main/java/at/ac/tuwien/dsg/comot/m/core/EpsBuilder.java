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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import at.ac.tuwien.dsg.comot.m.adapter.general.Processor;
import at.ac.tuwien.dsg.comot.m.common.Constants;
import at.ac.tuwien.dsg.comot.m.common.EpsAdapterStatic;
import at.ac.tuwien.dsg.comot.m.common.InformationClient;
import at.ac.tuwien.dsg.comot.m.common.enums.Action;
import at.ac.tuwien.dsg.comot.m.common.enums.EpsEvent;
import at.ac.tuwien.dsg.comot.m.common.enums.Type;
import at.ac.tuwien.dsg.comot.m.common.event.CustomEvent;
import at.ac.tuwien.dsg.comot.m.common.event.LifeCycleEvent;
import at.ac.tuwien.dsg.comot.m.common.event.state.ExceptionMessage;
import at.ac.tuwien.dsg.comot.m.common.event.state.Transition;
import at.ac.tuwien.dsg.comot.m.common.exception.EpsException;
import at.ac.tuwien.dsg.comot.model.devel.structure.CloudService;
import at.ac.tuwien.dsg.comot.model.provider.OfferedServiceUnit;
import at.ac.tuwien.dsg.comot.model.provider.Resource;
import at.ac.tuwien.dsg.comot.model.type.OsuType;

@Component
public class EpsBuilder extends Processor {

	private static final Logger LOG = LoggerFactory.getLogger(EpsBuilder.class);

	@Autowired
	protected ApplicationContext context;
	@Autowired
	protected InformationClient infoService;

	private Set<String> staticEps = new HashSet<>();

	@Override
	public List<Binding> getBindings(String queueName, String instanceId) {
		List<Binding> bindings = new ArrayList<>();

		bindings.add(bindingLifeCycle(queueName, "*." + Action.CREATED + "." + Type.SERVICE + ".#"));
		bindings.add(bindingLifeCycle(queueName, "*." + Action.UNDEPLOYED + "." + Type.SERVICE + ".#"));

		bindings.add(bindingCustom(queueName, "*." + EpsEvent.EPS_DYNAMIC_REQUESTED + "." + Type.SERVICE + ".*"));
		bindings.add(bindingCustom(queueName, "*." + EpsEvent.EPS_DYNAMIC_REMOVED + "." + Type.SERVICE + ".*"));
		bindings.add(bindingCustom(queueName, "*." + EpsEvent.EPS_SUPPORT_ASSIGNED + "." + Type.SERVICE + ".*"));
		bindings.add(bindingCustom(queueName, "*." + EpsEvent.EPS_REFRESHED + ".#"));

		return bindings;
	}

	@Override
	public void onLifecycleEvent(String serviceId, String groupId, Action action, CloudService service,
			Map<String, Transition> transitions, LifeCycleEvent event) throws Exception {

		if (infoService.isServiceOfDynamicEps(serviceId)) {

			if (action == Action.CREATED) {
				String staticDeplId = infoService.instanceIdOfStaticEps(Constants.SALSA_SERVICE_STATIC);

				manager.sendCustom(new CustomEvent(serviceId, serviceId, EpsEvent.EPS_SUPPORT_REQUESTED.toString(),
						staticDeplId, null));

			} else if (action == Action.UNDEPLOYED) {

				manager.sendLifeCycle(new LifeCycleEvent(serviceId, serviceId, Action.REMOVED));

			}
		}
	}

	@Override
	public void onCustomEvent(String serviceId, String groupId, String eventName, String epsId, String optionalMessage,
			Map<String, Transition> transitions, CustomEvent event) throws Exception {

		EpsEvent action = EpsEvent.valueOf(eventName);

		if (action == EpsEvent.EPS_DYNAMIC_REQUESTED && !event.getOrigin().equals(getId())) {

			String newServiceId = infoService.getOsuInstance(optionalMessage).getService().getId();

			manager.sendLifeCycle(new LifeCycleEvent(newServiceId, newServiceId, Action.CREATED));

		} else if (action == EpsEvent.EPS_SUPPORT_ASSIGNED && infoService.isServiceOfDynamicEps(serviceId)) {

			manager.sendLifeCycle(new LifeCycleEvent(serviceId, serviceId, Action.START));

		} else if (action == EpsEvent.EPS_DYNAMIC_REMOVED) {

			manager.sendLifeCycle(new LifeCycleEvent(serviceId, serviceId, Action.STOP));

		} else if (action == EpsEvent.EPS_REFRESHED) {

			refresh();
		}

	}

	@Override
	public void onExceptionEvent(ExceptionMessage msg) throws Exception {
		// not needed
	}

	public void refresh() throws EpsException {

		List<OfferedServiceUnit> osus = infoService.getOsus();

		// create static EPSes
		for (OfferedServiceUnit osu : osus) {

			if (isStaticEps(osu) && !staticEps.contains(osu.getId())) {

				try {

					Class<?> clazz = null;
					String ip = null;
					String port = null;

					for (Resource res : osu.getResources()) {
						switch (res.getType().getName()) {
						case Constants.ADAPTER_CLASS:
							clazz = Class.forName(res.getName());
							break;
						case Constants.IP:
							ip = res.getName();
							break;
						case Constants.PORT:
							port = res.getName();
							break;
						}
					}

					String epsId = infoService.createOsuInstance(osu.getId());
					EpsAdapterStatic adapter = (EpsAdapterStatic) context.getBean(clazz);
					adapter.start(epsId, ip, (port != null) ? Integer.valueOf(port) : null);

					staticEps.add(osu.getId());

				} catch (Exception e) {
					LOG.warn("{}", e);
				}
			}
		}
	}

	public static boolean isStaticEps(OfferedServiceUnit osu) {
		return osu.getType().equals(OsuType.EPS.toString()) && osu.getServiceTemplate() == null;
	}

}
