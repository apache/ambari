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

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.parsers.RequestBodyParser;
import org.apache.ambari.server.api.services.serializers.ResultSerializer;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Unit test for {@link AdminSettingService}
 */
public class AdminSettingServiceTest extends BaseServiceTest {
  @Override
  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    List<ServiceTestInvocation> listInvocations = new ArrayList<>();

    //getSetting
    AdminSettingService adminSettingService = new TestAdminSettingService("settingName");
    Method m = adminSettingService.getClass().getMethod("getSetting", String.class, HttpHeaders.class, UriInfo.class, String.class);
    Object[] args = new Object[] {null, getHttpHeaders(), getUriInfo(), "settingName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, adminSettingService, m, args, null));

    //getSettings
    adminSettingService = new TestAdminSettingService(null);
    m = adminSettingService.getClass().getMethod("getSettings", String.class, HttpHeaders.class, UriInfo.class);
    args = new Object[] {null, getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, adminSettingService, m, args, null));

    //createSetting
    adminSettingService = new TestAdminSettingService(null);
    m = adminSettingService.getClass().getMethod("createSetting", String.class, HttpHeaders.class, UriInfo.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST, adminSettingService, m, args, "body"));

    //updateSetting
    adminSettingService = new TestAdminSettingService("settingName");
    m = adminSettingService.getClass().getMethod("updateSetting", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "settingName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.PUT, adminSettingService, m, args, "body"));

    //deleteSetting
    adminSettingService = new TestAdminSettingService("settingName");
    m = adminSettingService.getClass().getMethod("deleteSetting", HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {getHttpHeaders(), getUriInfo(), "settingName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.DELETE, adminSettingService, m, args, null));

    return listInvocations;
  }

  private class TestAdminSettingService extends AdminSettingService {
    private String adminSettingName;

    private TestAdminSettingService(String settingName) {
      this.adminSettingName = settingName;
    }

    @Override
    ResourceInstance createSettingResource(String settingName) {
      assertEquals(this.adminSettingName, settingName);
      return getTestResource();
    }

    @Override
    RequestFactory getRequestFactory() {
      return getTestRequestFactory();
    }

    @Override
    protected RequestBodyParser getBodyParser() {
      return getTestBodyParser();
    }

    @Override
    protected ResultSerializer getResultSerializer() {
      return getTestResultSerializer();
    }
  }
}
