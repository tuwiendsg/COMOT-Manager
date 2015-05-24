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
package at.ac.tuwien.dsg.comot.m.adapter.general;

import java.util.List;
import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Binding.DestinationType;

import at.ac.tuwien.dsg.comot.m.common.Constants;
import at.ac.tuwien.dsg.comot.m.common.enums.Action;
import at.ac.tuwien.dsg.comot.m.common.event.CustomEvent;
import at.ac.tuwien.dsg.comot.m.common.event.LifeCycleEvent;
import at.ac.tuwien.dsg.comot.m.common.event.state.ExceptionMessage;
import at.ac.tuwien.dsg.comot.m.common.event.state.Transition;
import at.ac.tuwien.dsg.comot.model.devel.structure.CloudService;

public abstract class Processor implements IProcessor {

	protected IManager manager;
	protected String participantId;

	@Override
	public void init(IManager dispatcher, String participantId) {
		this.manager = dispatcher;
		this.participantId = participantId;

	}

	public IManager getManager() {
		return manager;
	}

	public static Binding bindingLifeCycle(String queueName, String key) {
		return new Binding(queueName, DestinationType.QUEUE, Constants.EXCHANGE_LIFE_CYCLE,
				key, null);
	}

	public static Binding bindingCustom(String queueName, String key) {
		return new Binding(queueName, DestinationType.QUEUE, Constants.EXCHANGE_CUSTOM_EVENT,
				key, null);
	}

	public static Binding bindingException(String queueName, String key) {
		return new Binding(queueName, DestinationType.QUEUE, Constants.EXCHANGE_EXCEPTIONS,
				key, null);
	}

	@Override
	public abstract List<Binding> getBindings(String queueName, String instanceId);

	@Override
	public abstract void onLifecycleEvent(
			String serviceId,
			String groupId,
			Action action,
			CloudService service,
			Map<String, Transition> transitions,
			LifeCycleEvent event) throws Exception;

	@Override
	public abstract void onCustomEvent(
			String serviceId,
			String groupId,
			String eventName,
			String epsId,
			String optionalMessage,
			Map<String, Transition> transitions,
			CustomEvent event) throws Exception;

	@Override
	public abstract void onExceptionEvent(ExceptionMessage msg) throws Exception;

	public String logId() {
		return manager.logId();
	}

	public String getId() {
		return participantId;
	}

}
