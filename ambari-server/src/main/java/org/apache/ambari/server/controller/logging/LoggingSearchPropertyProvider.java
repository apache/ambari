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
package org.apache.ambari.server.controller.logging;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.LogDefinition;
import org.apache.ambari.server.state.StackId;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class LoggingSearchPropertyProvider implements PropertyProvider {

  private static final Logger LOG = Logger.getLogger(LoggingSearchPropertyProvider.class);

  private static final String CLUSTERS_PATH = "/api/v1/clusters";

  private static final String PATH_TO_SEARCH_ENGINE = "/logging/searchEngine";

  private static AtomicInteger errorLogCounterForLogSearchConnectionExceptions = new AtomicInteger(0);

  private final LoggingRequestHelperFactory requestHelperFactory;

  private final ControllerFactory controllerFactory;

  public LoggingSearchPropertyProvider() {
    this(new LoggingRequestHelperFactoryImpl(), new DefaultControllerFactory());
  }

  protected LoggingSearchPropertyProvider(LoggingRequestHelperFactory requestHelperFactory, ControllerFactory controllerFactory) {
    this.requestHelperFactory = requestHelperFactory;
    this.controllerFactory = controllerFactory;
  }

  @Override
  public Set<Resource> populateResources(Set<Resource> resources, Request request, Predicate predicate) throws SystemException {

    AmbariManagementController controller =
      controllerFactory.getAmbariManagementController();

    for (Resource resource : resources) {
      // obtain the required identifying properties on the host component resource
      final String componentName = (String)resource.getPropertyValue(PropertyHelper.getPropertyId("HostRoles", "component_name"));
      final String hostName = (String) resource.getPropertyValue(PropertyHelper.getPropertyId("HostRoles", "host_name"));
      final String clusterName = (String) resource.getPropertyValue(PropertyHelper.getPropertyId("HostRoles", "cluster_name"));

      // query the stack definitions to find the correct component name (stack name mapped to LogSearch-defined name)
      final String mappedComponentNameForLogSearch =
        getMappedComponentNameForSearch(clusterName, componentName, controller);

      if (mappedComponentNameForLogSearch != null) {
        HostComponentLoggingInfo loggingInfo =
          new HostComponentLoggingInfo();

        // make query to LogSearch server to find the associated file names
        // create helper instance using factory
        LoggingRequestHelper requestHelper =
          requestHelperFactory.getHelper(controller, clusterName);

        // if LogSearch service is available
        if (requestHelper != null) {
          // send query to obtain logging metadata
          Set<String> logFileNames =
            requestHelper.sendGetLogFileNamesRequest(mappedComponentNameForLogSearch, hostName);

          LogLevelQueryResponse levelQueryResponse =
            requestHelper.sendLogLevelQueryRequest(mappedComponentNameForLogSearch, hostName);

          if ((logFileNames != null) && (!logFileNames.isEmpty())) {
            loggingInfo.setComponentName(mappedComponentNameForLogSearch);
            List<LogFileDefinitionInfo> listOfFileDefinitions =
              new LinkedList<LogFileDefinitionInfo>();

            for (String fileName : logFileNames) {
              // generate the URIs that can be used by clients to obtain search results/tail log results/etc
              final String searchEngineURI = controller.getAmbariServerURI(getFullPathToSearchEngine(clusterName));
              final String logFileTailURI = requestHelper.createLogFileTailURI(searchEngineURI, mappedComponentNameForLogSearch, hostName);
              // all log files are assumed to be service types for now
              listOfFileDefinitions.add(new LogFileDefinitionInfo(fileName, LogFileType.SERVICE, searchEngineURI, logFileTailURI));
            }

            loggingInfo.setListOfLogFileDefinitions(listOfFileDefinitions);

            // add the log levels for this host component to the logging structure
            if (levelQueryResponse != null) {
              loggingInfo.setListOfLogLevels(levelQueryResponse.getNameValueList());
            }

            LOG.debug("Adding logging info for component name = " + componentName + " on host name = " + hostName);
            // add the logging metadata for this host component
            resource.setProperty("logging", loggingInfo);
          } else {
            Utils.logErrorMessageWithCounter(LOG, errorLogCounterForLogSearchConnectionExceptions,
              "Error occurred while making request to LogSearch service, unable to populate logging properties on this resource");
          }
        }
      }

    }

    return resources;
  }

  private String getMappedComponentNameForSearch(String clusterName, String componentName, AmbariManagementController controller) {
    try {
      AmbariMetaInfo metaInfo = controller.getAmbariMetaInfo();
      StackId stackId =
        controller.getClusters().getCluster(clusterName).getCurrentStackVersion();
      final String stackName = stackId.getStackName();
      final String stackVersion = stackId.getStackVersion();
      final String serviceName =
        metaInfo.getComponentToService(stackName, stackVersion, componentName);

      ComponentInfo componentInfo =
        metaInfo.getComponent(stackName, stackVersion, serviceName, componentName);
      if (componentInfo != null) {
        List<LogDefinition> listOfLogs =
          componentInfo.getLogs();
        // for now, the assumption is that there is only one log file associated with each
        // component in LogSearch, but this may change in the future
        if ((listOfLogs != null) && (!listOfLogs.isEmpty())) {
          LogDefinition definition = listOfLogs.get(0);
          // return the first log id we find
          return definition.getLogId();
        }
      }

    } catch (AmbariException e) {
      LOG.error("Error occurred while attempting to locate the log component name for component = " + componentName, e);
    }

    return null;
  }

  private String getFullPathToSearchEngine(String clusterName) {
    return CLUSTERS_PATH + "/" + clusterName + PATH_TO_SEARCH_ENGINE;
  }

  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    return Collections.emptySet();
  }

  /**
   * Internal interface used to control how the AmbariManagementController
   * instance is obtained.  This is useful for unit testing as well.
   */
  interface ControllerFactory {
    AmbariManagementController getAmbariManagementController();
  }

  private static class DefaultControllerFactory implements ControllerFactory {
    @Override
    public AmbariManagementController getAmbariManagementController() {
      return AmbariServer.getController();
    }
  }
}
