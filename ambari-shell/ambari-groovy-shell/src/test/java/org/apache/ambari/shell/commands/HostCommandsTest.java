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
package org.apache.ambari.shell.commands;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.ambari.groovy.client.AmbariClient;
import org.apache.ambari.shell.completion.Host;
import org.apache.ambari.shell.model.AmbariContext;
import org.apache.ambari.shell.model.FocusType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HostCommandsTest {

  @InjectMocks
  private HostCommands hostCommands;

  @Mock
  private AmbariClient client;
  @Mock
  private AmbariContext context;

  @Test
  public void testFocusHostForValidHost() {
    when(client.getHostNames()).thenReturn(singletonMap("host1", "HEALTHY"));

    String result = hostCommands.focusHost(new Host("host1"));

    verify(context).setFocus("host1", FocusType.HOST);
    assertEquals("Focus set to: host1", result);
  }

  @Test
  public void testFocusHostForInvalidHost() {
    when(client.getHostNames()).thenReturn(singletonMap("host3", "HEALTHY"));

    String result = hostCommands.focusHost(new Host("host1"));

    verify(context, times(0)).setFocus("host1", FocusType.HOST);
    assertEquals("host1 is not a valid host name", result);
  }
}
