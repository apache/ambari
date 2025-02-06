/*
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

package org.apache.ambari.server.api;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;

import org.junit.Test;

public class AmbariCsrfProtectionFilterTest {

  @Test
  public void testGetMethod() throws IOException {
    AmbariCsrfProtectionFilter filter = new AmbariCsrfProtectionFilter();
    ContainerRequestContext containerRequest = createMock(ContainerRequestContext.class);
    expect(containerRequest.getMethod()).andReturn("GET");
    replay(containerRequest);
    filter.filter(containerRequest);
  }

  @Test(expected = WebApplicationException.class)
  public void testPostNoXRequestedBy() throws IOException {
    AmbariCsrfProtectionFilter filter = new AmbariCsrfProtectionFilter();
    ContainerRequestContext containerRequest = createMock(ContainerRequestContext.class);
    MultivaluedHashMap headers = new MultivaluedHashMap();
    expect(containerRequest.getMethod()).andReturn("POST");
    expect(containerRequest.getHeaders()).andReturn(headers);
    replay(containerRequest);
    filter.filter(containerRequest);
  }

  @Test
  public void testPostXRequestedBy() throws IOException {
    AmbariCsrfProtectionFilter filter = new AmbariCsrfProtectionFilter();
    ContainerRequestContext containerRequest = createMock(ContainerRequestContext.class);
    MultivaluedHashMap headers = new MultivaluedHashMap();
    headers.add("X-Requested-By","anything");
    expect(containerRequest.getMethod()).andReturn("GET");
    expect(containerRequest.getHeaders()).andReturn(headers);
    replay(containerRequest);
    filter.filter(containerRequest);
  }

}
