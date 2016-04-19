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
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.slider.ViewStatus.Validation;
import org.apache.ambari.view.slider.clients.AmbariCluster;
import org.apache.ambari.view.slider.clients.AmbariClusterInfo;
import org.apache.ambari.view.slider.clients.AmbariHostComponent;
import org.apache.ambari.view.slider.clients.AmbariService;
import org.apache.ambari.view.slider.clients.AmbariServiceInfo;
import org.apache.ambari.view.slider.rest.client.AmbariHttpClient;
import org.apache.ambari.view.slider.rest.client.Metric;
import org.apache.ambari.view.slider.rest.client.SliderAppMasterClient;
import org.apache.ambari.view.slider.rest.client.SliderAppMasterClient.SliderAppMasterData;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.ApplicationNotFoundException;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.slider.api.ClusterDescription;
import org.apache.slider.client.SliderClient;
import org.apache.slider.common.params.ActionCreateArgs;
import org.apache.slider.common.params.ActionFlexArgs;
import org.apache.slider.common.params.ActionFreezeArgs;
import org.apache.slider.common.params.ActionInstallKeytabArgs;
import org.apache.slider.common.params.ActionInstallPackageArgs;
import org.apache.slider.common.params.ActionThawArgs;
import org.apache.slider.common.tools.SliderUtils;
import org.apache.slider.core.exceptions.SliderException;
import org.apache.slider.core.exceptions.UnknownApplicationInstanceException;
import org.apache.slider.core.main.LauncherExitCodes;
import org.apache.slider.providers.agent.application.metadata.Application;
import org.apache.slider.providers.agent.application.metadata.Component;
import org.apache.slider.providers.agent.application.metadata.Metainfo;
import org.apache.slider.providers.agent.application.metadata.MetainfoParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SliderAppsViewControllerImpl implements SliderAppsViewController {
  private static final Logger logger = LoggerFactory
      .getLogger(SliderAppsViewControllerImpl.class);
  private static String METRICS_PREFIX = "metrics/";
  @Inject
  private ViewContext viewContext;
  private List<SliderAppType> appTypes;
  private Integer createAppCounter = -1;
  @Inject
  private SliderAppsAlerts sliderAlerts;
  private Map<String, MetricsHolder> appMetrics = new HashMap<String, MetricsHolder>();

  private String getAppsFolderPath() {
    return viewContext.getAmbariProperty("resources.dir") + "/apps";
  }

  private String getAppsCreateFolderPath() {
    return getAppsFolderPath() + "/create";
  }

  @Override
  public ViewStatus getViewStatus() {
    ViewStatus status = new ViewStatus();
    Map<String, String> newHadoopConfigs = new HashMap<String, String>();
    status.setVersion(SliderAppsConfiguration.INSTANCE.getVersion());
    String ambariCluster = getViewParameterValue(PARAM_AMBARI_CLUSTER_API);
    String ambariUsername = getViewParameterValue(PARAM_AMBARI_USERNAME);
    String ambariPassword = getViewParameterValue(PARAM_AMBARI_PASSWORD);
    if (ambariCluster != null && ambariUsername != null
        && ambariPassword != null && ambariCluster.trim().length() > 0
        && ambariUsername.trim().length() > 0
        && ambariPassword.trim().length() > 0) {
      String APIPREFIX = "/api/v1/clusters/";
      int index = ambariCluster.indexOf(APIPREFIX);
      if (index > 0) {
        String ambariUrl = ambariCluster.substring(0, index);
        String clusterName = ambariCluster
            .substring(index + APIPREFIX.length());
        if (clusterName.endsWith("/")) {
          clusterName = clusterName.substring(0, clusterName.length() - 1);
        }
        AmbariHttpClient ambariClient = new AmbariHttpClient(ambariUrl,
            ambariUsername, ambariPassword, viewContext);
        try {
          AmbariClusterInfo clusterInfo = ambariClient.getClusterInfo();
          if (clusterInfo!=null && clusterName.equals(clusterInfo.getName())) {
            AmbariCluster cluster = ambariClient.getCluster(clusterInfo);
            AmbariServiceInfo hdfsServiceInfo = null;
            AmbariServiceInfo yarnServiceInfo = null;
            // Validate stack-version
            Validation validateStackVersion = validateStackVersion(clusterInfo.getVersion());
            if (validateStackVersion != null) {
              status.getValidations().add(validateStackVersion);
            }
            for (AmbariServiceInfo svc : cluster.getServices()) {
              if ("HDFS".equals(svc.getId())) {
                hdfsServiceInfo = svc;
              } else if ("YARN".equals(svc.getId())) {
                yarnServiceInfo = svc;
              }
            }
            // HDFS
            if (hdfsServiceInfo != null) {
              if (!hdfsServiceInfo.isStarted()) {
                status.getValidations().add(
                    new ViewStatus.Validation("HDFS service is not started"));
              }
            } else {
              status.getValidations().add(
                  new ViewStatus.Validation("HDFS service is not installed"));
            }
            // YARN
            if (yarnServiceInfo != null) {
              if (!yarnServiceInfo.isStarted()) {
                status.getValidations().add(
                    new ViewStatus.Validation("YARN service is not started"));
              }
            } else {
              status.getValidations().add(
                  new ViewStatus.Validation("YARN service is not installed"));
            }
            // JAVA_HOME
            Map<String, String> ambariServerConfigs = ambariClient.getAmbariServerConfigs();
            if (ambariServerConfigs.containsKey("java.home")) {
              newHadoopConfigs.put("java.home", ambariServerConfigs.get("java.home"));
              status.getParameters().put(PROPERTY_JAVA_HOME, ambariServerConfigs.get("java.home"));
            }
            // Configs
            if (cluster.getDesiredConfigs().containsKey("core-site")) {
              Map<String, String> coreSiteConfigs = ambariClient
                  .getConfiguration(cluster, "core-site", cluster
                      .getDesiredConfigs().get("core-site"));
              newHadoopConfigs.putAll(coreSiteConfigs);
            }
            if (cluster.getDesiredConfigs().containsKey("cluster-env")) {
              Map<String, String> clusterEnvConfigs = ambariClient
                  .getConfiguration(cluster, "cluster-env", cluster
                      .getDesiredConfigs().get("cluster-env"));
              newHadoopConfigs.put("security_enabled",
                  clusterEnvConfigs.get("security_enabled"));
            }
            if (cluster.getDesiredConfigs().containsKey("hdfs-site")) {
              Map<String, String> hdfsSiteConfigs = ambariClient
                  .getConfiguration(cluster, "hdfs-site", cluster
                      .getDesiredConfigs().get("hdfs-site"));
              newHadoopConfigs.putAll(hdfsSiteConfigs);
            }
            if (cluster.getDesiredConfigs().containsKey("yarn-site")) {
              Map<String, String> yarnSiteConfigs = ambariClient
                  .getConfiguration(cluster, "yarn-site", cluster
                      .getDesiredConfigs().get("yarn-site"));
              newHadoopConfigs.putAll(yarnSiteConfigs);
              status.getParameters().put(PROPERTY_YARN_RM_WEBAPP_URL,
                  newHadoopConfigs.get("yarn.resourcemanager.webapp.address"));
            }
            if (cluster.getDesiredConfigs().containsKey("yarn-env")) {
              Map<String, String> yarnEnvConfigs = ambariClient.getConfiguration(cluster, "yarn-env", cluster
                  .getDesiredConfigs().get("yarn-env"));
              String yarnUser = yarnEnvConfigs.get("yarn_user");
              if (yarnUser == null || yarnUser.trim().length() < 1) {
                yarnUser = "yarn";
              }
              newHadoopConfigs.put("yarn_user", yarnUser); // YARN service user
            }
            newHadoopConfigs.put("slider.user", getUserToRunAs(newHadoopConfigs)); // Slider user
            status.getParameters().put(PROPERTY_SLIDER_USER, newHadoopConfigs.get("slider.user"));
            if (newHadoopConfigs.containsKey("security_enabled")) {
              boolean securityEnabled = Boolean.valueOf(newHadoopConfigs.get("security_enabled"));
              if (securityEnabled) {
                String yarnUser = newHadoopConfigs.get("yarn_user");
                if (yarnUser != null && yarnUser.equals(newHadoopConfigs.get("slider.user"))) {
                  status.getValidations().add(
                      new ViewStatus.Validation("Slider view does not support accessing secured YARN cluster as YARN superuser (" + yarnUser + ")"));
                }
              }
            }
            if (cluster.getDesiredConfigs().containsKey("zoo.cfg")) {
              Map<String, String> zkEnvConfigs = ambariClient.getConfiguration(
                  cluster, "zoo.cfg",
                  cluster.getDesiredConfigs().get("zoo.cfg"));
              StringBuilder zkQuorumBuilder = new StringBuilder();
              String port = zkEnvConfigs.get("clientPort");
              AmbariService zkService = ambariClient.getService(cluster,
                  "ZOOKEEPER");
              if (zkService != null) {
                List<AmbariHostComponent> hostsList = zkService
                    .getComponentsToHostComponentsMap().get("ZOOKEEPER_SERVER");
                int count = 1;
                for (AmbariHostComponent host : hostsList) {
                  zkQuorumBuilder.append(host.getHostName() + ":" + port);
                  if (count++ < hostsList.size()) {
                    zkQuorumBuilder.append(",");
                  }
                }
                newHadoopConfigs.put(PROPERTY_SLIDER_ZK_QUORUM,
                    zkQuorumBuilder.toString());
              } else {
                status.getValidations().add(
                    new ViewStatus.Validation(
                        "ZooKeeper service is not installed"));
              }
            } else {
              status.getValidations()
              .add(
                  new ViewStatus.Validation(
                      "ZooKeeper service is not installed"));
            }
            if (cluster.getDesiredConfigs().containsKey("ams-site")) {
              Map<String, String> amsConfigs = ambariClient.getConfiguration(cluster, "ams-site", cluster.getDesiredConfigs().get("ams-site"));
              AmbariService amsService = ambariClient.getService(cluster, "AMBARI_METRICS");
              List<AmbariHostComponent> hostsList = amsService.getComponentsToHostComponentsMap().get("METRICS_COLLECTOR");
              if (hostsList != null && hostsList.size() > 0) {
                String collectorHostName = hostsList.get(0).getHostName();
                newHadoopConfigs.put(PROPERTY_METRICS_SERVER_HOSTNAME, collectorHostName);
                status.getParameters().put(PROPERTY_METRICS_SERVER_HOSTNAME, collectorHostName);
              }
              if (amsConfigs != null && amsConfigs.containsKey("timeline.metrics.service.webapp.address")) {
                String portString = amsConfigs.get("timeline.metrics.service.webapp.address");
                int sepIndex = portString.indexOf(':');
                if (sepIndex > -1) {
                  portString = portString.substring(sepIndex + 1);
                }
                newHadoopConfigs.put(PROPERTY_METRICS_SERVER_PORT, portString);
                status.getParameters().put(PROPERTY_METRICS_SERVER_PORT, portString);
              }
              newHadoopConfigs.put(PROPERTY_METRICS_LIBRARY_PATH, "file:///usr/lib/ambari-metrics-hadoop-sink/ambari-metrics-hadoop-sink.jar");
              status.getParameters().put(PROPERTY_METRICS_LIBRARY_PATH, "file:///usr/lib/ambari-metrics-hadoop-sink/ambari-metrics-hadoop-sink.jar");
            }
            Validation validateHDFSAccess = validateHDFSAccess(newHadoopConfigs, hdfsServiceInfo);
            if (validateHDFSAccess != null) {
              status.getValidations().add(validateHDFSAccess);
            }
          } else {
            status.getValidations().add(
                new ViewStatus.Validation("Ambari cluster with ID ["
                    + clusterName + "] was not found on Ambari server"));
          }
        } catch (Throwable e) {
          logger.error("Exception determining view status", e);
          String message = e.getClass().getName() + ": " + e.getMessage();
          if (e instanceof RuntimeException && e.getCause() != null) {
            message = e.getCause().getClass().getName() + ": " + e.getMessage();
          }
          message = String.format("Unable to initialize Slider view: %s", message);
          status.getValidations().add(new ViewStatus.Validation(message));
        }
      } else {
        status
            .getValidations()
            .add(
                new ViewStatus.Validation(
                    "Ambari server cluster API URL should include cluster name, for example http://ambari.server:8080/api/v1/clusters/c1"));
      }
    } else {
      status.getValidations().add(
          new ViewStatus.Validation(
              "View parameters specifying Ambari details required"));
    }
    synchronized (viewContext) {
      if (!newHadoopConfigs.equals(viewContext.getInstanceData())) {
        Set<String> removeKeys = new HashSet<String>(viewContext.getInstanceData().keySet());
        for (Entry<String, String> e : newHadoopConfigs.entrySet()) {
          viewContext.putInstanceData(e.getKey(), e.getValue());
          removeKeys.remove(e.getKey());
        }
        for (String key : removeKeys) {
          viewContext.removeInstanceData(key);
        }
      }
    }
    return status;
  }

  /**
   * Slider-view supports only some stack-versions. This method validates that the targeted cluster is supported.
   *
   * @param clusterVersion
   * @return
   */
  private Validation validateStackVersion(String clusterVersion) {
    // Assuming cluster versions are of the format "X-a.b.c.d"
    String stackName = clusterVersion;
    String stackVersion = clusterVersion;
    int dashIndex = clusterVersion.indexOf('-');
    if (dashIndex > -1 && dashIndex < clusterVersion.length() - 1) {
      stackName = stackName.substring(0, dashIndex);
      stackVersion = stackVersion.substring(dashIndex + 1);
    }
    String[] versionSplits = stackVersion.split("\\.");
    if (!"HDP".equals(stackName) || versionSplits.length < 2) {
      return new Validation("Stack version (" + clusterVersion + ") used by cluster is not supported");
    }
    try {
      int majorVersion = Integer.parseInt(versionSplits[0]);
      int minorVersion = Integer.parseInt(versionSplits[1]);
      if (!(majorVersion >= 2 && minorVersion >= 2)) {
        return new Validation("Stack version (" + clusterVersion + ") used by cluster is not supported");
      }
    } catch (NumberFormatException e) {
      return new Validation("Stack version (" + clusterVersion + ") used by cluster is not supported");
    }
    return null;
  }

  private Validation validateHDFSAccess(final Map<String, String> hadoopConfigs, AmbariServiceInfo hdfsServiceInfo) {
    if (hdfsServiceInfo != null && hdfsServiceInfo.isStarted()) {
      if (hadoopConfigs.containsKey("fs.defaultFS")) {
        try {
          invokeHDFSClientRunnable(new HDFSClientRunnable<Boolean>() {
            @Override
            public Boolean run(FileSystem fs) throws IOException, InterruptedException {
              Path homePath = fs.getHomeDirectory();
              fs.listFiles(homePath, false);
              return Boolean.TRUE;
            }
          }, hadoopConfigs);
        } catch (IOException e) {
          String message;
          if (hadoopConfigs.get("security_enabled").toLowerCase().equals("true")
              && (getViewParameterValue(PARAM_VIEW_PRINCIPAL) == null
              || getViewParameterValue(PARAM_VIEW_PRINCIPAL_KEYTAB) == null)) {
            message = "Slider View requires access to user's home directory in HDFS to proceed. Please check the kerberos configs";
          } else {
          message = "Slider View requires access to user's home directory in HDFS to proceed. Contact your administrator to create the home directory. ("
              + e.getMessage() + ")";
          }
          logger.warn(message, e);
          return new Validation(message);
        } catch (InterruptedException e) {
          String message = "Slider View requires access to user's home directory in HDFS to proceed. Contact your administrator to create the home directory. ("
              + e.getMessage() + ")";
          logger.warn(message, e);
          return new Validation(message);
        }
      } else {
        return new Validation("Location of HDFS filesystem is unknown for verification. Please check the 'fs.defaultFS' config in core-site.xml");
      }
    }
    return null;
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

  private static interface HDFSClientRunnable<T> {
    public T run(FileSystem fs) throws IOException, InterruptedException;
  }

  private String getUserToRunAs() {
    return getUserToRunAs(getHadoopConfigs());
  }

  private String getUserToRunAs(Map<String, String> hadoopConfigs) {
    String user = getViewParameterValue(PARAM_SLIDER_USER);
    if (user == null || user.trim().length() < 1) {
      if (hadoopConfigs.containsKey("yarn_user")) {
        return hadoopConfigs.get("yarn_user");
      }
      return "yarn";
    } else if ("${username}".equals(user)) {
      return viewContext.getUsername();
    } else {
      return user;
    }
  }

  private <T> T invokeHDFSClientRunnable(final HDFSClientRunnable<T> runnable, final Map<String, String> hadoopConfigs) throws IOException,
      InterruptedException {
    ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
    try {
      boolean securityEnabled = Boolean.valueOf(hadoopConfigs.get("security_enabled"));
      final HdfsConfiguration hdfsConfiguration = new HdfsConfiguration();
      for (Entry<String, String> entry : hadoopConfigs.entrySet()) {
        hdfsConfiguration.set(entry.getKey(), entry.getValue());
      }
      UserGroupInformation.setConfiguration(hdfsConfiguration);
      UserGroupInformation sliderUser;
      String loggedInUser = getUserToRunAs(hadoopConfigs);
      if (securityEnabled) {
        String viewPrincipal = getViewParameterValue(PARAM_VIEW_PRINCIPAL);
        String viewPrincipalKeytab = getViewParameterValue(PARAM_VIEW_PRINCIPAL_KEYTAB);
        UserGroupInformation ambariUser = UserGroupInformation.loginUserFromKeytabAndReturnUGI(viewPrincipal, viewPrincipalKeytab);
        if (loggedInUser.equals(ambariUser.getShortUserName())) {
          // HDFS throws exception when caller tries to impresonate themselves.
          // User: admin@EXAMPLE.COM is not allowed to impersonate admin
          sliderUser = ambariUser;
        } else {
          sliderUser = UserGroupInformation.createProxyUser(loggedInUser, ambariUser);
        }
      } else {
        sliderUser = UserGroupInformation.getBestUGI(null, loggedInUser);
      }
      try {
        T value = sliderUser.doAs(new PrivilegedExceptionAction<T>() {
          @Override
          public T run() throws Exception {
            String fsPath = hadoopConfigs.get("fs.defaultFS");
            FileSystem fs = FileSystem.get(URI.create(fsPath), hdfsConfiguration);
            try {
              return runnable.run(fs);
            } finally {
              fs.close();
            }
          }
        });
        return value;
      } catch (UndeclaredThrowableException e) {
        throw e;
      }
    } finally {
      Thread.currentThread().setContextClassLoader(currentClassLoader);
    }
  }

  private <T> T invokeSliderClientRunnable(final SliderClientContextRunnable<T> runnable) throws IOException, InterruptedException, YarnException {
    ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
    try {
      boolean securityEnabled = Boolean.valueOf(getHadoopConfigs().get("security_enabled"));
      UserGroupInformation.setConfiguration(getSliderClientConfiguration());
      UserGroupInformation sliderUser;
      String loggedInUser = getUserToRunAs();
      if (securityEnabled) {
        String viewPrincipal = getViewParameterValue(PARAM_VIEW_PRINCIPAL);
        String viewPrincipalKeytab = getViewParameterValue(PARAM_VIEW_PRINCIPAL_KEYTAB);
        UserGroupInformation ambariUser = UserGroupInformation.loginUserFromKeytabAndReturnUGI(viewPrincipal, viewPrincipalKeytab);
        if (loggedInUser.equals(ambariUser.getShortUserName())) {
          // HDFS throws exception when caller tries to impresonate themselves.
          // User: admin@EXAMPLE.COM is not allowed to impersonate admin
          sliderUser = ambariUser;
        } else {
          sliderUser = UserGroupInformation.createProxyUser(loggedInUser, ambariUser);
        }
      } else {
        sliderUser = UserGroupInformation.getBestUGI(null, loggedInUser);
      }
      try {
        T value = sliderUser.doAs(new PrivilegedExceptionAction<T>() {
          @Override
          public T run() throws Exception {
            final SliderClient sliderClient = createSliderClient();
            try {
              return runnable.run(sliderClient);
            } finally {
              destroySliderClient(sliderClient);
            }
          }
        });
        return value;
      } catch (UndeclaredThrowableException e) {
        Throwable cause = e.getCause();
        if (cause instanceof YarnException) {
          YarnException ye = (YarnException) cause;
          throw ye;
        }
        throw e;
      }
    } finally {
      Thread.currentThread().setContextClassLoader(currentClassLoader);
    }
  }

  @Override
  public boolean appExists(final String appName) throws IOException, InterruptedException, YarnException {
    return invokeSliderClientRunnable(new SliderClientContextRunnable<Boolean>() {
      @Override
      public Boolean run(SliderClient sliderClient) throws YarnException, IOException {
        if (appName != null) {
          try {
            return sliderClient.actionExists(appName, false) == SliderClient.EXIT_SUCCESS;
          } catch (UnknownApplicationInstanceException e) {
            return Boolean.FALSE;
          }
        }
        return Boolean.FALSE;
      }
    });
  }

  @Override
  public SliderApp getSliderApp(final String applicationId, final Set<String> properties)
     throws YarnException, IOException, InterruptedException {
    return invokeSliderClientRunnable(new SliderClientContextRunnable<SliderApp>() {
      @Override
      public SliderApp run(SliderClient sliderClient) throws YarnException, IOException {
        if (applicationId!=null) {
          ApplicationId appId = getApplicationId(applicationId);
          if (appId != null) {
            ApplicationReport yarnApp = sliderClient.getApplicationReport(appId);
            return createSliderAppObject(yarnApp, properties, sliderClient);
          }
        }
        return null;
      }
    });
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
    if (YarnApplicationState.KILLED.equals(yarnApp.getYarnApplicationState()) || YarnApplicationState.FAILED.equals(yarnApp.getYarnApplicationState())) {
      try {
        if (sliderClient.actionExists(yarnApp.getName(), false) != LauncherExitCodes.EXIT_SUCCESS) {
          // YARN application is killed or failed, and no HDFS content - Application has been destroyed.
          return null;
        }
      } catch (UnknownApplicationInstanceException e) {
        return null; // Application not in HDFS - means it is not frozen
      } catch (YarnException e) {
        logger.warn("Unable to determine status of killed app " + yarnApp.getName(), e);
        return null;
      } catch (IOException e) {
        logger.warn("Unable to determine status of killed app " + yarnApp.getName(), e);
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
            app.setTypeId(value.toUpperCase() + "-" + app.getAppVersion());
          } else if ("version".equals(key)) {
            app.setAppVersion(value);
            app.setTypeId(app.getType() + "-" + value);
          } else if ("description".equals(key)) {
            app.setDescription(value);
          }
        }
      }
    }
    if (properties != null && !properties.isEmpty()) {
      SliderAppType matchedAppType = null;
      List<SliderAppType> matchingAppTypes = getSliderAppTypes(null);
      if (matchingAppTypes != null && matchingAppTypes.size() > 0) {
        for (SliderAppType appType : matchingAppTypes) {
          if ((appType.getTypeName() != null && appType.getTypeName()
              .equalsIgnoreCase(app.getType()))
              && (appType.getTypeVersion() != null && appType.getTypeVersion()
                  .equalsIgnoreCase(app.getAppVersion()))) {
            matchedAppType = appType;
            app.setTypeId(appType.getId());
            break;
          }
        }
      }

      SliderAppMasterClient sliderAppClient = yarnApp.getTrackingUrl() == null ? null
          : new SliderAppMasterClient(yarnApp.getTrackingUrl(), viewContext);
      SliderAppMasterData appMasterData = null;
      Map<String, String> quickLinks = new HashMap<String, String>();
      Set<String> metrics = new HashSet<String>();
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
                if (matchedAppType != null) {
                  MetricsHolder metricsHolder = appMetrics.get(matchedAppType
                      .uniqueName());
                  app.setJmx(sliderAppClient.getJmx(jmxUrl, viewContext,
                      matchedAppType, metricsHolder));
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
                      // Set total instances count from statistics
                      appComponent.setInstanceCount(appComponent
                          .getActiveContainers().size()
                          + appComponent.getCompletedContainers().size());
                      if (description.statistics != null
                          && description.statistics.containsKey(componentEntry.getKey())) {
                        Map<String, Integer> statisticsMap = description.statistics.get(componentEntry.getKey());
                        if (statisticsMap.containsKey("containers.desired")) {
                          appComponent.setInstanceCount(statisticsMap.get("containers.desired"));
                        }
                      }
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
            } else if (property.startsWith(METRICS_PREFIX)) {
              metrics.add(property.substring(METRICS_PREFIX.length()));
            } else if ("supportedMetrics".equals(property)) {
              if (matchedAppType != null) {
                app.setSupportedMetrics(matchedAppType.getSupportedMetrics());
              }
            }
          }
        }
      }
      if (metrics.size() > 0) {
        if (quickLinks.isEmpty()) {
          quickLinks = sliderAppClient
              .getQuickLinks(appMasterData.publisherUrl);
        }
        if (quickLinks != null && quickLinks.containsKey(METRICS_API_NAME)) {
          String metricsUrl = quickLinks.get(METRICS_API_NAME);
          MetricsHolder metricsHolder = appMetrics.get(matchedAppType
              .uniqueName());
          app.setMetrics(sliderAppClient.getMetrics(yarnApp.getName(),
              metricsUrl, metrics, null, viewContext, matchedAppType,
              metricsHolder));
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
      if (logger.isDebugEnabled()) {
        logger.debug("Slider Client configuration: " + sliderClientConfiguration.toString());
      }
      sliderClientConfiguration = client.bindArgs(sliderClientConfiguration,
          new String[] { "help" });
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

  private String getViewParameterValue(String parameterName) {
    String value = viewContext.getProperties().get(parameterName);
    if ("null".equals(value)) {
      return null;
    }
    return value;
  }

  protected Map<String, String> getHadoopConfigs() {
    return viewContext.getInstanceData();
  }

  /**
   * Dynamically determines Slider client configuration. If unable to determine,
   * <code>null</code> is returned.
   * 
   * @return
   */
  private Configuration getSliderClientConfiguration() {
    HdfsConfiguration hdfsConfig = new HdfsConfiguration();
    YarnConfiguration yarnConfig = new YarnConfiguration(hdfsConfig);

    Map<String, String> hadoopConfigs = getHadoopConfigs();
    for(Entry<String, String> entry: hadoopConfigs.entrySet()) {
      String entryValue = entry.getValue();
      if (entryValue == null) {
        entryValue = "";
      }
      yarnConfig.set(entry.getKey(), entryValue);
    }
    yarnConfig.set(PROPERTY_SLIDER_SECURITY_ENABLED, hadoopConfigs.get("security_enabled"));
    if (hadoopConfigs.containsKey(PROPERTY_SLIDER_ZK_QUORUM)) {
      yarnConfig.set(PROPERTY_SLIDER_ZK_QUORUM, hadoopConfigs.get(PROPERTY_SLIDER_ZK_QUORUM));
    }
    return yarnConfig;
  }

  private boolean areViewParametersSet() {
    Map<String, String> hadoopConfigs = getHadoopConfigs();
    return hadoopConfigs.containsKey("fs.defaultFS")
        && hadoopConfigs.containsKey("yarn.resourcemanager.address")
        && hadoopConfigs.containsKey("yarn.resourcemanager.webapp.address")
        && hadoopConfigs.containsKey(PROPERTY_SLIDER_ZK_QUORUM);
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
    try {
      // Need to determine security enablement before loading app types
      getViewStatus();
    } catch (Throwable t) {
      logger.warn("Unable to determine if cluster is secured when loading app-types", t);
    }
    Map<String, String> hadoopConfigs = getHadoopConfigs();
    final boolean securityEnabled = Boolean.valueOf(hadoopConfigs.get("security_enabled"));
    if (appTypes == null) {
      appTypes = loadAppTypes();
    }
    if (appTypes != null) {
      for (SliderAppType appType : appTypes) {
        Map<String, String> configs = appType.typeConfigsUnsecured;
        JsonObject resourcesObj = appType.resourcesUnsecured;
        if (securityEnabled) {
          configs = appType.typeConfigsSecured;
          if (configs == null || configs.isEmpty()) {
            configs = appType.typeConfigsUnsecured;
          }
        }
        if (securityEnabled) {
          resourcesObj = appType.resourcesSecured;
          if (resourcesObj == null) {
            resourcesObj = appType.resourcesUnsecured;
          }
        }
        Map<String, String> appTypeConfigs = new HashMap<String, String>();
        for (Entry<String, String> e : configs.entrySet()) {
          String valueString = e.getValue();
          if (valueString != null && valueString.contains("${USER_NAME}")) {
            valueString = valueString.replace("${USER_NAME}", getUserToRunAs(hadoopConfigs));
          }
          appTypeConfigs.put(e.getKey(), valueString);
        }
        appType.setTypeConfigs(appTypeConfigs);

        if (resourcesObj != null) {
          for (SliderAppTypeComponent component : appType.getTypeComponents()) {
            JsonElement componentJson = resourcesObj.get(component.getName());
            if (componentJson != null && componentJson.isJsonObject()) {
              JsonObject componentObj = componentJson.getAsJsonObject();
              if (componentObj.has("yarn.component.instances")) {
                component.setInstanceCount(Integer.parseInt(componentObj.get(
                    "yarn.component.instances").getAsString()));
              }
              if (componentObj.has("yarn.role.priority")) {
                component.setPriority(Integer.parseInt(componentObj.get("yarn.role.priority").getAsString()));
              }
              if (componentObj.has("yarn.memory")) {
                component.setYarnMemory(Integer.parseInt(componentObj.get("yarn.memory").getAsString()));
              }
            }
          }
        }
      }
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
            Metainfo metainfo = new MetainfoParser().fromXmlStream(zipFile
                .getInputStream(zipFile.getEntry("metainfo.xml")));
            // Create app type object
            if (metainfo.getApplication() != null) {
              Application application = metainfo.getApplication();
              ZipArchiveEntry appConfigZipEntry = zipFile.getEntry("appConfig-default.json");
              ZipArchiveEntry appConfigSecuredZipEntry = zipFile.getEntry("appConfig-secured-default.json");
              if (appConfigZipEntry == null) {
                throw new IllegalStateException("Slider App package '" + appZip.getName() + "' does not contain 'appConfig-default.json' file");
              }
              ZipArchiveEntry resourcesZipEntry = zipFile.getEntry("resources-default.json");
              ZipArchiveEntry resourcesSecuredZipEntry = zipFile.getEntry("resources-secured-default.json");
              if (resourcesZipEntry == null) {
                throw new IllegalStateException("Slider App package '" + appZip.getName() + "' does not contain 'resources-default.json' file");
              }
              SliderAppType appType = new SliderAppType();
              appType.setId(application.getName() + "-" + application.getVersion());
              appType.setTypeName(application.getName());
              appType.setTypeDescription(application.getComment());
              appType.setTypeVersion(application.getVersion());
              appType.setTypePackageFileName(appZip.getName());
              // Configs
              appType.typeConfigsUnsecured = parseAppTypeConfigs(zipFile, appConfigZipEntry, appZip.getName(), application.getName());
              if (appConfigSecuredZipEntry != null) {
                appType.typeConfigsSecured = parseAppTypeConfigs(zipFile, appConfigSecuredZipEntry, appZip.getName(), application.getName());
              }
              // Resources
              appType.resourcesUnsecured = parseAppTypeResources(zipFile, resourcesZipEntry);
              if (resourcesSecuredZipEntry != null) {
                appType.resourcesSecured = parseAppTypeResources(zipFile, resourcesSecuredZipEntry);
              }
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
                appTypeComponent.setCategory(component.getCategory());
                appTypeComponentList.add(appTypeComponent);
              }

              MetricsHolder metricsHolder = new MetricsHolder();
              metricsHolder.setJmxMetrics(readMetrics(zipFile,
                  "jmx_metrics.json"));
              metricsHolder.setTimelineMetrics(readMetrics(zipFile,
                  "timeline_metrics.json"));
              appType.setSupportedMetrics(getSupportedMetrics(metricsHolder
                  .getTimelineMetrics()));
              appMetrics.put(appType.uniqueName(), metricsHolder);

              appType.setTypeComponents(appTypeComponentList);
              appTypes.add(appType);
            }
          } catch (ZipException e) {
            logger.warn("Unable to parse Slider App package " + appZip.getAbsolutePath(), e);
          } catch (IOException e) {
            logger.warn("Unable to parse Slider App package " + appZip.getAbsolutePath(), e);
          } catch (Throwable e) {
            logger.warn("Unable to parse Slider App package " + appZip.getAbsolutePath(), e);
          }
        }
      }
    }
    return appTypes;
  }

  private JsonObject parseAppTypeResources(ZipFile zipFile, ZipArchiveEntry resourcesZipEntry) throws ZipException, IOException {
    String resourcesJsonString = IOUtils.toString(zipFile.getInputStream(resourcesZipEntry), "UTF-8");
    JsonElement resourcesJson = new JsonParser().parse(resourcesJsonString);
    return resourcesJson.getAsJsonObject().get("components").getAsJsonObject();
  }

  private Map<String, String> parseAppTypeConfigs(ZipFile zipFile, ZipArchiveEntry appConfigZipEntry, String zipFileName, String appName) throws IOException,
      ZipException {
    String appConfigJsonString = IOUtils.toString(zipFile.getInputStream(appConfigZipEntry), "UTF-8");
    JsonElement appConfigJson = new JsonParser().parse(appConfigJsonString);
    Map<String, String> configsMap = new HashMap<String, String>();
    JsonObject appTypeGlobalJson = appConfigJson.getAsJsonObject().get("global").getAsJsonObject();
    for (Entry<String, JsonElement> e : appTypeGlobalJson.entrySet()) {
      String key = e.getKey();
      String valueString = e.getValue().getAsString();
      if ("application.def".equals(key)) {
        valueString = String.format(".slider/package/%s/%s", appName, zipFileName);
      }
      configsMap.put(key, valueString);
    }
    return configsMap;
  }

  private List<String> getSupportedMetrics(
      Map<String, Map<String, Map<String, Metric>>> metrics) {
    Set<String> supportedMetrics = new HashSet<String>();
    if (metrics != null && metrics.size() > 0) {
      for (Map<String, Map<String, Metric>> compMetrics : metrics
          .values()) {
        for (Map<String, Metric> compMetric : compMetrics.values()) {
          supportedMetrics.addAll(compMetric.keySet());
        }
      }
    }
    return new ArrayList<String>(supportedMetrics);
  }

  Map<String, Map<String, Map<String, Metric>>> readMetrics(ZipFile zipFile,
      String fileName) {
    Map<String, Map<String, Map<String, Metric>>> metrics = null;
    try {
      InputStream inputStream = zipFile.getInputStream(zipFile
          .getEntry(fileName));
      ObjectMapper mapper = new ObjectMapper();

      metrics = mapper.readValue(inputStream,
          new TypeReference<Map<String, Map<String, Map<String, Metric>>>>() {
          });
    } catch (IOException e) {
      logger.info("Error reading metrics for file " + fileName + ". " + e.getMessage());
    }

    return metrics;
  }

  @Override
  public String createSliderApp(JsonObject json) throws IOException,
      YarnException, InterruptedException {
    if (json.has("name") && json.has("typeConfigs")
        && json.has("resources") && json.has("typeName")) {
      final String appTypeId = json.get("typeName").getAsString();
      SliderAppType sliderAppType = getSliderAppType(appTypeId, null);
      final String appName = json.get("name").getAsString();
      final String queueName = json.has("queue") ? json.get("queue").getAsString() : null;
      final boolean securityEnabled = Boolean.valueOf(getHadoopConfigs().get("security_enabled"));
      final boolean twoWaySSlEnabled = json.has("twoWaySSLEnabled") ? Boolean.valueOf(json.get("twoWaySSLEnabled").getAsString()) : false;
      JsonObject configs = json.get("typeConfigs").getAsJsonObject();
      final String hdpVersion = configs.has("env.HDP_VERSION") ? configs.get(
          "env.HDP_VERSION").getAsString() : null;
      JsonObject resourcesObj = json.get("resources").getAsJsonObject();
      JsonArray componentsArray = resourcesObj.get("components").getAsJsonArray();
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
      saveAppConfigs(configs, componentsArray, appName, sliderAppType.getTypeName(), securityEnabled, twoWaySSlEnabled, appConfigJsonFile);
      saveAppResources(resourcesObj, resourcesJsonFile);

      final ActionCreateArgs createArgs = new ActionCreateArgs();
      createArgs.template = appConfigJsonFile;
      createArgs.resources = resourcesJsonFile;
      if (queueName != null && queueName.trim().length() > 0) {
        createArgs.queue = queueName;
      }

      final ActionInstallPackageArgs installArgs = new ActionInstallPackageArgs();
      String localAppPackageFileName = sliderAppType.getTypePackageFileName();
      installArgs.name = sliderAppType.getTypeName();
      installArgs.packageURI = getAppsFolderPath() + "/" + localAppPackageFileName;
      installArgs.replacePkg = true;

      final List<ActionInstallKeytabArgs> installKeytabActions = new ArrayList<ActionInstallKeytabArgs>();
      if (securityEnabled) {
        for (String keytab : getUserToRunAsKeytabs(sliderAppType.getTypeName())) {
          ActionInstallKeytabArgs keytabArgs = new ActionInstallKeytabArgs();
          keytabArgs.keytabUri = keytab;
          keytabArgs.folder = appName;
          keytabArgs.overwrite = true;
          installKeytabActions.add(keytabArgs);
        }
      }

      return invokeSliderClientRunnable(new SliderClientContextRunnable<String>() {
        @Override
        public String run(SliderClient sliderClient) throws YarnException, IOException, InterruptedException {
          try {
            File sliderJarFile = SliderUtils.findContainingJar(SliderClient.class);
            if (sliderJarFile != null) {
              if (logger.isDebugEnabled()) {
                logger.debug("slider.libdir=" + sliderJarFile.getParentFile().getAbsolutePath());
              }
              System.setProperty("slider.libdir", sliderJarFile.getParentFile().getAbsolutePath());
            }
          } catch (Throwable t) {
            logger.warn("Unable to determine 'slider.libdir' path", t);
          }
          if (securityEnabled) {
            for (ActionInstallKeytabArgs keytabArgs : installKeytabActions) {
              if (logger.isDebugEnabled()) {
                logger.debug("Installing keytab " + keytabArgs.keytabUri);
              }
              sliderClient.actionInstallKeytab(keytabArgs);
            }
          }
          if (StringUtils.isNotEmpty(hdpVersion)) {
            System.setProperty("HDP_VERSION", hdpVersion);
            logger.info("Setting system property HDP_VERSION=" + hdpVersion);
          }
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

  private void saveAppResources(JsonObject clientResourcesObj,
      File resourcesJsonFile) throws IOException {
    JsonObject resourcesObj = new JsonObject();
    JsonArray clientComponentsArray = clientResourcesObj.get("components").getAsJsonArray();
    resourcesObj.addProperty("schema",
        "http://example.org/specification/v2.0.0");
    resourcesObj.add("metadata", new JsonObject());
    resourcesObj.add("global",
        clientResourcesObj.has("global") ? clientResourcesObj.get("global")
            .getAsJsonObject() : new JsonObject());
    JsonObject componentsObj = new JsonObject();
    if (clientComponentsArray != null) {
      for (int i = 0; i < clientComponentsArray.size(); i++) {
        JsonObject inputComponent = clientComponentsArray.get(i).getAsJsonObject();
        if (inputComponent.has("id")) {
          JsonObject componentValue = new JsonObject();
          if (inputComponent.has("priority")) {
            componentValue.addProperty("yarn.role.priority", inputComponent
                .get("priority").getAsString());
          }
          if (inputComponent.has("instanceCount")) {
            componentValue.addProperty("yarn.component.instances",
                inputComponent.get("instanceCount").getAsString());
          }
          if (inputComponent.has("yarnMemory")) {
            componentValue.addProperty("yarn.memory",
                inputComponent.get("yarnMemory").getAsString());
          }
          if (inputComponent.has("yarnCpuCores")) {
            componentValue.addProperty("yarn.vcores",
                inputComponent.get("yarnCpuCores").getAsString());
          }
          if (inputComponent.has("yarnLabel")) {
            componentValue.addProperty("yarn.label.expression", inputComponent
                .get("yarnLabel").getAsString());
          }
          componentsObj.add(inputComponent.get("id").getAsString(),
              componentValue);
        }
      }
    }
    resourcesObj.add("components", componentsObj);
    String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(resourcesObj);
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

  /*
   * When security is enabled, the AppMaster itself needs the keytab identifying the calling user.
   * The user's keytab should be at the same location as the view's keytab, and should be
   * named as ${username}.headless.keytab.
   * 
   * This method returns the list of keytabs where the first keytab is always the AppMaster's 
   * keytab. Additional keys will be provided, only if found at the location of the view's keytab.
   * Additional keytabs should be of the format ${username}.<APP_TYPE>.*.keytab
   */
  private List<String> getUserToRunAsKeytabs(String appType) {
    List<String> keytabsList = new ArrayList<String>();
    String viewKeytab = viewContext.getProperties().get(PARAM_VIEW_PRINCIPAL_KEYTAB);
    String folderPath = "";
    int index = viewKeytab.lastIndexOf('/');
    if (index > -1) {
      folderPath = viewKeytab.substring(0, index);
    }
    String username = getUserToRunAs();
    String userKeytab = folderPath + "/" + username + ".headless.keytab";
    File folder = new File(folderPath);
    if (folder.exists()) {
      final Pattern userKeytabPattern = Pattern.compile("^" + username + "\\." + appType + "\\..*\\.keytab");
      String[] keytabNames = folder.list(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          return userKeytabPattern.matcher(name).matches();
        }
      });
      if (keytabNames != null) {
        for (String keytabName : keytabNames) {
          keytabsList.add(folderPath + "/" + keytabName);
        }
      }
    }
    keytabsList.add(0, userKeytab);
    if (logger.isDebugEnabled()) {
      logger.debug(username + " keytabs: " + keytabsList);
    }
    return keytabsList;
  }

  private void saveAppConfigs(JsonObject configs, JsonArray componentsArray,
      String appName, String appType, boolean securityEnabled, boolean twoWaySSlEnabled, File appConfigJsonFile) throws IOException {
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
    if (securityEnabled) {
      JsonObject appMasterComponent = new JsonObject();
      String userToRunAsKeytab = getUserToRunAsKeytabs(appType).get(0);
      String fileName = userToRunAsKeytab.substring(userToRunAsKeytab.lastIndexOf('/') + 1);
      String userName = fileName.substring(0, fileName.indexOf('.'));
      String viewPrincipalName = getViewParameterValue(PARAM_VIEW_PRINCIPAL);
      int atIndex = viewPrincipalName.lastIndexOf('@');
      String viewPrincipalDomain = atIndex > -1 ? viewPrincipalName.substring(atIndex+1) : "";
      appMasterComponent.add("slider.keytab.principal.name", new JsonPrimitive(userName + "@" + viewPrincipalDomain));
      appMasterComponent.add("slider.am.login.keytab.name", new JsonPrimitive(fileName));
      appMasterComponent.add("slider.hdfs.keytab.dir", new JsonPrimitive(".slider/keytabs/" + appName));
      componentsObj.add("slider-appmaster", appMasterComponent);
   }
   if (twoWaySSlEnabled) {
     JsonObject appMasterComponent;
     if (componentsObj.has("slider-appmaster")) {
       appMasterComponent = componentsObj.get("slider-appmaster").getAsJsonObject();
     } else {
       appMasterComponent = new JsonObject();
       componentsObj.add("slider-appmaster", appMasterComponent);
     }
     appMasterComponent.add("ssl.server.client.auth", new JsonPrimitive("true"));
   }
   appConfigs.add("components", componentsObj);
    String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(appConfigs);
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(appConfigJsonFile);
      IOUtils.write(jsonString, fos);
    } finally {
      if (fos != null) {
        fos.close();
      }
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Saved appConfigs.json at " + appConfigJsonFile.getAbsolutePath());
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
