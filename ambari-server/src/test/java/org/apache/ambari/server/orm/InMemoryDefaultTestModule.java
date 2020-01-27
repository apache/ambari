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
package org.apache.ambari.server.orm;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.ldap.LdapModule;
import org.apache.ambari.server.ldap.service.AmbariLdapConfigurationProvider;
import org.apache.ambari.server.stack.StackManager;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.stack.StackManagerMock;
import org.easymock.EasyMock;
import org.springframework.beans.factory.config.BeanDefinition;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.util.Modules;

public class InMemoryDefaultTestModule extends AbstractModule {

  /**
   * Properties instance with ability to mock test-specific properties
   */
  public static class MockedProperties extends Properties {

    private static final String STACKS_DIR = "src/test/resources/stacks";
    private static final String VERSION_FILE = "src/test/resources/version";
    private static final String RESOURCE_ROOT = "src/test/resources/";

    private static final String REAL_RESOURCE_ROOT = "src/main/resources";
    private static final String REAL_STACKS_DIR = "src/main/resources/stacks";
    private static final String REAL_COMMON_SERVICES_DIR = "src/main/resources/common-services";

    private static final String TEMP_DIR_PATTERN = "Temp";

    private File stacks, version, resourcesRoot, commonServices;
    boolean useRealPath = false;
    boolean setExtraProperties = false;

    public MockedProperties() {
      super();
      initBaseProperties(null, null, null, null);
    }

    public MockedProperties(boolean useRealPath, boolean setExtraProperties) {
      super();
      this.useRealPath = useRealPath;
      this.setExtraProperties = setExtraProperties;
      initBaseProperties(null, null, null, null);
    }

    public MockedProperties(boolean useRealPath, boolean setExtraProperties, @Nullable String stacksDir,
                            @Nullable String versionFile, @Nullable String resourceRootDir,
                            @Nullable String commonServicesDir) {
      super();
      this.useRealPath = useRealPath;
      this.setExtraProperties = setExtraProperties;
      initBaseProperties(stacksDir, versionFile, resourceRootDir, commonServicesDir);
    }

    protected void initBaseProperties(@Nullable String stacksDir, @Nullable String versionFile,
                                      @Nullable String resourceRootDir, @Nullable String commonServicesDir) {

      if (System.getProperty("os.name").contains("Windows")) {
        // here is a resource root in target/test-classes/*
        stacks = new File(Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("stacks")).getPath());

        File realResourceRoot, realTestResourceRoot;
        try {
          File realRootDir = new File(stacks.getParent(), "../..").getCanonicalFile();
          realResourceRoot = new File(realRootDir, REAL_RESOURCE_ROOT);
          realTestResourceRoot = new File(realRootDir, RESOURCE_ROOT);

        } catch (IOException e) {
          throw new RuntimeException(String.format("Unable to resolve stack root dir: %s", REAL_RESOURCE_ROOT));
        }

        version = new File(realTestResourceRoot, "version");  // should be always be resolved from the test dir
        resourcesRoot = (useRealPath)
          ? realResourceRoot
          : realTestResourceRoot;

        stacks = new File(resourcesRoot, "stacks");
        commonServices = new File(resourcesRoot, new File(REAL_COMMON_SERVICES_DIR).getName());

        if (stacksDir != null) { // override stacks dir only in relation to actual resources dir
          stacks = (stacksDir.contains(TEMP_DIR_PATTERN))
            ? new File(stacksDir)
            :new File(resourcesRoot, new File(stacksDir).getName());
        }

        if (resourceRootDir != null) {
         resourcesRoot = (resourceRootDir.contains(TEMP_DIR_PATTERN))
           ? new File(resourceRootDir)
           : new File(resourcesRoot, new File(resourceRootDir).getName());
        }

        if (commonServicesDir != null) {
          commonServices = (commonServicesDir.contains(TEMP_DIR_PATTERN))
            ? new File(commonServicesDir)
            : new File(resourcesRoot, new File(commonServicesDir).getName());
        }
      } else {
        stacks = new File(Optional.ofNullable(stacksDir).orElse(
          (useRealPath)
          ? REAL_STACKS_DIR
          : STACKS_DIR
        ));
        resourcesRoot = new File(Optional.ofNullable(resourceRootDir).orElse(
          (useRealPath)
          ? REAL_RESOURCE_ROOT
          : RESOURCE_ROOT
        ));
        version = new File(Optional.ofNullable(versionFile).orElse(VERSION_FILE));
        commonServices = new File(Optional.ofNullable(commonServicesDir).orElse(REAL_COMMON_SERVICES_DIR));
      }
      setProperty(Configuration.METADATA_DIR_PATH.getKey(), stacks.getPath());
      setProperty(Configuration.SERVER_VERSION_FILE.getKey(), version.getPath());
      setProperty(Configuration.RESOURCES_DIR.getKey(), resourcesRoot.getPath());
      setProperty(Configuration.SHARED_RESOURCES_DIR.getKey(), resourcesRoot.getPath());
      if (setExtraProperties) {
        setProperty(Configuration.COMMON_SERVICES_DIR_PATH.getKey(), commonServices.getPath());
      }
    }

    public File getCommonServicesDir() {
      return commonServices;
    }

