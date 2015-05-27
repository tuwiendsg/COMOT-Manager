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
package at.ac.tuwien.dsg.comot.m.core.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import at.ac.tuwien.dsg.comot.m.common.InfoClient;
import at.ac.tuwien.dsg.comot.m.common.enums.Action;
import at.ac.tuwien.dsg.comot.m.common.enums.EpsEvent;
import at.ac.tuwien.dsg.comot.m.common.test.UtilsT;
import at.ac.tuwien.dsg.comot.m.core.analytics.ElasticityAnalyzis;
import at.ac.tuwien.dsg.comot.m.core.analytics.ResultLine;
import at.ac.tuwien.dsg.comot.m.core.analytics.TimeAnalyzis;
import at.ac.tuwien.dsg.comot.m.core.spring.AppContextCoreInsertData;
import at.ac.tuwien.dsg.comot.m.core.test.utils.TeAgentAdapter;
import at.ac.tuwien.dsg.comot.m.core.test.utils.TeBean;
import at.ac.tuwien.dsg.comot.m.recorder.RecorderException;
import at.ac.tuwien.dsg.comot.m.recorder.repo.ChangeRepo;
import at.ac.tuwien.dsg.comot.m.recorder.revisions.RevisionApi;
import at.ac.tuwien.dsg.comot.model.provider.OfferedServiceUnit;
import at.ac.tuwien.dsg.comot.model.provider.OsuInstance;

public class EvalTest extends AbstractTest {

	private static final Logger LOG = LoggerFactory.getLogger(EvalTest.class);

	protected String templateId = "HelloElasticity";

	protected TeAgentAdapter agent;
	protected String staticDeplId;

	@Autowired
	private TimeAnalyzis engine;
	@Autowired
	protected RevisionApi revisionApi;
	@Autowired
	protected ChangeRepo changeRepo;
	@Autowired
	protected ElasticityAnalyzis elAnalysis;
	@Autowired
	protected TeBean testBean;

	int number = 5;

	int delay = 90; // seconds

	@Test
	public void oneSALSA() throws Exception {

		staticDeplId = infoService.instanceIdOfStaticEps(AppContextCoreInsertData.SALSA_SERVICE_STATIC);
		agent = new TeAgentAdapter("prototype", env.getProperty("uri.broker.host"));

		List<String> services = new ArrayList<>();

		for (int i = 0; i < number; i++) {
			services.add(coordinator.createService(infoService.getTemplate(templateId).getDescription()));

			agent.assertLifeCycleEvent(Action.CREATED);

			coordinator.assignSupportingOsu(services.get(i), staticDeplId);

			agent.assertCustomEvent(EpsEvent.EPS_SUPPORT_REQUESTED.toString());
			agent.assertCustomEvent(EpsEvent.EPS_SUPPORT_ASSIGNED.toString());
		}

		for (int i = 0; i < number; i++) {
			UtilsT.sleepSeconds(delay);
			coordinator.startService(services.get(i));
		}

		for (int i = 0; i < number; i++) {
			agent.waitForLifeCycleEvent(Action.DEPLOYED);
			agent.waitForLifeCycleEvent(Action.DEPLOYED);
			agent.waitForLifeCycleEvent(Action.DEPLOYED);
			agent.waitForLifeCycleEvent(Action.DEPLOYED);
		}

		LOG.info("FINISHED");

		results(services);

	}

	@Test
	public void multipleDynamicSALSA() throws Exception {

		staticDeplId = infoService.instanceIdOfStaticEps(AppContextCoreInsertData.SALSA_SERVICE_STATIC);
		agent = new TeAgentAdapter("prototype", env.getProperty("uri.broker.host"));

		// deploy dynamic salsas
		for (int i = 0; i < number; i++) {
			UtilsT.sleepSeconds(delay);

			OfferedServiceUnit salsaOsu = infoService.getOsu(AppContextCoreInsertData.SALSA_SERVICE_DYNAMIC);
			coordinator.createDynamicService(salsaOsu.getId());
		}

		// for (int i = 0; i < number; i++) {
		// agent.waitForCustomEvent(EpsEvent.EPS_DYNAMIC_CREATED.toString());
		// }

		LOG.info("ALL SALSAS CREATED");

		List<String> services = new ArrayList<>();
		List<OsuInstance> salsas = infoService.getEpsInstances(InfoClient.USER_MANAGED);

		UtilsT.sleepSeconds(300);

		// create services
		for (int i = 0; i < number; i++) {

			services.add(coordinator.createService(infoService.getTemplate(templateId).getDescription()));

			agent.assertLifeCycleEvent(Action.CREATED);

			coordinator.assignSupportingOsu(services.get(i), salsas.get(i).getId());

			agent.assertCustomEvent(EpsEvent.EPS_SUPPORT_REQUESTED.toString());
			agent.assertCustomEvent(EpsEvent.EPS_SUPPORT_ASSIGNED.toString());
		}

		// deploy services
		for (int i = 0; i < number; i++) {
			UtilsT.sleepSeconds(delay);
			coordinator.startService(services.get(i));
		}

		UtilsT.sleepSeconds(2000);

		// for (int i = 0; i < number; i++) {
		// agent.waitForLifeCycleEvent(Action.DEPLOYED);
		// agent.waitForLifeCycleEvent(Action.DEPLOYED);
		// agent.waitForLifeCycleEvent(Action.DEPLOYED);
		// agent.waitForLifeCycleEvent(Action.DEPLOYED);
		// }

		LOG.info("FINISHED");

		results(services);

	}

	public void results(List<String> services) throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, ClassNotFoundException, RecorderException {

		Double stagingSum = 0.0;
		int stagingNr = 0;
		Double totalSum = 0.0;
		int totalNr = 0;

		Double eventTimeSum = 0.0;
		int eventTimeNr = 0;

		for (int i = 0; i < number; i++) {
			for (ResultLine line : engine.deploymentEvents(services.get(i))) {

				if (line.getStage().equals("STAGING")) {
					stagingSum = stagingSum + line.getLength();
					stagingNr++;
				}

				if (line.getStage().equals("SUM")) {
					totalSum = totalSum + line.getLength();
					totalNr++;
				}
				LOG.info(line.toString());
			}

			eventTimeSum = eventTimeSum + testBean.computeDuration(services.get(i));
			eventTimeNr++;
		}

		LOG.info("STAGING: {}", stagingSum / stagingNr);
		LOG.info("TOTAL: {}", totalSum / totalNr);
		LOG.info("AVG eventTime: {} ", eventTimeSum / eventTimeNr);
	}
}
