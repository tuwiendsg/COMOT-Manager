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
package at.ac.tuwien.dsg.comot.m.core.analytics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import at.ac.tuwien.dsg.comot.model.SyblDirective;
import at.ac.tuwien.dsg.csdg.elasticityInformation.elasticityRequirements.Constraint;
import at.ac.tuwien.dsg.csdg.elasticityInformation.elasticityRequirements.Strategy;
import at.ac.tuwien.dsg.csdg.outputProcessing.eventsNotification.ActionPlanEvent;
import at.ac.tuwien.dsg.csdg.outputProcessing.eventsNotification.CustomEvent;
import at.ac.tuwien.dsg.csdg.outputProcessing.eventsNotification.IEvent;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ElasticPlanReport {

	IEvent.Stage currentStage;
	Long timestamp;
	Long changeTimestamp;

	List<Constraint> constraints;
	List<Strategy> strategies;
	Set<SyblDirective> directives;
	List<ActionReport> actions;
	List<CustomEventReport> customEvents;

	public ElasticPlanReport() {

	}

	public ElasticPlanReport(ActionPlanEvent plan, Long timestamp, Long changeTimestamp) {
		super();
		constraints = plan.getConstraints();
		strategies = plan.getStrategies();
		this.timestamp = timestamp;
		this.changeTimestamp = changeTimestamp;
		currentStage = IEvent.Stage.START;
	}

	public void addActionEvent(ActionReport event) {

		if (actions == null) {
			actions = new ArrayList<>();
		}
		actions.add(event);
	}

	public void addDirective(SyblDirective directive) {

		if (directives == null) {
			directives = new HashSet<>();
		}
		directives.add(directive);
	}

	public void addCustomEvent(CustomEvent event, Long timestamp) {

		if (customEvents == null) {
			customEvents = new ArrayList<>();
		}
		customEvents.add(new CustomEventReport(event, timestamp));
	}

	public List<ActionReport> getActions() {
		return actions;
	}

	public void setActions(List<ActionReport> actions) {
		this.actions = actions;
	}

	public List<CustomEventReport> getCustomEvents() {
		return customEvents;
	}

	public void setCustomEvents(List<CustomEventReport> customEvents) {
		this.customEvents = customEvents;
	}

	public IEvent.Stage getCurrentStage() {
		return currentStage;
	}

	public void setCurrentStage(IEvent.Stage currentStage) {
		this.currentStage = currentStage;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	public Long getChangeTimestamp() {
		return changeTimestamp;
	}

	public void setChangeTimestamp(Long changeTimestamp) {
		this.changeTimestamp = changeTimestamp;
	}

	public Set<SyblDirective> getDirectives() {
		return directives;
	}

	public void setDirectives(Set<SyblDirective> directives) {
		this.directives = directives;
	}

	public List<Constraint> getConstraints() {
		return constraints;
	}

	public void setConstraints(List<Constraint> constraints) {
		this.constraints = constraints;
	}

	public List<Strategy> getStrategies() {
		return strategies;
	}

	public void setStrategies(List<Strategy> strategies) {
		this.strategies = strategies;
	}

}
