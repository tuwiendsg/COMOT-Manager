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
package at.ac.tuwien.dsg.comot.m.recorder;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import at.ac.tuwien.dsg.comot.model.AppContextModel;

@EnableAsync
@Configuration
@EnableTransactionManagement
@EnableNeo4jRepositories
@Import({ AppContextModel.class })
@ComponentScan
public class AppContextServrec extends Neo4jConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(AppContextServrec.class);

	public static final String IMPERMANENT_NEO4J_DB = "IMPERMANENT_NEO4J_DB";
	public static final String EMBEDDED_NEO4J_DB = "EMBEDDED_NEO4J_DB";

	@Autowired
	protected GraphDatabaseService db;

	public AppContextServrec() {
		setBasePackage("at.ac.tuwien.dsg.comot.m.recorder");
	}

	@Bean
	public ExecutionEngine executionEngine() {
		LOG.info("GraphDatabaseService:  {}", db);
		return new ExecutionEngine(db);
	}

}