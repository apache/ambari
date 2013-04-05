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
package org.apache.ambari.server.security.authorization;

import com.google.inject.AbstractModule;
import org.apache.ambari.server.configuration.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import java.lang.reflect.Constructor;
import java.util.Properties;

public class AuthorizationTestModule extends AbstractModule {
  @Override
  protected void configure() {

    bind(PasswordEncoder.class).to(StandardPasswordEncoder.class);
    bind(Properties.class).toInstance(buildTestProperties());
    bind(Configuration.class).toConstructor(getConfigurationConstructor());
  }

  protected Properties buildTestProperties() {
    Properties properties = new Properties();
    properties.setProperty(Configuration.CLIENT_SECURITY_KEY, "ldap");
    return properties;
  }

  protected Constructor<Configuration> getConfigurationConstructor() {
    try {
      return Configuration.class.getConstructor(Properties.class);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("Expected constructor not found in Configuration.java", e);
    }
  }
}
