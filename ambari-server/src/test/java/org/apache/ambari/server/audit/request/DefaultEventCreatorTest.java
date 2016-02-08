/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.audit.request;

import junit.framework.Assert;

import java.util.HashMap;

import org.apache.ambari.server.api.query.QueryImpl;
import org.apache.ambari.server.api.resources.HostComponentResourceDefinition;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.LocalUriInfo;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.RequestBody;
import org.apache.ambari.server.api.services.RequestFactory;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.audit.request.eventcreator.DefaultEventCreator;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Before;
import org.junit.Test;

public class DefaultEventCreatorTest {

  private DefaultEventCreator defaultEventCreator;
  private RequestFactory requestFactory = new RequestFactory();

  @Before
  public void before() {
    defaultEventCreator = new DefaultEventCreator();
  }

  @Test
  public void defaultEventCreatorTest() {
    ResourceInstance resource = new QueryImpl(new HashMap<Resource.Type, String>(), new HostComponentResourceDefinition(), null);
    Request request =  requestFactory.createRequest(null, new RequestBody(), new LocalUriInfo("http://apache.org"), Request.Type.POST, resource);
    Result result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.OK, "message"));

    String actual = defaultEventCreator.createAuditEvent(request, result).getAuditMessage();
    String expected = "POST http://apache.org, 200 OK (message)";
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void defaultEventCreatorTest__noStatusMessage() {
    ResourceInstance resource = new QueryImpl(new HashMap<Resource.Type, String>(), new HostComponentResourceDefinition(), null);
    Request request =  requestFactory.createRequest(null, new RequestBody(), new LocalUriInfo("http://apache.org"), Request.Type.POST, resource);
    Result result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.OK));

    String actual = defaultEventCreator.createAuditEvent(request, result).getAuditMessage();
    String expected = "POST http://apache.org, 200 OK ()";
    Assert.assertEquals(expected, actual);
  }
}