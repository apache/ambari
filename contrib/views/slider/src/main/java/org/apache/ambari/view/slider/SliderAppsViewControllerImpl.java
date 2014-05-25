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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipException;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.slider.clients.AmbariClient;
import org.apache.ambari.view.slider.clients.AmbariCluster;
import org.apache.ambari.view.slider.clients.AmbariClusterInfo;
import org.apache.ambari.view.slider.clients.AmbariHostComponent;
import org.apache.ambari.view.slider.clients.AmbariService;
import org.apache.ambari.view.slider.clients.AmbariServiceInfo;
import org.apache.ambari.view.slider.rest.client.SliderAppMasterClient;
import org.apache.ambari.view.slider.rest.client.SliderAppMasterClient.SliderAppMasterData;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.ApplicationNotFoundException;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.log4j.Logger;
import org.apache.slider.api.ClusterDescription;
import org.apache.slider.client.SliderClient;
import org.apache.slider.common.SliderKeys;
import org.apache.slider.common.tools.SliderFileSystem;
import org.apache.slider.core.exceptions.UnknownApplicationInstanceException;
import org.apache.slider.core.main.LauncherExitCodes;
import org.apache.slider.providers.agent.application.metadata.Component;
import org.apache.slider.providers.agent.application.metadata.Metainfo;
import org.apache.slider.providers.agent.application.metadata.MetainfoParser;
import org.apache.slider.providers.agent.application.metadata.Service;
import org.apache.tools.zip.ZipFile;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
	private List<SliderAppType> appTypes;

	private String getAppsFolderPath() {
		return viewContext
		    .getAmbariProperty(org.apache.ambari.server.configuration.Configuration.RESOURCES_DIR_KEY)
		    + "/apps";
	}

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

	private AmbariCluster getAmbariCluster() {
		AmbariClusterInfo clusterInfo = ambariClient.getClusterInfo();
		if (clusterInfo != null)
			return ambariClient.getCluster(clusterInfo);
		return null;
	}

	@Override
	public SliderApp getSliderApp(String applicationId, Set<String> properties)
	    throws YarnException, IOException {
		if (applicationId != null) {
			int index = applicationId.indexOf('_');
			if (index > -1 && index < applicationId.length() - 1) {
				ApplicationId appId = ApplicationId.newInstance(
				    Long.parseLong(applicationId.substring(0, index)),
				    Integer.parseInt(applicationId.substring(index + 1)));
				ClassLoader currentClassLoader = Thread.currentThread()
				    .getContextClassLoader();
				Thread.currentThread().setContextClassLoader(
				    getClass().getClassLoader());
				try {
					SliderClient sliderClient = getSliderClient();
					ApplicationReport yarnApp = sliderClient.getApplicationReport(appId);
					return createSliderAppObject(yarnApp, properties, sliderClient);
				} finally {
					Thread.currentThread().setContextClassLoader(currentClassLoader);
				}
			}
		}
		return null;
	}

	private SliderApp createSliderAppObject(ApplicationReport yarnApp,
	    Set<String> properties, SliderClient sliderClient) {
		if (yarnApp == null)
			return null;

		SliderApp app = new SliderApp();
		app.setState(yarnApp.getYarnApplicationState().name());

		// Valid Slider App?
		// We want all Slider apps except the ones which properly finished.
		if (YarnApplicationState.FINISHED.equals(yarnApp.getYarnApplicationState())) {
			try {
				if (sliderClient.actionExists(yarnApp.getName(), false) == LauncherExitCodes.EXIT_SUCCESS)
					app.setState(SliderApp.STATE_FROZEN);
			} catch (UnknownApplicationInstanceException e) {
				return null; // Application not in HDFS - means it is not frozen
			} catch (YarnException e) {
				logger.warn(
				    "Unable to determine frozen state for " + yarnApp.getName(), e);
				return null;
			} catch (IOException e) {
				logger.warn(
				    "Unable to determine frozen state for " + yarnApp.getName(), e);
				return null;
			}
		}

		app.setId(Long.toString(yarnApp.getApplicationId().getClusterTimestamp())
		    + "_" + Integer.toString(yarnApp.getApplicationId().getId()));
		app.setName(yarnApp.getName());
		app.setUser(yarnApp.getUser());
		app.setDiagnostics(yarnApp.getDiagnostics());
		app.setYarnId(yarnApp.getApplicationId().toString());
		app.setType(yarnApp.getApplicationType());
		app.setStartTime(yarnApp.getStartTime());
		app.setEndTime(yarnApp.getFinishTime());
		if (properties != null && !properties.isEmpty()) {
			SliderAppMasterClient sliderAppClient = yarnApp.getTrackingUrl() == null ? null
			    : new SliderAppMasterClient(yarnApp.getTrackingUrl());
			SliderAppMasterData appMasterData = null;
			Map<String, String> quickLinks = new HashMap<String, String>();
			for (String property : properties) {
				if ("RUNNING".equals(app.getState())) {
					if (sliderAppClient != null) {
						if (appMasterData == null)
							appMasterData = sliderAppClient.getAppMasterData();
						if ("urls".equals(property.toLowerCase())) {
							if (quickLinks.isEmpty())
								quickLinks = sliderAppClient
								    .getQuickLinks(appMasterData.publisherUrl);
							app.setUrls(quickLinks);
						} else if ("configs".equals(property.toLowerCase())) {
							Map<String, Map<String, String>> configs = sliderAppClient
							    .getConfigs(appMasterData.publisherUrl);
							app.setConfigs(configs);
						} else if ("jmx".equals(property.toLowerCase())) {
							if (quickLinks.isEmpty())
								quickLinks = sliderAppClient
								    .getQuickLinks(appMasterData.publisherUrl);
							if (quickLinks != null && quickLinks.containsKey("JMX")) {
								String jmxUrl = quickLinks.get("JMX");
								app.setJmx(sliderAppClient.getJmx(jmxUrl));
							}
							Map<String, Map<String, String>> configs = sliderAppClient
							    .getConfigs(appMasterData.publisherUrl);
							app.setConfigs(configs);
						} else if ("components".equals(property.toLowerCase())) {
							try {
								System.setProperty(SliderKeys.HADOOP_USER_NAME, "yarn");
								ClusterDescription description = sliderClient
								    .getClusterDescription(yarnApp.getName());
								if (description != null && description.status != null
								    && !description.status.isEmpty()) {
									Map<String, SliderAppComponent> componentTypeMap = new HashMap<String, SliderAppComponent>();
									for (Entry<String, Object> e : description.status.entrySet()) {
										@SuppressWarnings("unchecked")
										Map<String, Map<String, Map<String, Object>>> componentsObj = (Map<String, Map<String, Map<String, Object>>>) e
										    .getValue();
										boolean isLive = "live".equals(e.getKey());
										for (Entry<String, Map<String, Map<String, Object>>> componentEntry : componentsObj
										    .entrySet()) {
											SliderAppComponent appComponent = componentTypeMap
											    .get(componentEntry.getKey());
											if (appComponent == null) {
												appComponent = new SliderAppComponent();
												appComponent.setComponentName(componentEntry.getKey());
												appComponent
												    .setActiveContainers(new HashMap<String, Map<String, String>>());
												appComponent
												    .setCompletedContainers(new HashMap<String, Map<String, String>>());
												componentTypeMap.put(componentEntry.getKey(),
												    appComponent);
											}
											for (Entry<String, Map<String, Object>> containerEntry : componentEntry
											    .getValue().entrySet()) {
												Map<String, String> containerDataMap = new HashMap<String, String>();
												String containerId = containerEntry.getKey();
												Map<String, Object> containerValues = containerEntry
												    .getValue();
												for (String containerProperty : containerValues
												    .keySet()) {
													Object containerPropertyValue = containerValues
													    .get(containerProperty);
													containerDataMap.put(containerProperty,
													    containerPropertyValue.toString());
												}
												if (isLive)
													appComponent.getActiveContainers().put(containerId,
													    containerDataMap);
												else
													appComponent.getCompletedContainers().put(
													    containerId, containerDataMap);
											}
											appComponent.setInstanceCount(appComponent
											    .getActiveContainers().size()
											    + appComponent.getCompletedContainers().size());
										}
									}
									app.setComponents(componentTypeMap);
								}
							} catch (UnknownApplicationInstanceException e) {
								logger.warn(
								    "Unable to determine app components for "
								        + yarnApp.getName(), e);
							} catch (YarnException e) {
								logger.warn(
								    "Unable to determine app components for "
								        + yarnApp.getName(), e);
								throw new RuntimeException(e.getMessage(), e);
							} catch (IOException e) {
								logger.warn(
								    "Unable to determine app components for "
								        + yarnApp.getName(), e);
								throw new RuntimeException(e.getMessage(), e);
							}
						}
					}
				}
			}
		}
		return app;
	}

	/**
	 * Creates a new {@link SliderClient} initialized with appropriate
	 * configuration. If configuration was not determined, <code>null</code> is
	 * returned.
	 * 
	 * @return
	 */
	protected SliderClient getSliderClient() {
		Configuration sliderClientConfiguration = getSliderClientConfiguration();
		if (sliderClientConfiguration != null) {
			SliderClient client = new SliderClient() {
				@Override
				public String getUsername() throws IOException {
					return null;
				}

				@Override
				protected void serviceInit(Configuration conf) throws Exception {
					super.serviceInit(conf);
					// Override the default FS client to set the super user.
					FileSystem fs = FileSystem.get(FileSystem.getDefaultUri(getConfig()),
					    getConfig(), "yarn");
					SliderFileSystem fileSystem = new SliderFileSystem(fs, getConfig());
					Field fsField = SliderClient.class
					    .getDeclaredField("sliderFileSystem");
					fsField.setAccessible(true);
					fsField.set(this, fileSystem);
				}
			};
			try {
				sliderClientConfiguration = client.bindArgs(sliderClientConfiguration,
				    new String[] { "usage" });
			} catch (Exception e) {
				logger.warn("Unable to set SliderClient configs", e);
				throw new RuntimeException(e.getMessage(), e);
			}
			client.init(sliderClientConfiguration);
			client.start();
			return client;
		}
		return null;
	}

	/**
	 * Dynamically determines Slider client configuration. If unable to determine,
	 * <code>null</code> is returned.
	 * 
	 * @return
	 */
	private Configuration getSliderClientConfiguration() {
		AmbariCluster ambariCluster = getAmbariCluster();
		if (ambariCluster != null) {
			AmbariService zkService = ambariClient.getService(ambariCluster,
			    "ZOOKEEPER");
			if (zkService != null && ambariCluster.getDesiredConfigs() != null
			    && ambariCluster.getDesiredConfigs().containsKey("global")
			    && ambariCluster.getDesiredConfigs().containsKey("yarn-site")
			    && ambariCluster.getDesiredConfigs().containsKey("core-site")) {
				Map<String, String> globalConfigs = ambariClient.getConfiguration(
				    ambariCluster, "global",
				    ambariCluster.getDesiredConfigs().get("yarn-site"));
				Map<String, String> yarnSiteConfigs = ambariClient.getConfiguration(
				    ambariCluster, "yarn-site",
				    ambariCluster.getDesiredConfigs().get("yarn-site"));
				Map<String, String> coreSiteConfigs = ambariClient.getConfiguration(
				    ambariCluster, "core-site",
				    ambariCluster.getDesiredConfigs().get("core-site"));
				String zkPort = globalConfigs.get("clientPort");
				String hdfsPath = coreSiteConfigs.get("fs.defaultFS");
				String rmAddress = yarnSiteConfigs.get("yarn.resourcemanager.address");
				String rmSchedulerAddress = yarnSiteConfigs
				    .get("yarn.resourcemanager.scheduler.address");
				StringBuilder zkQuorum = new StringBuilder();
				List<AmbariHostComponent> zkHosts = zkService
				    .getComponentsToHostComponentsMap().get("ZOOKEEPER_SERVER");
				for (AmbariHostComponent zkHost : zkHosts) {
					if (zkQuorum.length() > 0)
						zkQuorum.append(',');
					zkQuorum.append(zkHost.getHostName() + ":" + zkPort);
				}
				HdfsConfiguration hdfsConfig = new HdfsConfiguration();
				YarnConfiguration yarnConfig = new YarnConfiguration(hdfsConfig);

				yarnConfig.set("slider.yarn.queue", "default");
				yarnConfig.set("yarn.log-aggregation-enable", "true");
				yarnConfig.set("yarn.resourcemanager.address", rmAddress);
				yarnConfig.set("yarn.resourcemanager.scheduler.address",
				    rmSchedulerAddress);
				yarnConfig.set("fs.defaultFS", hdfsPath);
				yarnConfig.set("slider.zookeeper.quorum", zkQuorum.toString());
				return yarnConfig;
			}
		}
		return null;
	}

	@Override
	public List<SliderApp> getSliderApps(Set<String> properties)
	    throws YarnException, IOException {
		List<SliderApp> sliderApps = new ArrayList<SliderApp>();
		ClassLoader currentClassLoader = Thread.currentThread()
		    .getContextClassLoader();
		Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
		try {
			SliderClient sliderClient = getSliderClient();
			List<ApplicationReport> yarnApps = sliderClient.listSliderInstances(null);
			for (ApplicationReport yarnApp : yarnApps) {
				SliderApp sliderAppObject = createSliderAppObject(yarnApp, properties,
				    sliderClient);
				if (sliderAppObject != null)
					sliderApps.add(sliderAppObject);
			}
		} finally {
			Thread.currentThread().setContextClassLoader(currentClassLoader);
		}
		return sliderApps;
	}

	@Override
	public void deleteSliderApp(String applicationId) throws YarnException,
	    IOException {
		ClassLoader currentClassLoader = Thread.currentThread()
		    .getContextClassLoader();
		Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
		try {
			Set<String> properties = new HashSet<String>();
			properties.add("id");
			properties.add("name");
			SliderApp sliderApp = getSliderApp(applicationId, properties);
			if (sliderApp == null)
				throw new ApplicationNotFoundException(applicationId);

			SliderClient sliderClient = getSliderClient();
			sliderClient.actionDestroy(sliderApp.getName());
		} finally {
			Thread.currentThread().setContextClassLoader(currentClassLoader);
		}
	}

	@Override
	public SliderAppType getSliderAppType(String appTypeId, Set<String> properties) {
		List<SliderAppType> appTypes = getSliderAppTypes(properties);
		if (appTypeId != null && appTypes != null) {
			for (SliderAppType appType : appTypes)
				if (appTypeId != null && appTypeId.equals(appType.getId()))
					return appType;
		}
		return null;
	}

	@Override
	public List<SliderAppType> getSliderAppTypes(Set<String> properties) {
		if (appTypes == null)
			appTypes = loadAppTypes();
		return appTypes;
	}

	private List<SliderAppType> loadAppTypes() {
		List<SliderAppType> appTypes = null;
		String appsFolderPath = getAppsFolderPath();
		File appsFolder = new File(appsFolderPath);
		if (appsFolder.exists()) {
			File[] appZips = appsFolder
			    .listFiles((FilenameFilter) new RegexFileFilter("^.*\\.zip$"));
			if (appZips != null) {
				appTypes = new ArrayList<SliderAppType>();
				for (File appZip : appZips) {
					try {
						ZipFile zipFile = new ZipFile(appZip);
						Metainfo metainfo = new MetainfoParser().parse(zipFile
						    .getInputStream(zipFile.getEntry("metainfo.xml")));
						// Create app type object
						if (metainfo.getServices() != null
						    && metainfo.getServices().size() > 0) {
							Service service = metainfo.getServices().get(0);
							String appConfigJsonString = IOUtils.toString(
							    zipFile.getInputStream(zipFile.getEntry("appConfig.json")),
							    "UTF-8");
							String resourcesJsonString = IOUtils.toString(
							    zipFile.getInputStream(zipFile.getEntry("resources.json")),
							    "UTF-8");
							JsonElement appConfigJson = new JsonParser()
							    .parse(appConfigJsonString);
							JsonElement resourcesJson = new JsonParser()
							    .parse(resourcesJsonString);
							SliderAppType appType = new SliderAppType();
							appType.setId(service.getName());
							appType.setTypeName(service.getName());
							appType.setTypeDescription(service.getComment());
							appType.setTypeVersion(service.getVersion());
							appType.setTypePackageFileName(appZip.getName());
							// Configs
							Map<String, String> configsMap = new HashMap<String, String>();
							JsonObject appTypeGlobalJson = appConfigJson.getAsJsonObject()
							    .get("global").getAsJsonObject();
							for (Entry<String, JsonElement> e : appTypeGlobalJson.entrySet())
								configsMap.put(e.getKey(), e.getValue().getAsString());
							appType.setTypeConfigs(configsMap);
							// Components
							ArrayList<SliderAppTypeComponent> appTypeComponentList = new ArrayList<SliderAppTypeComponent>();
							for (Component component : service.getComponents()) {
								SliderAppTypeComponent appTypeComponent = new SliderAppTypeComponent();
								appTypeComponent.setDisplayName(component.getName());
								appTypeComponent.setId(component.getName());
								appTypeComponent.setName(component.getName());
								appTypeComponent.setYarnMemory(1024);
								appTypeComponent.setYarnCpuCores(1);
								// appTypeComponent.setPriority(component.);
								if (component.getMinInstanceCount() != null)
									appTypeComponent.setInstanceCount(Integer.parseInt(component
									    .getMinInstanceCount()));
								if (component.getMaxInstanceCount() != null)
									appTypeComponent.setMaxInstanceCount(Integer
									    .parseInt(component.getMaxInstanceCount()));
								if (resourcesJson != null) {
									JsonElement componentJson = resourcesJson.getAsJsonObject()
									    .get("components").getAsJsonObject()
									    .get(component.getName());
									if (componentJson != null
									    && componentJson.getAsJsonObject().has(
									        "yarn.role.priority")) {
										appTypeComponent.setPriority(Integer.parseInt(componentJson
										    .getAsJsonObject().get("yarn.role.priority")
										    .getAsString()));
									}
								}
								appTypeComponent.setCategory(component.getCategory());
								appTypeComponentList.add(appTypeComponent);
							}
							appType.setTypeComponents(appTypeComponentList);
							appTypes.add(appType);
						}
					} catch (ZipException e) {
						logger.warn("Unable to parse app " + appZip.getAbsolutePath(), e);
					} catch (IOException e) {
						logger.warn("Unable to parse app " + appZip.getAbsolutePath(), e);
					}
				}
			}
		}
		return appTypes;
	}
}
