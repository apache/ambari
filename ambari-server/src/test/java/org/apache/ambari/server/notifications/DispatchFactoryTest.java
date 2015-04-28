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
package org.apache.ambari.server.notifications;

import java.io.File;
import java.util.Properties;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.notifications.dispatchers.EmailDispatcher;
import org.apache.ambari.server.notifications.dispatchers.SNMPDispatcher;
import org.junit.Assert;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * Tests {@link DispatchFactory}.
 */
public class DispatchFactoryTest {

  /**
   * Verify that know {@link NotificationDispatcher}s are registered via the
   * Guice {@link Module} and that they singletons.
   *
   * @throws Exception
   */
  @Test
  public void testDispatchFactoryRegistration() throws Exception {
    String sourceResourceDirectory = "src" + File.separator + "test"
        + File.separator + "resources";

    Properties properties = new Properties();
    properties.setProperty(Configuration.SERVER_PERSISTENCE_TYPE_KEY,"in-memory");
    properties.setProperty(Configuration.OS_VERSION_KEY, "centos6");
    properties.setProperty(Configuration.SHARED_RESOURCES_DIR_KEY,sourceResourceDirectory);

    Injector injector = Guice.createInjector(new ControllerModule(properties));
    DispatchFactory dispatchFactory = injector.getInstance(DispatchFactory.class);
    DispatchFactory dispatchFactory2 = injector.getInstance(DispatchFactory.class);

    // verify singleton
    Assert.assertEquals(dispatchFactory, dispatchFactory2);

    EmailDispatcher emailDispatcher = injector.getInstance(EmailDispatcher.class);
    EmailDispatcher emailDispatcher2 = (EmailDispatcher) dispatchFactory.getDispatcher(emailDispatcher.getType());

    Assert.assertNotNull(emailDispatcher);
    Assert.assertNotNull(emailDispatcher2);

    // verify singleton
    Assert.assertEquals(emailDispatcher, emailDispatcher2);

    SNMPDispatcher snmpDispatcher = injector.getInstance(SNMPDispatcher.class);
    SNMPDispatcher snmpDispatcher2 = (SNMPDispatcher) dispatchFactory.getDispatcher(snmpDispatcher.getType());

    Assert.assertNotNull(snmpDispatcher);
    Assert.assertNotNull(snmpDispatcher2);

    // verify singleton
    Assert.assertEquals(snmpDispatcher, snmpDispatcher2);
  }

}
