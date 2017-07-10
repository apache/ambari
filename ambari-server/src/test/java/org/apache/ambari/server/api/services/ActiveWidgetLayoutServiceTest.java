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
package org.apache.ambari.server.api.services;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.orm.entities.WidgetLayoutEntity;
import org.junit.Test;

/**
 * Unit tests for ActiveWidgetLayoutService.
 */
public class ActiveWidgetLayoutServiceTest {

  @Test
  public void testCreateResourceWithUppercaseUsername() {
    // GIVEN
    ActiveWidgetLayoutService activeWidgetLayoutService = new TestActiveWidgetLayoutService("MyUser");
    // WHEN
    Response response = activeWidgetLayoutService.getServices(null, null, null);
    // THEN
    assertEquals("myuser", ((WidgetLayoutEntity) response.getEntity()).getUserName());
  }

  private class TestActiveWidgetLayoutService extends ActiveWidgetLayoutService {
    public TestActiveWidgetLayoutService(String username) {
      super(username);
    }

    @Override
    protected Response handleRequest(HttpHeaders headers, String body, UriInfo uriInfo,
                                     Request.Type requestType, final ResourceInstance resource) {
      return new Response() {
        @Override
        public Object getEntity() {
          WidgetLayoutEntity entity = new WidgetLayoutEntity();
          entity.setUserName(resource.getKeyValueMap().get(Resource.Type.User));
          return entity;
        }

        @Override
        public int getStatus() {
          return 0;
        }

        @Override
        public MultivaluedMap<String, Object> getMetadata() {
          return null;
        }
      };
    }
  }
}
