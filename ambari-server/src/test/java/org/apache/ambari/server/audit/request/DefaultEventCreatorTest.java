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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

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
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import com.sun.jersey.core.util.MultivaluedMapImpl;

public class DefaultEventCreatorTest {

  private DefaultEventCreator defaultEventCreator;
  private RequestFactory requestFactory = new RequestFactory();

  @BeforeClass
  public static void beforeClass() {
    SecurityContextHolder.setContext(new SecurityContext() {
      @Override
      public Authentication getAuthentication() {
        return new Authentication() {
          @Override
          public Collection<? extends GrantedAuthority> getAuthorities() {
            return null;
          }

          @Override
          public Object getCredentials() {
            return null;
          }

          @Override
          public Object getDetails() {
            return null;
          }

          @Override
          public Object getPrincipal() {
            return new User("testuser", "password", Collections.EMPTY_LIST);
          }

          @Override
          public boolean isAuthenticated() {
            return true;
          }

          @Override
          public void setAuthenticated(boolean b) throws IllegalArgumentException {

          }

          @Override
          public String getName() {
            return null;
          }
        };
      }

      @Override
      public void setAuthentication(Authentication authentication) {

      }
    });
  }

  @Before
  public void before() {
    defaultEventCreator = new DefaultEventCreator();
  }

  @Test
  public void defaultEventCreatorTest__okWithMessage() {
    ResourceInstance resource = new QueryImpl(new HashMap<Resource.Type, String>(), new HostComponentResourceDefinition(), null);
    Request request =  requestFactory.createRequest(createHttpHeaders(), new RequestBody(), new LocalUriInfo("http://apache.org"), Request.Type.POST, resource);
    Result result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.OK, "message"));

    String actual = defaultEventCreator.createAuditEvent(request, result).getAuditMessage();
    String expected = "User(testuser), RemoteIp(1.2.3.4), RequestType(POST), url(http://apache.org), ResultStatus(200 OK)";
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void defaultEventCreatorTest__errorWithMessage() {
    ResourceInstance resource = new QueryImpl(new HashMap<Resource.Type, String>(), new HostComponentResourceDefinition(), null);
    Request request =  requestFactory.createRequest(createHttpHeaders(), new RequestBody(), new LocalUriInfo("http://apache.org"), Request.Type.POST, resource);
    Result result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.BAD_REQUEST, "message"));

    String actual = defaultEventCreator.createAuditEvent(request, result).getAuditMessage();
    String expected = "User(testuser), RemoteIp(1.2.3.4), RequestType(POST), url(http://apache.org), ResultStatus(400 Bad Request), Reason(message)";
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void defaultEventCreatorTest__okWithoutMessage() {
    ResourceInstance resource = new QueryImpl(new HashMap<Resource.Type, String>(), new HostComponentResourceDefinition(), null);
    Request request =  requestFactory.createRequest(createHttpHeaders(), new RequestBody(), new LocalUriInfo("http://apache.org"), Request.Type.POST, resource);
    Result result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.OK));

    String actual = defaultEventCreator.createAuditEvent(request, result).getAuditMessage();
    String expected = "User(testuser), RemoteIp(1.2.3.4), RequestType(POST), url(http://apache.org), ResultStatus(200 OK)";
    Assert.assertEquals(expected, actual);
  }

  private HttpHeaders createHttpHeaders() {
    return new HttpHeaders() {
      @Override
      public List<String> getRequestHeader(String s) {
        return null;
      }

      @Override
      public MultivaluedMap<String, String> getRequestHeaders() {
        MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
        headers.add("X-Forwarded-For","1.2.3.4");
        return headers;
      }

      @Override
      public List<MediaType> getAcceptableMediaTypes() {
        return null;
      }

      @Override
      public List<Locale> getAcceptableLanguages() {
        return null;
      }

      @Override
      public MediaType getMediaType() {
        return null;
      }

      @Override
      public Locale getLanguage() {
        return null;
      }

      @Override
      public Map<String, Cookie> getCookies() {
        return null;
      }
    };
  }

}