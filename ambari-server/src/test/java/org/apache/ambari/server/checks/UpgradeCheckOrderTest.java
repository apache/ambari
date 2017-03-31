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
package org.apache.ambari.server.checks;

import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.ambari.server.audit.AuditLoggerModule;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ControllerModule;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Tests the order of pre-upgrade checks.
 */
public class UpgradeCheckOrderTest {

  /**
   * Tests that instances of {@link AbstractCheckDescriptor} are ordered
   * correctly.
   *
   * @throws Exception
   */
  @Test
  public void testUpgradeOrder() throws Exception {
    String sourceResourceDirectory = "src" + File.separator + "test" + File.separator + "resources";

    Properties properties = new Properties();
    properties.setProperty(Configuration.SERVER_PERSISTENCE_TYPE.getKey(), "in-memory");
    properties.setProperty(Configuration.OS_VERSION.getKey(), "centos6");
    properties.setProperty(Configuration.SHARED_RESOURCES_DIR.getKey(), sourceResourceDirectory);

    Injector injector = Guice.createInjector(new ControllerModule(properties), new AuditLoggerModule());
    UpgradeCheckRegistry registry = injector.getInstance(UpgradeCheckRegistry.class);
    UpgradeCheckRegistry registry2 = injector.getInstance(UpgradeCheckRegistry.class);

    // verify singleton
    Assert.assertEquals(registry, registry2);

    // get the check list
    List<AbstractCheckDescriptor> checks = registry.getUpgradeChecks();

    // scan for all checks
    ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
    AssignableTypeFilter filter = new AssignableTypeFilter(AbstractCheckDescriptor.class);
    scanner.addIncludeFilter(filter);

    // grab all check subclasses using the exact folder they are in to avoid loading the SampleServiceCheck from the test jar
    Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents("org.apache.ambari.server.checks");

    // verify they are equal
    Assert.assertEquals(beanDefinitions.size(), checks.size());

    AbstractCheckDescriptor lastCheck = null;
    for (AbstractCheckDescriptor check : checks) {
      UpgradeCheckGroup group = UpgradeCheckGroup.DEFAULT;
      UpgradeCheckGroup lastGroup = UpgradeCheckGroup.DEFAULT;

      if (null != lastCheck) {

        UpgradeCheck annotation = check.getClass().getAnnotation(UpgradeCheck.class);
        UpgradeCheck lastAnnotation = lastCheck.getClass().getAnnotation(UpgradeCheck.class);

        if (null != annotation && null != lastAnnotation) {
          group = annotation.group();
          lastGroup = lastAnnotation.group();
          Assert.assertTrue(lastGroup.getOrder().compareTo(group.getOrder()) <= 0);
        }
      }

      lastCheck = check;
    }
  }
}
