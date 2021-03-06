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
package at.ac.tuwien.dsg.comot.m.cs.mapper.orika;

import java.util.Date;

import javax.annotation.PostConstruct;

import ma.glasnost.orika.CustomConverter;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.ConverterFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import ma.glasnost.orika.metadata.Property;
import ma.glasnost.orika.metadata.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import at.ac.tuwien.dsg.comot.m.common.model.monitoring.ElementMonitoring;
import at.ac.tuwien.dsg.comot.m.common.model.monitoring.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement.MonitoredElementLevel;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;

@Component
public class MelaOutputOrika {

	private static final Logger LOG = LoggerFactory.getLogger(MelaOutputOrika.class);

	protected MapperFacade facade;

	@PostConstruct
	public void build() {

		MapperFactory mapperFactory = new DefaultMapperFactory.Builder().build();
		ConverterFactory converterFactory = mapperFactory.getConverterFactory();

		Property.Builder.propertyFor(MonitoredElementMonitoringSnapshot.class, "employment")
				.getter("hashCode()");

		mapperFactory.classMap(ElementMonitoring.class, MonitoredElementMonitoringSnapshot.class)
				.field("id", "monitoredElement.id")
				.fieldBToA("children", "children")
				.fieldBToA("monitoredElement.level", "type")
				.customize(
						new CustomMapper<ElementMonitoring, MonitoredElementMonitoringSnapshot>() {
							@Override
							public void mapBtoA(MonitoredElementMonitoringSnapshot snapshot,
									ElementMonitoring element, MappingContext context) {

								// has wrong getter
								element.setHashCode(snapshot.hashCode());
								if (snapshot.getTimestamp() != null) {
									element.setTimestamp(new Date(Long.valueOf(snapshot.getTimestamp())));
								}
								MetricValue melaValue;

								for (at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric melaMetric : snapshot
										.getMonitoredData().keySet()) {

									melaValue = snapshot.getMonitoredData().get(melaMetric);

									Metric metric = facade.map(melaMetric, Metric.class);
									facade.map(melaValue, metric);

									element.addMetric(metric);
								}
							}
						})
				.register();

		mapperFactory.classMap(Metric.class, at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric.class)
				.field("name", "name")
				.field("measurementUnit", "measurementUnit")
				.field("metricType", "type")
				.register();

		mapperFactory.classMap(Metric.class, MetricValue.class)
				.field("value", "value")
				.field("valueType", "valueType")
				.register();

		converterFactory.registerConverter(new MelaDataTypeConverter());
		facade = mapperFactory.getMapperFacade();

	}

	public MapperFacade get() {
		return facade;
	}

	public class MelaDataTypeConverter extends CustomConverter<MonitoredElementLevel, ElementMonitoring.Type> {
		@Override
		public ElementMonitoring.Type convert(MonitoredElementLevel source,
				Type<? extends ElementMonitoring.Type> destinationType) {

			switch (source) {
			case SERVICE:
				return ElementMonitoring.Type.SERVICE;
			case SERVICE_TOPOLOGY:
				return ElementMonitoring.Type.TOPOLOGY;
			case SERVICE_UNIT:
				return ElementMonitoring.Type.UNIT;
			case VM:
				return ElementMonitoring.Type.VM;
			default:
				throw new IllegalArgumentException(
						"Unexpected value "
								+ source
								+ " of enum at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement.MonitoredElementLevel");
			}
		}
	}

}
