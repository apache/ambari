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
package org.apache.ambari.shell.model;

import static org.junit.Assert.assertEquals;

import org.apache.ambari.groovy.client.AmbariClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class AmbariContextTest {

  @InjectMocks
  private AmbariContext ambariContext;

  @Mock
  private AmbariClient ambariClient;

  @Test
  public void testGetPromptForRoot() {
    ReflectionTestUtils.setField(ambariContext, "cluster", "single-node");

    String result = ambariContext.getPrompt();

    assertEquals(FocusType.ROOT.prefix() + ":single-node>", result);
  }

  @Test
  public void testGetPromptForRootButNotConnected() {
    ReflectionTestUtils.setField(ambariContext, "cluster", null);

    String result = ambariContext.getPrompt();

    assertEquals("ambari-shell>", result);
  }

  @Test
  public void testGetPromptForFocus() {
    ReflectionTestUtils.setField(ambariContext, "cluster", "single-node");
    ReflectionTestUtils.setField(ambariContext, "focus", new Focus("target", FocusType.HOST));

    String result = ambariContext.getPrompt();

    assertEquals(String.format("%s:%s>", FocusType.HOST.prefix(), "target"), result);
  }
}
