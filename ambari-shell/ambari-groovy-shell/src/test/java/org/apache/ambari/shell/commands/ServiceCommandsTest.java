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

import static org.mockito.Mockito.verify;

import org.apache.ambari.groovy.client.AmbariClient;
import org.apache.ambari.shell.completion.Service;
import org.apache.ambari.shell.model.AmbariContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ServiceCommandsTest {

  @InjectMocks
  private ServiceCommands serviceCommands;

  @Mock
  private AmbariClient client;
  @Mock
  private AmbariContext context;

  @Test
  public void testStopAllServices() {
    serviceCommands.stopServices(null);

    verify(client).stopAllServices();
  }

  @Test
  public void testStopService() {
    serviceCommands.stopServices(new Service("ZOOKEEPER"));

    verify(client).stopService("ZOOKEEPER");
  }

  @Test
  public void testStartAllServices() {
    serviceCommands.startServices(null);

    verify(client).startAllServices();
  }

  @Test
  public void testStartService() {
    serviceCommands.startServices(new Service("ZOOKEEPER"));

    verify(client).startService("ZOOKEEPER");
  }

}
