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

package org.apache.ambari.server.api.handlers;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.query.Query;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.Map;

/**
 * Responsible for read requests.
 */
public class ReadHandler implements RequestHandler {

  @Override
  public Result handleRequest(Request request) {
    Query query = request.getResource().getQuery();

    //Partial response
    for (Map.Entry<String, TemporalInfo> entry : request.getFields().entrySet()) {
      // Iterate over map and add props/temporalInfo
      String propertyId = entry.getKey();
      query.addProperty(PropertyHelper.getPropertyCategory(propertyId), PropertyHelper.getPropertyName(propertyId), entry.getValue());
    }

   query.setUserPredicate(request.getQueryPredicate());

    try {
      return query.execute();
    } catch (AmbariException e) {
      //TODO: exceptions
      throw new RuntimeException("An exception occurred processing the request: " + e, e);
    }
  }
}
