/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.slider.clients;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.State;
import org.apache.log4j.Logger;

import com.google.inject.Singleton;

@Singleton
public class AmbariInternalClient implements AmbariClient {

	private static final Logger logger = Logger
	    .getLogger(AmbariInternalClient.class);

	@Override
	public AmbariCluster getCluster(AmbariClusterInfo clusterInfo) {
		ClusterController clusterController = ClusterControllerHelper
		    .getClusterController();
		try {
			EqualsPredicate<String> clusterPredicate = new EqualsPredicate<String>(
			    "Clusters/cluster_name", clusterInfo.getName());
			Set<Resource> clusterResources = clusterController.getResources(
			    Resource.Type.Cluster, PropertyHelper.getReadRequest(),
			    clusterPredicate);
			if (!clusterResources.isEmpty()) {
				Resource clusterResource = clusterResources.iterator().next();
				AmbariCluster cluster = new AmbariCluster();
				cluster.setName(clusterResource.getPropertyValue(
				    "Clusters/cluster_name").toString());
				cluster.setVersion(clusterResource.getPropertyValue("Clusters/version")
				    .toString());
				Map<String, String> desiredConfigsMap = new HashMap<String, String>();
				Map<String, Object> desiredConfigsMapResource = clusterResource
				    .getPropertiesMap().get("Clusters/desired_configs");
				for (Map.Entry<String, Object> siteEntry : desiredConfigsMapResource
				    .entrySet()) {
					desiredConfigsMap.put(siteEntry.getKey(),
					    ((DesiredConfig) siteEntry.getValue()).getTag());
				}
				cluster.setDesiredConfigs(desiredConfigsMap);

				EqualsPredicate<String> serviceClusterPredicate = new EqualsPredicate<String>(
				    "ServiceInfo/cluster_name", cluster.getName());
				EqualsPredicate<String> hostClusterPredicate = new EqualsPredicate<String>(
				    "Hosts/cluster_name", cluster.getName());
				Set<Resource> serviceResources = clusterController.getResources(
				    Resource.Type.Service, PropertyHelper.getReadRequest(),
				    serviceClusterPredicate);
				Set<Resource> hostResources = clusterController.getResources(
				    Resource.Type.Host, PropertyHelper.getReadRequest(),
				    hostClusterPredicate);
				List<AmbariServiceInfo> servicesList = new ArrayList<AmbariServiceInfo>();
				List<AmbariHostInfo> hostsList = new ArrayList<AmbariHostInfo>();
				for (Resource serviceResource : serviceResources) {
					AmbariServiceInfo service = new AmbariServiceInfo();
					service.setId(serviceResource.getPropertyValue(
					    "ServiceInfo/service_name").toString());
					service.setStarted(State.STARTED.toString().equals(
					    serviceResource.getPropertyValue("ServiceInfo/state")));
					service.setMaintenanceMode("ON".equals(serviceResource
					    .getPropertyValue("ServiceInfo/maintenance_state")));
					servicesList.add(service);
				}
				for (Resource hostResource : hostResources) {
					AmbariHostInfo host = new AmbariHostInfo();
					host.setHostName(hostResource.getPropertyValue("Hosts/host_name")
					    .toString());
					hostsList.add(host);
				}
				cluster.setServices(servicesList);
				cluster.setHosts(hostsList);
				return cluster;
			}
		} catch (UnsupportedPropertyException e) {
			logger.warn(
			    "Unable to determine Ambari cluster details - "
			        + clusterInfo.getName(), e);
			throw new RuntimeException(e.getMessage(), e);
		} catch (NoSuchResourceException e) {
			logger.warn(
			    "Unable to determine Ambari cluster details - "
			        + clusterInfo.getName(), e);
			throw new RuntimeException(e.getMessage(), e);
		} catch (NoSuchParentResourceException e) {
			logger.warn(
			    "Unable to determine Ambari cluster details - "
			        + clusterInfo.getName(), e);
			throw new RuntimeException(e.getMessage(), e);
		} catch (SystemException e) {
			logger.warn(
			    "Unable to determine Ambari cluster details - "
			        + clusterInfo.getName(), e);
			throw new RuntimeException(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public AmbariClusterInfo getClusterInfo() {
		ClusterController clusterController = ClusterControllerHelper
		    .getClusterController();
		try {
			Set<Resource> resources = clusterController.getResources(
			    Resource.Type.Cluster, PropertyHelper.getReadRequest(), null);
			if (resources.size() > 0) {
				Resource clusterResource = resources.iterator().next();
				AmbariClusterInfo clusterInfo = new AmbariClusterInfo();
				clusterInfo.setName(clusterResource.getPropertyValue(
				    "Clusters/cluster_name").toString());
				clusterInfo.setVersion(clusterResource.getPropertyValue(
				    "Clusters/version").toString());
				return clusterInfo;
			}
		} catch (UnsupportedPropertyException e) {
			logger.warn("Unable to determine Ambari cluster", e);
			throw new RuntimeException(e.getMessage(), e);
		} catch (NoSuchResourceException e) {
			logger.warn("Unable to determine Ambari cluster", e);
			throw new RuntimeException(e.getMessage(), e);
		} catch (NoSuchParentResourceException e) {
			logger.warn("Unable to determine Ambari cluster", e);
			throw new RuntimeException(e.getMessage(), e);
		} catch (SystemException e) {
			logger.warn("Unable to determine Ambari cluster", e);
			throw new RuntimeException(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public Map<String, String> getConfiguration(AmbariClusterInfo cluster,
	    String configType, String configTag) {
		ClusterController clusterController = ClusterControllerHelper
		    .getClusterController();
		try {
			EqualsPredicate<String> clusterPredicate = new EqualsPredicate<String>(
			    "Config/cluster_name", cluster.getName());
			EqualsPredicate<String> typePredicate = new EqualsPredicate<String>(
			    "type", configType);
			EqualsPredicate<String> tagPredicate = new EqualsPredicate<String>("tag",
			    configTag);
			AndPredicate typeTagPredicate = new AndPredicate(typePredicate,
			    tagPredicate);
			AndPredicate configsPredicate = new AndPredicate(clusterPredicate,
			    typeTagPredicate);

			Set<Resource> configResources = clusterController.getResources(
			    Resource.Type.Configuration, PropertyHelper.getReadRequest(),
			    configsPredicate);
			if (!configResources.isEmpty()) {
				Resource configResource = configResources.iterator().next();
				Map<String, String> configs = new HashMap<String, String>();
				Object props = configResource.getPropertiesMap().get("properties");
				if (props instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, String> propsMap = (Map<String, String>) props;
					configs.putAll(propsMap);
				}
				return configs;
			}
		} catch (UnsupportedPropertyException e) {
			logger.warn("Unable to determine Ambari cluster configuration", e);
			throw new RuntimeException(e.getMessage(), e);
		} catch (NoSuchResourceException e) {
			logger.warn("Unable to determine Ambari cluster configuration", e);
			throw new RuntimeException(e.getMessage(), e);
		} catch (NoSuchParentResourceException e) {
			logger.warn("Unable to determine Ambari cluster configuration", e);
			throw new RuntimeException(e.getMessage(), e);
		} catch (SystemException e) {
			logger.warn("Unable to determine Ambari cluster configuration", e);
			throw new RuntimeException(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public AmbariService getService(AmbariClusterInfo clusterInfo,
	    String serviceId) {
		ClusterController clusterController = ClusterControllerHelper
		    .getClusterController();
		try {
			EqualsPredicate<String> clusterPredicate = new EqualsPredicate<String>(
			    "ServiceInfo/cluster_name", clusterInfo.getName());
			EqualsPredicate<String> servicePredicate = new EqualsPredicate<String>(
			    "ServiceInfo/service_name", serviceId);
			AndPredicate andPredicate = new AndPredicate(clusterPredicate,
			    servicePredicate);
			Set<Resource> serviceResources = clusterController.getResources(
			    Resource.Type.Service, PropertyHelper.getReadRequest(), andPredicate);
			if (!serviceResources.isEmpty()) {
				Resource serviceResource = serviceResources.iterator().next();
				AmbariService service = new AmbariService();
				service.setId(serviceResource.getPropertyValue(
				    "ServiceInfo/service_name").toString());
				service.setStarted(State.STARTED.toString().equals(
				    serviceResource.getPropertyValue("ServiceInfo/state")));
				service.setMaintenanceMode("ON".equals(serviceResource
				    .getPropertyValue("ServiceInfo/maintenance_state")));
				// Components
				Map<String, List<AmbariHostComponent>> componentsMap = new HashMap<String, List<AmbariHostComponent>>();
				service.setComponentsToHostComponentsMap(componentsMap);
				clusterPredicate = new EqualsPredicate<String>(
				    "ServiceComponentInfo/cluster_name", clusterInfo.getName());
				servicePredicate = new EqualsPredicate<String>(
				    "ServiceComponentInfo/service_name", serviceId);
				andPredicate = new AndPredicate(clusterPredicate, servicePredicate);
				Set<Resource> componentResources = clusterController.getResources(
				    Resource.Type.Component, PropertyHelper.getReadRequest(),
				    andPredicate);
				if (!componentResources.isEmpty()) {
					for (Resource componentResouce : componentResources) {
						List<AmbariHostComponent> hostComponents = new ArrayList<AmbariHostComponent>();
						String componentName = componentResouce.getPropertyValue(
						    "ServiceComponentInfo/component_name").toString();
						componentsMap.put(componentName, hostComponents);
						clusterPredicate = new EqualsPredicate<String>(
						    "HostRoles/cluster_name", clusterInfo.getName());
						EqualsPredicate<String> componentPredicate = new EqualsPredicate<String>(
						    "HostRoles/component_name", componentName);
						andPredicate = new AndPredicate(clusterPredicate,
						    componentPredicate);
						Set<Resource> hostComponentResources = clusterController
						    .getResources(Resource.Type.HostComponent,
						        PropertyHelper.getReadRequest(), andPredicate);
						if (!hostComponentResources.isEmpty()) {
							for (Resource hostComponentResource : hostComponentResources) {
								AmbariHostComponent hc = new AmbariHostComponent();
								hc.setHostName(hostComponentResource.getPropertyValue(
								    "HostRoles/host_name").toString());
								hc.setName(hostComponentResource.getPropertyValue(
								    "HostRoles/component_name").toString());
								hc.setStarted(State.STARTED.toString().equals(
								    hostComponentResource.getPropertyValue("HostRoles/state")
								        .toString()));
								hostComponents.add(hc);
							}
						}
					}
				}
				return service;
			}
		} catch (UnsupportedPropertyException e) {
			logger.warn("Unable to determine service details - " + serviceId, e);
			throw new RuntimeException(e.getMessage(), e);
		} catch (NoSuchResourceException e) {
			logger.warn("Unable to determine service details - " + serviceId, e);
			throw new RuntimeException(e.getMessage(), e);
		} catch (NoSuchParentResourceException e) {
			logger.warn("Unable to determine service details - " + serviceId, e);
			throw new RuntimeException(e.getMessage(), e);
		} catch (SystemException e) {
			logger.warn("Unable to determine service details - " + serviceId, e);
			throw new RuntimeException(e.getMessage(), e);
		}
		return null;
	}
}