    public File getStacksDir() {
      return stacks;
    }

    public File getVersionFile() {
      return version;
    }

    public File getResourcesRoot() {
      return resourcesRoot;
    }

    public static File getCommonServiceDir() {
      return (System.getProperty("os.name").contains("Windows"))
        ? new File(getDefaultResourcesDir(), new File(REAL_COMMON_SERVICES_DIR).getName())
        : new File(REAL_COMMON_SERVICES_DIR);
    }

    public static File getDefaultStackDir() {
      if (System.getProperty("os.name").contains("Windows")) {
        File stackRoot = new File(
          Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("stacks")).getPath()
        );

        File realTestResourceRoot;
        try {
          File realRootDir = new File(stackRoot.getParent(), "../..").getCanonicalFile();
          realTestResourceRoot = new File(realRootDir, RESOURCE_ROOT);
        } catch (IOException e) {
          throw new RuntimeException(String.format("Unable to resolve stack root dir: %s", RESOURCE_ROOT));
        }

        return new File(realTestResourceRoot, new File(STACKS_DIR).getName());
      }

      return new File(STACKS_DIR);
    }

    public  static File getDefaultVersionFile() {
      return (System.getProperty("os.name").contains("Windows"))
        ? new File(getDefaultResourcesDir(), "version")
        : new File(VERSION_FILE);
    }

    public  static File getDefaultResourcesDir() {
      return (System.getProperty("os.name").contains("Windows"))
        ? new File(getDefaultStackDir().getParent())
        : new File(RESOURCE_ROOT);
    }
  }

  /**
   * MockProperties build helper
   */
  public static class MockedPropertiesBuilder {
    private String stacksDir = null;
    private String versionFile = null;
    private String resourceRootDir = null;
    private String commonServiceDir = null;
    private boolean useRealPath = false;
    private boolean setExtraProperties = false;

    public MockedPropertiesBuilder() {
      super();
    }

    public MockedPropertiesBuilder(boolean useRealPath) {
      super();
      this.useRealPath = useRealPath;
    }

    public MockedPropertiesBuilder useRealPath() {
      this.useRealPath = true;
      return this;
    }

    public MockedPropertiesBuilder setExtraProperties() {
      this.setExtraProperties = true;
      return this;
    }

    public MockedPropertiesBuilder commonServiceDir(String commonServiceDir) {
      this.commonServiceDir = commonServiceDir;
      return this;
    }

    public MockedPropertiesBuilder stacksDir(String stacksDir) {
      this.stacksDir = stacksDir;
      return this;
    }

    public MockedPropertiesBuilder versionFile(String verionFile) {
      this.versionFile = verionFile;
      return this;
    }

    public MockedPropertiesBuilder resourceRootDir(String resourceRootDir) {
      this.resourceRootDir = resourceRootDir;
      return this;
    }

    public MockedProperties build() {
      return new MockedProperties(useRealPath, setExtraProperties, stacksDir, versionFile, resourceRootDir,
        commonServiceDir);
    }
  }

  MockedProperties properties = new MockedProperties();

  /**
   * Saves all {@link ControllerModule} logic, but changes bean discovery mechanism.
   * In this implementation scan for {@link org.apache.ambari.server.EagerSingleton}
   * and {@link org.apache.ambari.server.StaticallyInject} and
   * {@link org.apache.ambari.server.AmbariService} annotations will not be run for every test.
   */
  private static class BeanDefinitionsCachingTestControllerModule extends ControllerModule {

    // Access should be synchronised to allow concurrent test runs.
    private static final AtomicReference<Set<Class<?>>> matchedAnnotationClasses
        = new AtomicReference<>(null);

    private static final AtomicReference<Set<BeanDefinition>> foundNotificationBeanDefinitions
        = new AtomicReference<>(null);

    public BeanDefinitionsCachingTestControllerModule(Properties properties) throws Exception {
      super(properties);
    }

    @Override
    protected Set<Class<?>> bindByAnnotation(Set<Class<?>> matchedClasses) {
      Set<Class<?>> newMatchedClasses = super.bindByAnnotation(matchedAnnotationClasses.get());
      matchedAnnotationClasses.compareAndSet(null, Collections.unmodifiableSet(newMatchedClasses));
      return null;
    }

    @Override
    protected Set<BeanDefinition> bindNotificationDispatchers(Set<BeanDefinition> beanDefinitions){
      Set<BeanDefinition> newBeanDefinitions = super.bindNotificationDispatchers(foundNotificationBeanDefinitions.get());
      foundNotificationBeanDefinitions.compareAndSet(null, Collections.unmodifiableSet(newBeanDefinitions));
      return null;
    }
  }

  @Override
  protected void configure() {
    if (!properties.containsKey(Configuration.SERVER_PERSISTENCE_TYPE.getKey())) {
      properties.setProperty(Configuration.SERVER_PERSISTENCE_TYPE.getKey(), "in-memory");
    }

    if (!properties.containsKey(Configuration.OS_VERSION.getKey())) {
      properties.setProperty(Configuration.OS_VERSION.getKey(), "centos5");
    }

    try {
      install(new LdapModule());
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
      bind(AmbariLdapConfigurationProvider.class).toInstance(EasyMock.createMock(AmbariLdapConfigurationProvider.class));

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
