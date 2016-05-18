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

package org.apache.ambari.server.api.services;

import org.apache.ambari.server.api.services.serializers.ResultSerializer;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.logging.LogQueryResponse;
import org.apache.ambari.server.controller.logging.LoggingRequestHelper;
import org.apache.ambari.server.controller.logging.LoggingRequestHelperFactory;
import org.apache.ambari.server.controller.logging.LoggingRequestHelperFactoryImpl;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.utils.RetryHelper;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This Service provides access to the LogSearch query services, including:
 *     - Access to all service log files in a given cluster
 *     - Search query capability across the service logs
 *     - Metrics data regarding logging (log level counts, etc)
 *
 */
public class LoggingService extends BaseService {

  private final ControllerFactory controllerFactory;

  private final LoggingRequestHelperFactory helperFactory;


  private final String clusterName;

  public LoggingService(String clusterName) {
    this(clusterName, new DefaultControllerFactory(), new LoggingRequestHelperFactoryImpl());
  }

  public LoggingService(String clusterName, ControllerFactory controllerFactory, LoggingRequestHelperFactory helperFactory) {
    this.clusterName = clusterName;
    this.controllerFactory = controllerFactory;
    this.helperFactory = helperFactory;
  }

  @GET
  @Path("searchEngine")
  @Produces("text/plain")
  public Response getSearchEngine(String body, @Context HttpHeaders headers, @Context UriInfo uri) {
    //TODO, fix this cast after testing,RWN
    return handleDirectRequest(uri, MediaType.TEXT_PLAIN_TYPE);
  }

  /**
   * Handling method for REST services that don't require the QueryParameter and
   * partial-response syntax support provided by the Ambari REST framework.
   *
   * In the case of the LoggingService, the query parameters passed to the search engine must
   * be preserved, since they are passed to the LogSearch REST API.
   *
   * @param uriInfo URI information for this request
   * @param mediaType media type for this request
   *
   * @return a Response for the request associated with this UriInfo
   */
  protected Response handleDirectRequest(UriInfo uriInfo, MediaType mediaType) {

    MultivaluedMap<String, String> queryParameters =
      uriInfo.getQueryParameters();

    Map<String, String> enumeratedQueryParameters =
      new HashMap<String, String>();


    for (String queryName : queryParameters.keySet()) {
      List<String> queryValue = queryParameters.get(queryName);
      for (String value : queryValue) {
        enumeratedQueryParameters.put(queryName, value);
      }
    }

    AmbariManagementController controller =
      controllerFactory.getController();

    LoggingRequestHelper requestHelper =
      helperFactory.getHelper(controller, clusterName);

    LogQueryResponse response =
      requestHelper.sendQueryRequest(enumeratedQueryParameters);

    if (response != null) {
      ResultSerializer serializer = mediaType == null ? getResultSerializer() : getResultSerializer(mediaType);

      Result result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.OK));

      Resource loggingResource = new ResourceImpl(Resource.Type.LoggingQuery);
      // include the top-level query result properties
      loggingResource.setProperty("startIndex", response.getStartIndex());
      loggingResource.setProperty("pageSize", response.getPageSize());
      loggingResource.setProperty("resultSize", response.getResultSize());
      loggingResource.setProperty("queryTimeMMS", response.getQueryTimeMS());
      loggingResource.setProperty("totalCount", response.getTotalCount());

      // include the individual responses
      loggingResource.setProperty("logList", response.getListOfResults());

      result.getResultTree().addChild(loggingResource, "logging");

      Response.ResponseBuilder builder = Response.status(result.getStatus().getStatusCode()).entity(
        serializer.serialize(result));


      if (mediaType != null) {
        builder.type(mediaType);
      }

      RetryHelper.clearAffectedClusters();
      return builder.build();
    }



    //TODO, add error handling and logging here, RWN
    return null;
  }


  /**
   * Internal interface that defines an access factory for the
   * AmbariManagementController.  This facilitates simpler unit testing.
   *
   */
  interface ControllerFactory {
    AmbariManagementController getController();
  }

  /**
   * Default implementation of the internal ControllerFactory interface,
   * which uses the AmbariServer static method to obtain the controller.
   */
  private static class DefaultControllerFactory implements ControllerFactory {
    @Override
    public AmbariManagementController getController() {
      return AmbariServer.getController();
    }
  }
}
