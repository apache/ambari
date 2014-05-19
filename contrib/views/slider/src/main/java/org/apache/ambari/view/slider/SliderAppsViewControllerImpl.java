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
import org.apache.ambari.view.slider.rest.client.AmbariCluster;
import org.apache.ambari.view.slider.rest.client.AmbariClusterInfo;
import org.apache.ambari.view.slider.rest.client.AmbariHttpClient;
import org.apache.ambari.view.slider.rest.client.AmbariService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SliderAppsViewControllerImpl implements SliderAppsViewController {

	@Inject
	private ViewContext viewContext;
	private AmbariHttpClient ambariClient;

	private AmbariHttpClient getAmbariClient() {
		// TODO Calculate Ambari location dynamically
		if (ambariClient == null)
			ambariClient = new AmbariHttpClient("http://localhost:8080",
			    viewContext.getUsername(), "admin");
		return ambariClient;
	}

	@Override
	public ViewStatus getViewStatus() {
		ViewStatus status = new ViewStatus();
		List<String> viewErrors = new ArrayList<String>();

		AmbariHttpClient client = getAmbariClient();
		AmbariClusterInfo clusterInfo = client.getClusterInfo();
		if (clusterInfo != null) {
			AmbariCluster cluster = client.getCluster(clusterInfo);
			List<String> services = cluster.getServices();
			if (services != null && services.size() > 0) {
				boolean zkFound = services.indexOf("ZOOKEEPER") > -1;
				boolean hdfsFound = services.indexOf("HDFS") > -1;
				boolean yarnFound = services.indexOf("YARN") > -1;
				if (!hdfsFound) {
					viewErrors.add("Slider applications view requires HDFS service");
				} else {
					AmbariService service = client.getService(clusterInfo, "HDFS");
					if (service != null) {
						if (!service.isStarted()) {
							viewErrors
							    .add("Slider applications view requires HDFS service to be started");
						}
					}
				}
				if (!yarnFound) {
					viewErrors.add("Slider applications view requires YARN service");
				} else {
					AmbariService service = client.getService(clusterInfo, "YARN");
					if (service != null) {
						if (!service.isStarted()) {
							viewErrors
							    .add("Slider applications view requires YARN service to be started");
						}
					}
				}
				if (!zkFound) {
					viewErrors.add("Slider applications view requires ZooKeeper service");
				} else {
					AmbariService service = client.getService(clusterInfo, "ZOOKEEPER");
					if (service != null) {
						if (!service.isStarted()) {
							viewErrors
							    .add("Slider applications view requires ZooKeeper service to be started");
						}
					}
				}
			} else {
				viewErrors
				    .add("Slider applications view is unable to locate any services");
			}
			// Check security
			if (cluster.getDesiredConfigs() != null
			    && cluster.getDesiredConfigs().containsKey("global")) {
				Map<String, String> globalConfig = client.getConfiguration(clusterInfo,
				    "global", cluster.getDesiredConfigs().get("global"));
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
