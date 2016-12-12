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

import java.io.File;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.stack.StackManager;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.stack.StackManagerMock;
import org.easymock.EasyMock;
import org.springframework.beans.factory.config.BeanDefinition;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.util.Modules;

public class InMemoryDefaultTestModule extends AbstractModule {

  Properties properties = new Properties();

  /**
   * Saves all {@link ControllerModule} logic, but changes bean discovery mechanism.
   * In this implementation scan for {@link org.apache.ambari.server.EagerSingleton}
   * and {@link org.apache.ambari.server.StaticallyInject} and
   * {@link org.apache.ambari.server.AmbariService} annotations will not be run for every test.
   */
  private static class BeanDefinitionsCachingTestControllerModule extends ControllerModule {

    // Access should be synchronised to allow concurrent test runs.
    private static final AtomicReference<Set<BeanDefinition>> foundBeanDefinitions
        = new AtomicReference<>(null);

    private static final AtomicReference<Set<BeanDefinition>> foundNotificationBeanDefinitions
        = new AtomicReference<>(null);

    private static final AtomicReference<Set<BeanDefinition>> foundUpgradeChecksDefinitions
        = new AtomicReference<>(null);


    public BeanDefinitionsCachingTestControllerModule(Properties properties) throws Exception {
      super(properties);
    }

    @Override
    protected Set<BeanDefinition> bindByAnnotation(Set<BeanDefinition> beanDefinitions) {
      Set<BeanDefinition> newBeanDefinitions = super.bindByAnnotation(foundBeanDefinitions.get());
      foundBeanDefinitions.compareAndSet(null, Collections.unmodifiableSet(newBeanDefinitions));
      return null;
    }

    @Override
    protected Set<BeanDefinition> bindNotificationDispatchers(Set<BeanDefinition> beanDefinitions){
      Set<BeanDefinition> newBeanDefinitions = super.bindNotificationDispatchers(foundNotificationBeanDefinitions.get());
      foundNotificationBeanDefinitions.compareAndSet(null, Collections.unmodifiableSet(newBeanDefinitions));
      return null;
    }

    @Override
    protected Set<BeanDefinition> registerUpgradeChecks(Set<BeanDefinition> beanDefinitions){
      Set<BeanDefinition> newBeanDefinition = super.registerUpgradeChecks(foundUpgradeChecksDefinitions.get());
      foundUpgradeChecksDefinitions.compareAndSet(null, Collections.unmodifiableSet(newBeanDefinition));
      return null;
    }
  }

  @Override
  protected void configure() {
    String stacks = "src/test/resources/stacks";
    String version = "src/test/resources/version";
    String sharedResourcesDir = "src/test/resources/";
    String resourcesDir = "src/test/resources/";
    if (System.getProperty("os.name").contains("Windows")) {
      stacks = ClassLoader.getSystemClassLoader().getResource("stacks").getPath();
      version = new File(new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).getParent(), "version").getPath();
      sharedResourcesDir = ClassLoader.getSystemClassLoader().getResource("").getPath();
    }

    if (!properties.containsKey(Configuration.SERVER_PERSISTENCE_TYPE.getKey())) {
      properties.setProperty(Configuration.SERVER_PERSISTENCE_TYPE.getKey(), "in-memory");
    }

    if (!properties.containsKey(Configuration.METADATA_DIR_PATH.getKey())) {
      properties.setProperty(Configuration.METADATA_DIR_PATH.getKey(), stacks);
    }

    if (!properties.containsKey(Configuration.SERVER_VERSION_FILE.getKey())) {
      properties.setProperty(Configuration.SERVER_VERSION_FILE.getKey(), version);
    }

    if (!properties.containsKey(Configuration.OS_VERSION.getKey())) {
      properties.setProperty(Configuration.OS_VERSION.getKey(), "centos5");
    }

    if (!properties.containsKey(Configuration.SHARED_RESOURCES_DIR.getKey())) {
      properties.setProperty(Configuration.SHARED_RESOURCES_DIR.getKey(), sharedResourcesDir);
    }

    if (!properties.containsKey(Configuration.RESOURCES_DIR.getKey())) {
      properties.setProperty(Configuration.RESOURCES_DIR.getKey(), resourcesDir);
    }

    try {
      install(Modules.override(new BeanDefinitionsCachingTestControllerModule(properties)).with(new AbstractModule() {
        @Override
        protected void configure() {
          // Cache parsed stacks.
          install(new FactoryModuleBuilder().implement(StackManager.class, StackManagerMock.class).build(StackManagerFactory.class));
        }
      }));
      AuditLogger al = EasyMock.createNiceMock(AuditLogger.class);
      EasyMock.expect(al.isEnabled()).andReturn(false).anyTimes();
      bind(AuditLogger.class).toInstance(al);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Gets the properties that will be used to initialize the system. If a
   * property is placed here which {@link #configure()} also sets, then
   * {@link #configure()} will not set it, and instead take the property that
   * the test has set.
   *
   * @return
   */
  public Properties getProperties() {
    return properties;
  }
}
