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

package org.apache.ambari.server.audit.request.eventcreator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.audit.AuditEvent;
import org.apache.ambari.server.audit.RequestAuditEvent;
import org.apache.ambari.server.audit.request.RequestAuditEventCreator;
import org.apache.ambari.server.controller.spi.Resource;
import org.joda.time.DateTime;

import com.google.inject.Singleton;

/**
 * Default creator for {@link org.apache.ambari.server.audit.request.RequestAuditLogger}
 */
@Singleton
public class DefaultEventCreator implements RequestAuditEventCreator {

  /**
   * Set of {@link org.apache.ambari.server.api.services.Request.Type}s that are handled by this plugin
   */
  private Set<Request.Type> requestTypes = new HashSet<Request.Type>();

  {
    requestTypes.addAll(Arrays.asList(Request.Type.values()));
    requestTypes.remove(Request.Type.GET); // get is not handled by default
  }

  /** {@inheritDoc} */
  @Override
  public Set<Request.Type> getRequestTypes() {
    return requestTypes;
  }

  /** {@inheritDoc} */
  @Override
  public Resource.Type getResourceType() {
    // null makes this creator as default
    return null;
  }

  /**
   * Creates a simple {@link AuditEvent} with the details of request and response
   * @param request HTTP request object
   * @param result HTTP result object
   * @return
   */
  @Override
  public AuditEvent createAuditEvent(final Request request, final Result result) {
      return RequestAuditEvent.builder()
        .withTimestamp(new DateTime())
        .withRequestType(request.getRequestType())
        .withUrl(request.getURI())
        .withResultStatus(result.getStatus())
        .build();
  }

}
