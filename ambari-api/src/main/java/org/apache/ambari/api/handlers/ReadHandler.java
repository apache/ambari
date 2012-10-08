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

package org.apache.ambari.api.handlers;

import org.apache.ambari.api.services.Request;
import org.apache.ambari.api.services.Result;
import org.apache.ambari.api.query.Query;
import org.apache.ambari.server.AmbariException;

/**
 * Responsible for read requests.
 */
public class ReadHandler implements RequestHandler {

  @Override
  public Result handleRequest(Request request) {
    Query query = request.getResourceDefinition().getQuery();

    //Partial response
    //todo: could be encapsulated in request/query
    for (String s : request.getPartialResponseFields()) {
      int i = s.lastIndexOf('/');
      if (i == -1) {
        query.addProperty(null, s);
      } else {
        query.addProperty(s.substring(0, i), s.substring(i + 1));
      }
    }

    try {
      return query.execute();
    } catch (AmbariException e) {
      //TODO: exceptions
      throw new RuntimeException("An exception occurred processing the request: " + e, e);
    }
  }
}
