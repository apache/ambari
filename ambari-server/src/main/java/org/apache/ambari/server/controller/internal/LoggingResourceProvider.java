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

package org.apache.ambari.server.controller.internal;


import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.logging.LogQueryResponse;
import org.apache.ambari.server.controller.logging.LoggingRequestHelper;
import org.apache.ambari.server.controller.logging.LoggingRequestHelperFactory;
import org.apache.ambari.server.controller.logging.LoggingRequestHelperFactoryImpl;
import org.apache.ambari.server.controller.logging.LoggingRequestHelperImpl;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LoggingResourceProvider extends AbstractControllerResourceProvider {

  private static final String LOGGING_SEARCH_SERVICE_PROPERTY_ID = PropertyHelper.getPropertyId("Logging", "search_service_name");
  private static final String LOGGING_SEARCH_TERM_PROPERTY_ID = PropertyHelper.getPropertyId("Logging", "searchTerm");
  private static final String LOGGING_COMPONENT_PROPERTY_ID = PropertyHelper.getPropertyId("Logging", "component");

  private static final Set<String> PROPERTY_IDS;

  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS;

  static {
    Set<String> localSet = new HashSet<String>();
    localSet.add(LOGGING_SEARCH_SERVICE_PROPERTY_ID);
    localSet.add(LOGGING_SEARCH_TERM_PROPERTY_ID);
    localSet.add(LOGGING_COMPONENT_PROPERTY_ID);

    PROPERTY_IDS = Collections.unmodifiableSet(localSet);

    Map<Resource.Type, String> localMap =
      new HashMap<Resource.Type, String>();

    localMap.put(Resource.Type.LoggingQuery, LOGGING_SEARCH_SERVICE_PROPERTY_ID);
    KEY_PROPERTY_IDS = Collections.unmodifiableMap(localMap);

  }


  public LoggingResourceProvider(Set<String> propertyIds,
                                 Map<Resource.Type, String> keyPropertyIds,
                                 AmbariManagementController controller) {



    super(PROPERTY_IDS, KEY_PROPERTY_IDS, controller);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return Collections.emptySet();
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    // just a simple text resource for now, to describe the logging service
    Resource resource = new ResourceImpl(Resource.Type.LoggingQuery);
    setResourceProperty(resource, LOGGING_SEARCH_SERVICE_PROPERTY_ID, "logging", getRequestPropertyIds(request, predicate));

    // TODO, fix this during refactoring
    LoggingRequestHelper requestHelper =
      new LoggingRequestHelperFactoryImpl().getHelper(AmbariServer.getController(), "");

    Map<String, String> queryParameters =
      new HashMap<String, String>();

    queryParameters.put("level", "ERROR");

    LogQueryResponse response =
      requestHelper.sendQueryRequest(queryParameters);

    // include the top-level query result properties
    resource.setProperty("startIndex", response.getStartIndex());
    resource.setProperty("pageSize", response.getPageSize());
    resource.setProperty("resultSize", response.getResultSize());
    resource.setProperty("queryTimeMMS", response.getQueryTimeMS());
    resource.setProperty("totalCount", response.getTotalCount());

    // include the individual responses
    resource.setProperty("logList", response.getListOfResults());

    return Collections.singleton(resource);
  }

  private static List<Map<String, String>> createTestData(Resource resource) {
    // just create some test data for verifying basic resource code, not an actual result
    Map<String, String> levelCounts = new HashMap<String, String>();
    levelCounts.put("INFO", "100");
    levelCounts.put("WARN", "250");
    levelCounts.put("DEBUG", "300");

    resource.setProperty("logLevels", levelCounts);

    List<Map <String, String>> listOfResults = new LinkedList<Map<String, String>>();
    Map<String, String> resultOne = new HashMap<String, String>();
    resultOne.put("data", "This is a test sentence.");
    resultOne.put("score", "100");
    resultOne.put("level", "INFO");
    resultOne.put("type", "hdfs_namenode");
    resultOne.put("host", "c6401.ambari.apache.org");
    resultOne.put("LoggerName", "NameNodeLogger");

    listOfResults.add(resultOne);
    return listOfResults;
  }

  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    Set<String> unSupportedProperties =
      super.checkPropertyIds(propertyIds);

    unSupportedProperties.remove("searchTerm");

    return unSupportedProperties;

  }
}
