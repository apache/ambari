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

package org.apache.ambari.view.hello;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.apache.ambari.view.ViewContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.XmlWebApplicationContext;

class HelloControllerTest {

  private XmlWebApplicationContext applicationContext;
  private MockMvc mockMvc;
  private ViewContext viewContext;

  @BeforeEach
  void setUp() {
    viewContext = createMock(ViewContext.class);

    MockServletContext servletContext =
        new MockServletContext("src/main/webapp", new FileSystemResourceLoader());
    servletContext.setAttribute(ViewContext.CONTEXT_ATTRIBUTE, viewContext);

    applicationContext = new XmlWebApplicationContext();
    applicationContext.setServletContext(servletContext);
    applicationContext.setConfigLocation("/WEB-INF/Hello-servlet.xml");
    applicationContext.refresh();

    mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
  }

  @AfterEach
  void tearDown() {
    applicationContext.close();
  }

  @Test
  void startsViewContextAndDispatchesGreeting() throws Exception {
    expect(viewContext.getUsername()).andReturn("Ada");
    replay(viewContext);

    assertNotNull(applicationContext.getBean(HelloController.class));
    mockMvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(view().name("hello"))
        .andExpect(model().attribute("greeting", "Hello Ada!"))
        .andExpect(forwardedUrl("/WEB-INF/jsp/hello.jsp"));

    verify(viewContext);
  }

  @Test
  void rendersFallbackGreetingWithoutAUser() throws Exception {
    expect(viewContext.getUsername()).andReturn(null);
    replay(viewContext);

    mockMvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("greeting", "Hello unknown user!"));

    verify(viewContext);
  }
}
