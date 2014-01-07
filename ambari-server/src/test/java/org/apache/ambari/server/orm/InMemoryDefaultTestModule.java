package org.apache.ambari.server.orm;

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

import com.google.inject.AbstractModule;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.scheduler.ExecutionScheduleManagerTest;
import org.apache.ambari.server.scheduler.ExecutionScheduler;

import java.util.Properties;

public class InMemoryDefaultTestModule extends AbstractModule {
  Properties properties = new Properties();

  @Override
  protected void configure() {
    properties.setProperty(Configuration.SERVER_PERSISTENCE_TYPE_KEY, "in-memory");
    properties.setProperty(Configuration.METADETA_DIR_PATH,
        "src/test/resources/stacks");
    properties.setProperty(Configuration.SERVER_VERSION_FILE,
            "target/version");
    properties.setProperty(Configuration.OS_VERSION_KEY,
        "centos5");
    try {
      install(new ControllerModule(properties));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Properties getProperties() {
    return properties;
  }
}
