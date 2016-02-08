/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.audit.request;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.audit.AuditEvent;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.controller.spi.Resource;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The purpose of this class is to create audit log entries for the HTTP requests
 */
@Singleton
public class RequestAuditLoggerImpl implements RequestAuditLogger {

  /**
   * Container for the plugins grouped by {@link org.apache.ambari.server.api.services.Request.Type} and {@link org.apache.ambari.server.controller.spi.Resource.Type}
   * Note that a single plugin can belong to multiple {@link org.apache.ambari.server.api.services.Request.Type}
   */
  private Map<Request.Type, Map<Resource.Type, RequestAuditEventCreator>> creators = new HashMap<Request.Type, Map<Resource.Type, RequestAuditEventCreator>>();

  /**
   * Audit logger that receives {@link AuditEvent}s and does the actual logging
   */
  private AuditLogger auditLogger;

  /**
   * Injecting dependencies through the constructor
   * @param auditLogger Audit Logger
   * @param creatorSet Set of plugins that are registered for requests
   */
  @Inject
  public RequestAuditLoggerImpl(AuditLogger auditLogger, Set<RequestAuditEventCreator> creatorSet) {
    this.auditLogger = auditLogger;
    fillCreators(creatorSet);
  }

  /**
   * Fills up {@link RequestAuditLoggerImpl#creators}
   * @param creatorSet
   */
  private void fillCreators(Set<RequestAuditEventCreator> creatorSet) {
    for(RequestAuditEventCreator creator: creatorSet) {
      for(Request.Type rt: creator.getRequestTypes()) {
        createMapEntryIfNeeded(rt);
        creators.get(rt).put(creator.getResourceType(), creator);
      }
    }
  }

  /**
   * Creates a map entry for a {@link Request.Type} in {@link RequestAuditLoggerImpl#creators} if missing
   * @param rt
   */
  private void createMapEntryIfNeeded(Request.Type rt) {
    if(!creators.containsKey(rt)) {
      creators.put(rt, new HashMap<Resource.Type, RequestAuditEventCreator>());
    }
  }

  /**
   * Finds the proper creator, then creates and logs and {@link AuditEvent}
   * @param request
   * @param result
   */
  @Override
  public void log(Request request, Result result) {
    Resource.Type resourceType = request.getResource().getResourceDefinition().getType();
    Request.Type requestType = request.getRequestType();

    RequestAuditEventCreator creator = selectCreator(resourceType, requestType);
    if(creator != null) {
      AuditEvent ae = creator.createAuditEvent(request, result);
      auditLogger.log(ae);
    }
  }

  /**
   * Select the proper creator.
   * If there is a match, the found creator is returned.
   * If no match, the default creator is returned.
   * If there is no default creator, then null is returned.
   * @param resourceType
   * @param requestType
   * @return
   */
  private RequestAuditEventCreator selectCreator(Resource.Type resourceType, Request.Type requestType) {
    if(creators.containsKey(requestType) && creators.get(requestType).containsKey(resourceType)) {
      return creators.get(requestType).get(resourceType);
    }

    // default creator if no match is found, or null when there is no default creator
    return creators.containsKey(requestType) ? creators.get(requestType).get(null) : null;
  }
}
