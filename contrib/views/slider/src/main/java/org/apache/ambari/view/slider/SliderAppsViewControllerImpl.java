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
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipException;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.slider.rest.client.Metric;
import org.apache.ambari.view.slider.rest.client.SliderAppMasterClient;
import org.apache.ambari.view.slider.rest.client.SliderAppMasterClient.SliderAppMasterData;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.security.UserGroupInformation;
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
import org.apache.slider.common.params.ActionCreateArgs;
import org.apache.slider.common.params.ActionFlexArgs;
import org.apache.slider.common.params.ActionFreezeArgs;
import org.apache.slider.common.params.ActionInstallPackageArgs;
import org.apache.slider.common.params.ActionThawArgs;
import org.apache.slider.common.tools.SliderFileSystem;
import org.apache.slider.core.exceptions.SliderException;
import org.apache.slider.core.exceptions.UnknownApplicationInstanceException;
import org.apache.slider.core.main.LauncherExitCodes;
import org.apache.slider.providers.agent.application.metadata.Application;
import org.apache.slider.providers.agent.application.metadata.Component;
import org.apache.slider.providers.agent.application.metadata.Metainfo;
import org.apache.slider.providers.agent.application.metadata.MetainfoParser;
import org.apache.tools.zip.ZipFile;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
  private List<SliderAppType> appTypes;
  private Integer createAppCounter = -1;
  @Inject
  private SliderAppsAlerts sliderAlerts;

  private String getAppsFolderPath() {
    return viewContext.getAmbariProperty("resources.dir") + "/apps";
  }

  private String getAppsCreateFolderPath() {
    return getAppsFolderPath() + "/create";
  }

  @Override
  public ViewStatus getViewStatus() {
    ViewStatus status = new ViewStatus();
    status.setVersion(SliderAppsConfiguration.INSTANCE.getVersion());
    return status;
  }

  private String getApplicationIdString(ApplicationId appId) {
    return Long.toString(appId.getClusterTimestamp()) + "_"
        + Integer.toString(appId.getId());
  }

  private ApplicationId getApplicationId(String appIdString) {
    if (appIdString != null) {
      int index = appIdString.indexOf('_');
      if (index > -1 && index < appIdString.length() - 1) {
        ApplicationId appId = ApplicationId.newInstance(
            Long.parseLong(appIdString.substring(0, index)),
            Integer.parseInt(appIdString.substring(index + 1)));
        return appId;
      }
    }
    return null;
  }
  
  private static interface SliderClientContextRunnable<T> {
    public T run(SliderClient sliderClient) throws YarnException, IOException, InterruptedException;
  }
  
  private <T> T invokeSliderClientRunnable(final SliderClientContextRunnable<T> runnable) throws IOException, InterruptedException {
    ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
    try {
      T value = UserGroupInformation.getBestUGI(null, viewContext.getUsername()).doAs(
          new PrivilegedExceptionAction<T>() {
            @Override
            public T run() throws Exception {
              final SliderClient sliderClient = createSliderClient();
              try{
                return runnable.run(sliderClient);
              }finally{
                destroySliderClient(sliderClient);
              }
            }
          });
      return value;
    } finally {
      Thread.currentThread().setContextClassLoader(currentClassLoader);
    }
  }
  
  @Override
  public SliderApp getSliderApp(String applicationId, final Set<String> properties)
      throws YarnException, IOException, InterruptedException {
    final ApplicationId appId = getApplicationId(applicationId);
    if (appId != null) {
      return invokeSliderClientRunnable(new SliderClientContextRunnable<SliderApp>() {
        @Override
        public SliderApp run(SliderClient sliderClient) throws YarnException, IOException {
          ApplicationReport yarnApp = sliderClient.getApplicationReport(appId);
          return createSliderAppObject(yarnApp, properties, sliderClient);
        }
      });
    }
    return null;
  }

  private SliderApp createSliderAppObject(ApplicationReport yarnApp,
      Set<String> properties, SliderClient sliderClient) {
    if (yarnApp == null) {
      return null;
    }

    SliderApp app = new SliderApp();
    app.setState(yarnApp.getYarnApplicationState().name());

    // Valid Slider App?
    // We want all Slider apps except the ones which properly finished.
    if (YarnApplicationState.FINISHED.equals(yarnApp.getYarnApplicationState())) {
      try {
        if (sliderClient.actionExists(yarnApp.getName(), false) == LauncherExitCodes.EXIT_SUCCESS) {
          app.setState(SliderApp.STATE_FROZEN);
        }
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

    app.setId(getApplicationIdString(yarnApp.getApplicationId()));
    app.setName(yarnApp.getName());
    app.setUser(yarnApp.getUser());
    app.setDiagnostics(yarnApp.getDiagnostics());
    app.setYarnId(yarnApp.getApplicationId().toString());
    app.setStartTime(yarnApp.getStartTime());
    app.setEndTime(yarnApp.getFinishTime());
    Set<String> applicationTags = yarnApp.getApplicationTags();
    if (applicationTags != null && applicationTags.size() > 0) {
      for (String tag : applicationTags) {
        int index = tag.indexOf(':');
        if (index > 0 && index < tag.length() - 1) {
          String key = tag.substring(0, index).trim();
          String value = tag.substring(index + 1).trim();
          if ("name".equals(key)) {
            app.setType(value);
          } else if ("version".equals(key)) {
            app.setAppVersion(value);
          } else if ("description".equals(key)) {
            app.setDescription(value);
          }
        }
      }
    }
    if (properties != null && !properties.isEmpty()) {
      SliderAppMasterClient sliderAppClient = yarnApp.getTrackingUrl() == null ? null
          : new SliderAppMasterClient(yarnApp.getTrackingUrl());
      SliderAppMasterData appMasterData = null;
      Map<String, String> quickLinks = new HashMap<String, String>();
      for (String property : properties) {
        if ("RUNNING".equals(app.getState())) {
          if (sliderAppClient != null) {
            if (appMasterData == null) {
              appMasterData = sliderAppClient.getAppMasterData();
            }
            if (appMasterData!=null && "urls".equals(property.toLowerCase())) {
              if (quickLinks.isEmpty()) {
                quickLinks = sliderAppClient
                    .getQuickLinks(appMasterData.publisherUrl);
              }
              app.setUrls(quickLinks);
            } else if (appMasterData!=null && "configs".equals(property.toLowerCase())) {
              Map<String, Map<String, String>> configs = sliderAppClient
                  .getConfigs(appMasterData.publisherUrl);
              app.setConfigs(configs);
            } else if (appMasterData!=null && "jmx".equals(property.toLowerCase())) {
              if (quickLinks.isEmpty()) {
                quickLinks = sliderAppClient
                    .getQuickLinks(appMasterData.publisherUrl);
              }
              if (quickLinks != null && quickLinks.containsKey("JMX")) {
                String jmxUrl = quickLinks.get("JMX");
                List<SliderAppType> appTypes = getSliderAppTypes(null);
                if (appTypes != null && appTypes.size() > 0) {
                  for (SliderAppType appType : appTypes) {
                    if (logger.isDebugEnabled()) {
                      logger.debug("TYPE: " + appType.getTypeName() + "   "
                          + app.getType());
                      logger.debug("VERSION: " + appType.getTypeVersion() + "   "
                          + app.getAppVersion());
                    }
                    if ((appType.getTypeName() != null && appType.getTypeName()
                        .equalsIgnoreCase(app.getType()))
                        && (appType.getTypeVersion() != null && appType
                            .getTypeVersion().equalsIgnoreCase(
                                app.getAppVersion()))) {
                      app.setJmx(sliderAppClient.getJmx(jmxUrl, viewContext,
                          appType));
                      break;
                    }
                  }
                }
              }
              Map<String, Map<String, String>> configs = sliderAppClient
                  .getConfigs(appMasterData.publisherUrl);
              app.setConfigs(configs);
            } else if ("components".equals(property.toLowerCase())) {
              try {
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
                        if (isLive) {
                          appComponent.getActiveContainers().put(containerId,
                              containerDataMap);
                        } else {
                          appComponent.getCompletedContainers().put(
                              containerId, containerDataMap);
                        }
                      }
                      appComponent.setInstanceCount(appComponent
                          .getActiveContainers().size()
                          + appComponent.getCompletedContainers().size());
                    }
                  }
                  app.setAlerts(sliderAlerts.generateComponentsAlerts(componentTypeMap, app.getType()));
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
   * configuration and started. This slider client can be used to invoke
   * individual API.
   * 
   * When work with this client is done,
   * {@link #destroySliderClient(SliderClient)} must be called.
   * 
   * @return created {@link SliderClient}
   * @see #destroySliderClient(SliderClient)
   * @see #runSliderCommand(String...)
   */
  protected SliderClient createSliderClient() {
    Configuration sliderClientConfiguration = getSliderClientConfiguration();
    SliderClient client = new SliderClient() {
      @Override
      public String getUsername() throws IOException {
        return viewContext.getUsername();
      }

      @Override
      protected void initHadoopBinding() throws IOException, SliderException {
        super.initHadoopBinding();
        // Override the default FS client to the calling user
        try {
          FileSystem fs = FileSystem.get(FileSystem.getDefaultUri(getConfig()),
              getConfig(), viewContext.getUsername());
          SliderFileSystem fileSystem = new SliderFileSystem(fs, getConfig());
          Field fsField = SliderClient.class
              .getDeclaredField("sliderFileSystem");
          fsField.setAccessible(true);
          fsField.set(this, fileSystem);
        } catch (InterruptedException e) {
          throw new SliderException("Slider view unable to override filesystem of Slider client", e);
        } catch (NoSuchFieldException e) {
          throw new SliderException("Slider view unable to override filesystem of Slider client", e);
        } catch (SecurityException e) {
          throw new SliderException("Slider view unable to override filesystem of Slider client", e);
        } catch (IllegalArgumentException e) {
          throw new SliderException("Slider view unable to override filesystem of Slider client", e);
        } catch (IllegalAccessException e) {
          throw new SliderException("Slider view unable to override filesystem of Slider client", e);
        }
      }

      @Override
      public void init(Configuration conf) {
        super.init(conf);
        try {
          initHadoopBinding();
        } catch (SliderException e) {
          throw new RuntimeException("Unable to automatically init Hadoop binding", e);
        } catch (IOException e) {
          throw new RuntimeException("Unable to automatically init Hadoop binding", e);
        }
      }
    };
    try {
      sliderClientConfiguration = client.bindArgs(sliderClientConfiguration,
          new String[] { "usage" });
      client.init(sliderClientConfiguration);
      client.start();
    } catch (Exception e) {
      logger.warn("Unable to create SliderClient", e);
      throw new RuntimeException(e.getMessage(), e);
    } catch (Throwable e) {
      logger.warn("Unable to create SliderClient", e);
      throw new RuntimeException(e.getMessage(), e);
    }
    return client;
  }

  protected void destroySliderClient(SliderClient sliderClient) {
    sliderClient.stop();
    sliderClient = null;
  }

  /**
   * Dynamically determines Slider client configuration. If unable to determine,
   * <code>null</code> is returned.
   * 
   * @return
   */
  private Configuration getSliderClientConfiguration() {
    String hdfsPath = viewContext.getProperties().get(PROPERTY_HDFS_ADDRESS);
    String rmAddress = viewContext.getProperties().get(PROPERTY_YARN_RM_ADDRESS);
    String rmSchedulerAddress = viewContext.getProperties().get(PROPERTY_YARN_RM_SCHEDULER_ADDRESS);
    String zkQuorum = viewContext.getProperties().get(PROPERTY_ZK_QUOROM);
    HdfsConfiguration hdfsConfig = new HdfsConfiguration();
    YarnConfiguration yarnConfig = new YarnConfiguration(hdfsConfig);

    yarnConfig.set("slider.yarn.queue", "default");
    yarnConfig.set("yarn.log-aggregation-enable", "true");
    yarnConfig.set("yarn.resourcemanager.address", rmAddress);
    yarnConfig.set("yarn.resourcemanager.scheduler.address", rmSchedulerAddress);
    yarnConfig.set("fs.defaultFS", hdfsPath);
    yarnConfig.set("slider.zookeeper.quorum", zkQuorum.toString());
    yarnConfig.set("yarn.application.classpath",
            "/etc/hadoop/conf,/usr/lib/hadoop/*,/usr/lib/hadoop/lib/*,/usr/lib/hadoop-hdfs/*,/usr/lib/hadoop-hdfs/lib/*,/usr/lib/hadoop-yarn/*,/usr/lib/hadoop-yarn/lib/*,/usr/lib/hadoop-mapreduce/*,/usr/lib/hadoop-mapreduce/lib/*");
    return yarnConfig;
  }

  private boolean areViewParametersSet() {
    String hdfsPath = viewContext.getProperties().get(PROPERTY_HDFS_ADDRESS);
    String rmAddress = viewContext.getProperties().get(PROPERTY_YARN_RM_ADDRESS);
    String rmSchedulerAddress = viewContext.getProperties().get(PROPERTY_YARN_RM_SCHEDULER_ADDRESS);
    String zkQuorum = viewContext.getProperties().get(PROPERTY_ZK_QUOROM);
    return hdfsPath!=null && rmAddress!=null && rmSchedulerAddress!=null && zkQuorum!=null;
  }

  @Override
  public List<SliderApp> getSliderApps(final Set<String> properties)
      throws YarnException, IOException, InterruptedException {
    if (!areViewParametersSet()) {
      return Collections.emptyList();
    }
    return invokeSliderClientRunnable(new SliderClientContextRunnable<List<SliderApp>>() {
      @Override
      public List<SliderApp> run(SliderClient sliderClient)
          throws YarnException, IOException {
        List<SliderApp> sliderApps = new ArrayList<SliderApp>();
        Map<String, SliderApp> sliderAppsMap = new HashMap<String, SliderApp>();
        List<ApplicationReport> yarnApps = sliderClient.listSliderInstances(null);
        for (ApplicationReport yarnApp : yarnApps) {
          SliderApp sliderAppObject = createSliderAppObject(yarnApp, properties,
              sliderClient);
          if (sliderAppObject != null) {
            if (sliderAppsMap.containsKey(sliderAppObject.getName())) {
              if (sliderAppsMap.get(sliderAppObject.getName()).getId()
                  .compareTo(sliderAppObject.getId()) < 0) {
                sliderAppsMap.put(sliderAppObject.getName(), sliderAppObject);
              }
            } else {
              sliderAppsMap.put(sliderAppObject.getName(), sliderAppObject);
            }
          }
        }
        if (sliderAppsMap.size() > 0)
          sliderApps.addAll(sliderAppsMap.values());
        return sliderApps;
      }
    });
  }

  @Override
  public void deleteSliderApp(final String applicationId) throws YarnException,
      IOException, InterruptedException {
    Integer code = invokeSliderClientRunnable(new SliderClientContextRunnable<Integer>() {
      @Override
      public Integer run(SliderClient sliderClient) throws YarnException,
          IOException, InterruptedException {
        Set<String> properties = new HashSet<String>();
        properties.add("id");
        properties.add("name");
        SliderApp sliderApp = getSliderApp(applicationId, properties);
        if (sliderApp == null) {
          throw new ApplicationNotFoundException(applicationId);
        }
        return sliderClient.actionDestroy(sliderApp.getName());
      }
    });
    logger.info("Deleted Slider App [" + applicationId + "] with exit code " + code);
  }

  @Override
  public SliderAppType getSliderAppType(String appTypeId, Set<String> properties) {
    List<SliderAppType> appTypes = getSliderAppTypes(properties);
    if (appTypeId != null && appTypes != null) {
      for (SliderAppType appType : appTypes) {
        if (appTypeId != null && appTypeId.equals(appType.getId())) {
          return appType;
        }
      }
    }
    return null;
  }

  @Override
  public List<SliderAppType> getSliderAppTypes(Set<String> properties) {
    if (appTypes == null) {
      if (!areViewParametersSet()) {
        return Collections.emptyList();
      }
      appTypes = loadAppTypes();
    }
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
            if (metainfo.getApplication() != null) {
              Application application = metainfo.getApplication();
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
              appType.setId(application.getName());
              appType.setTypeName(application.getName());
              appType.setTypeDescription(application.getComment());
              appType.setTypeVersion(application.getVersion());
              appType.setTypePackageFileName(appZip.getName());
              // Configs
              Map<String, String> configsMap = new HashMap<String, String>();
              JsonObject appTypeGlobalJson = appConfigJson.getAsJsonObject()
                  .get("global").getAsJsonObject();
              for (Entry<String, JsonElement> e : appTypeGlobalJson.entrySet()) {
                configsMap.put(e.getKey(), e.getValue().getAsString());
              }
              appType.setTypeConfigs(configsMap);
              // Components
              ArrayList<SliderAppTypeComponent> appTypeComponentList = new ArrayList<SliderAppTypeComponent>();
              for (Component component : application.getComponents()) {
                if ("CLIENT".equals(component.getCategory())) {
                  continue;
                }
                SliderAppTypeComponent appTypeComponent = new SliderAppTypeComponent();
                appTypeComponent.setDisplayName(component.getName());
                appTypeComponent.setId(component.getName());
                appTypeComponent.setName(component.getName());
                appTypeComponent.setYarnMemory(1024);
                appTypeComponent.setYarnCpuCores(1);
                // Updated below if present in resources.json
                appTypeComponent.setInstanceCount(1);
                // appTypeComponent.setPriority(component.);
                if (component.getMinInstanceCount() != null) {
                  appTypeComponent.setInstanceCount(Integer.parseInt(component
                      .getMinInstanceCount()));
                }
                if (component.getMaxInstanceCount() != null) {
                  appTypeComponent.setMaxInstanceCount(Integer
                      .parseInt(component.getMaxInstanceCount()));
                }
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

              appType.setJmxMetrics(readMetrics(zipFile, "jmx_metrics.json"));
              appType.setGangliaMetrics(readMetrics(zipFile,
                  "ganglia_metrics.json"));

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

  Map<String, Map<String, Map<String, Metric>>> readMetrics(ZipFile zipFile,
      String fileName) {
    Map<String, Map<String, Map<String, Metric>>> metrics = null;
    try {
      InputStream inputStream = zipFile.getInputStream(zipFile
          .getEntry("jmx_metrics.json"));
      ObjectMapper mapper = new ObjectMapper();

      metrics = mapper.readValue(inputStream,
          new TypeReference<Map<String, Map<String, Map<String, Metric>>>>() {
          });
    } catch (IOException e) {
      logger.info("Error reading metrics. " + e.getMessage());
    }

    return metrics;
  }

  @Override
  public String createSliderApp(JsonObject json) throws IOException,
      YarnException, InterruptedException {
    if (json.has("name") && json.has("typeConfigs")
        && json.has("typeComponents") && json.has("typeName")) {
      final String appType = json.get("typeName").getAsString();
      final String appName = json.get("name").getAsString();
      JsonObject configs = json.get("typeConfigs").getAsJsonObject();
      JsonArray componentsArray = json.get("typeComponents").getAsJsonArray();
      String appsCreateFolderPath = getAppsCreateFolderPath();
      File appsCreateFolder = new File(appsCreateFolderPath);
      if (!appsCreateFolder.exists()) {
        appsCreateFolder.mkdirs();
      }
      int appCount;
      synchronized (createAppCounter) {
        if (createAppCounter < 0) {
          // Not initialized
          createAppCounter = 0;
          String[] apps = appsCreateFolder.list();
          for (String app : apps) {
            try {
              int count = Integer.parseInt(app);
              if (count > createAppCounter) {
                createAppCounter = count;
              }
            } catch (NumberFormatException e) {
            }
          }
        }
        appCount = ++createAppCounter;
      }
      File appCreateFolder = new File(appsCreateFolder,
          Integer.toString(appCount));
      appCreateFolder.mkdirs();
      File appConfigJsonFile = new File(appCreateFolder, "appConfig.json");
      File resourcesJsonFile = new File(appCreateFolder, "resources.json");
      saveAppConfigs(configs, componentsArray, appConfigJsonFile);
      saveAppResources(componentsArray, resourcesJsonFile);

      final ActionCreateArgs createArgs = new ActionCreateArgs();
      createArgs.template = appConfigJsonFile;
      createArgs.resources = resourcesJsonFile;
      
      final ActionInstallPackageArgs installArgs = new ActionInstallPackageArgs();
      SliderAppType sliderAppType = getSliderAppType(appType, null);
      String localAppPackageFileName = sliderAppType.getTypePackageFileName();
      installArgs.name = appType;
      installArgs.packageURI = getAppsFolderPath() + "/" + localAppPackageFileName;
      installArgs.replacePkg = true;

      return invokeSliderClientRunnable(new SliderClientContextRunnable<String>() {
        @Override
        public String run(SliderClient sliderClient) throws YarnException, IOException, InterruptedException {
          sliderClient.actionInstallPkg(installArgs);
          sliderClient.actionCreate(appName, createArgs);
          ApplicationId applicationId = sliderClient.applicationId;
          if (applicationId != null) {
            return getApplicationIdString(applicationId);
          }
          return null;
        }
      });
    }
    return null;
  }

  private void saveAppResources(JsonArray componentsArray,
      File resourcesJsonFile) throws IOException {
    JsonObject resourcesObj = new JsonObject();
    resourcesObj.addProperty("schema",
        "http://example.org/specification/v2.0.0");
    resourcesObj.add("metadata", new JsonObject());
    resourcesObj.add("global", new JsonObject());
    JsonObject componentsObj = new JsonObject();
    if (componentsArray != null) {
      for (int i = 0; i < componentsArray.size(); i++) {
        JsonObject inputComponent = componentsArray.get(i).getAsJsonObject();
        if (inputComponent.has("id")) {
          JsonObject componentValue = new JsonObject();
          componentValue.addProperty("yarn.role.priority",
              inputComponent.get("priority").getAsString());
          componentValue.addProperty("yarn.component.instances", inputComponent
              .get("instanceCount").getAsString());
          componentsObj.add(inputComponent.get("id").getAsString(),
              componentValue);
        }
      }
    }
    resourcesObj.add("components", componentsObj);
    String jsonString = new Gson().toJson(resourcesObj);
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(resourcesJsonFile);
      IOUtils.write(jsonString, fos);
    } finally {
      if (fos != null) {
        fos.close();
      }
    }
  }

  private void saveAppConfigs(JsonObject configs, JsonArray componentsArray,
      File appConfigJsonFile) throws IOException {
    JsonObject appConfigs = new JsonObject();
    appConfigs.addProperty("schema", "http://example.org/specification/v2.0.0");
    appConfigs.add("metadata", new JsonObject());
    appConfigs.add("global", configs);
    JsonObject componentsObj = new JsonObject();
    if (componentsArray != null) {
      for (int i = 0; i < componentsArray.size(); i++) {
        JsonObject inputComponent = componentsArray.get(i).getAsJsonObject();
        if (inputComponent.has("id")) {
          componentsObj.add(inputComponent.get("id").getAsString(),
              new JsonObject());
        }
      }
    }
    appConfigs.add("components", componentsObj);
    String jsonString = new Gson().toJson(appConfigs);
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(appConfigJsonFile);
      IOUtils.write(jsonString, fos);
    } finally {
      if (fos != null) {
        fos.close();
      }
    }
  }

  @Override
  public void freezeApp(final String appId) throws YarnException, IOException,
      InterruptedException {
    ApplicationId applicationId = invokeSliderClientRunnable(new SliderClientContextRunnable<ApplicationId>() {
      @Override
      public ApplicationId run(SliderClient sliderClient) throws YarnException, IOException, InterruptedException {
        Set<String> properties = new HashSet<String>();
        properties.add("id");
        properties.add("name");
        final SliderApp sliderApp = getSliderApp(appId, properties);
        if (sliderApp == null)
          throw new ApplicationNotFoundException(appId);
        ActionFreezeArgs freezeArgs = new ActionFreezeArgs();
        sliderClient.actionFreeze(sliderApp.getName(), freezeArgs);
        return sliderClient.applicationId;
      }
    });
    logger.info("Frozen Slider App [" + appId + "] with response: " + applicationId.toString());
  }

  @Override
  public void thawApp(final String appId) throws YarnException, IOException, InterruptedException {
    ApplicationId applicationId = invokeSliderClientRunnable(new SliderClientContextRunnable<ApplicationId>() {
      @Override
      public ApplicationId run(SliderClient sliderClient) throws YarnException,
          IOException, InterruptedException {
        Set<String> properties = new HashSet<String>();
        properties.add("id");
        properties.add("name");
        final SliderApp sliderApp = getSliderApp(appId, properties);
        if (sliderApp == null)
          throw new ApplicationNotFoundException(appId);
        ActionThawArgs thawArgs = new ActionThawArgs();
        sliderClient.actionThaw(sliderApp.getName(), thawArgs);
        return sliderClient.applicationId;
      }
    });
    logger.info("Thawed Slider App [" + appId + "] with response: " + applicationId.toString());
  }

  @Override
  public void flexApp(final String appId, final Map<String, Integer> componentsMap)
      throws YarnException, IOException, InterruptedException {
    ApplicationId applicationId = invokeSliderClientRunnable(new SliderClientContextRunnable<ApplicationId>() {
      @Override
      public ApplicationId run(SliderClient sliderClient) throws YarnException,
          IOException, InterruptedException {
        Set<String> properties = new HashSet<String>();
        properties.add("id");
        properties.add("name");
        final SliderApp sliderApp = getSliderApp(appId, properties);
        if (sliderApp == null) {
          throw new ApplicationNotFoundException(appId);
        }
        ActionFlexArgs flexArgs = new ActionFlexArgs();
        flexArgs.parameters.add(sliderApp.getName());
        for (Entry<String, Integer> e : componentsMap.entrySet()) {
          flexArgs.componentDelegate.componentTuples.add(e.getKey());
          flexArgs.componentDelegate.componentTuples.add(e.getValue()
              .toString());
        }
        sliderClient.actionFlex(sliderApp.getName(), flexArgs);
        return sliderClient.applicationId;
      }
    });
    logger.info("Flexed Slider App [" + appId + "] with response: " + applicationId);
  }

}
