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

package org.apache.ambari.view.slider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.slider.clients.AmbariClient;
import org.apache.ambari.view.slider.clients.AmbariCluster;
import org.apache.ambari.view.slider.clients.AmbariClusterInfo;
import org.apache.ambari.view.slider.clients.AmbariServiceInfo;
import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SliderAppsViewControllerImpl implements SliderAppsViewController {

	private static final Logger logger = Logger
	    .getLogger(SliderAppsViewControllerImpl.class);
	@Inject
	private ViewContext viewContext;
	@Inject
	private AmbariClient ambariClient;

	@Override
	public ViewStatus getViewStatus() {
		ViewStatus status = new ViewStatus();
		List<String> viewErrors = new ArrayList<String>();

		AmbariClusterInfo clusterInfo = ambariClient.getClusterInfo();
		if (clusterInfo != null) {
			AmbariCluster cluster = ambariClient.getCluster(clusterInfo);
			List<AmbariServiceInfo> services = cluster.getServices();
			if (services != null && !services.isEmpty()) {
				AmbariServiceInfo hdfsService = null, yarnService = null, zkService = null;
				for (AmbariServiceInfo service : services) {
					if ("HDFS".equals(service.getId())) {
						hdfsService = service;
					} else if ("YARN".equals(service.getId())) {
						yarnService = service;
					} else if ("ZOOKEEPER".equals(service.getId())) {
						zkService = service;
					}
				}
				if (hdfsService == null) {
					viewErrors.add("Slider applications view requires HDFS service");
				} else {
					if (!hdfsService.isStarted()) {
						viewErrors
						    .add("Slider applications view requires HDFS service to be started");
					}
				}
				if (yarnService == null) {
					viewErrors.add("Slider applications view requires YARN service");
				} else {
					if (!yarnService.isStarted()) {
						viewErrors
						    .add("Slider applications view requires YARN service to be started");
					}
				}
				if (zkService == null) {
					viewErrors.add("Slider applications view requires ZooKeeper service");
				} else {
					if (!zkService.isStarted()) {
						viewErrors
						    .add("Slider applications view requires ZooKeeper service to be started");
					}
				}
			} else {
				viewErrors
				    .add("Slider applications view is unable to locate any services");
			}
			// Check security
			if (cluster.getDesiredConfigs() != null
			    && cluster.getDesiredConfigs().containsKey("global")) {
				Map<String, String> globalConfig = ambariClient.getConfiguration(
				    clusterInfo, "global", cluster.getDesiredConfigs().get("global"));
				if (globalConfig != null
				    && globalConfig.containsKey("security_enabled")) {
					String securityValue = globalConfig.get("security_enabled");
					if (Boolean.valueOf(securityValue)) {
						viewErrors
						    .add("Slider applications view cannot be rendered in secure mode");
					}
				} else {
					viewErrors
					    .add("Slider applications view is unable to determine the security status of the cluster");
				}
			} else {
				viewErrors
				    .add("Slider applications view is unable to determine the security status of the cluster");
			}
		} else {
			viewErrors.add("Slider applications view requires a cluster");
		}
		status.setVersion(SliderAppsConfiguration.INSTANCE.getVersion());
		status.setViewEnabled(viewErrors.size() < 1);
		status.setViewErrors(viewErrors);
		return status;
	}

	@Override
	public SliderApp getSliderApp(String applicationId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<SliderApp> getSliderApps() {
		// TODO Auto-generated method stub
		return null;
	}

}
