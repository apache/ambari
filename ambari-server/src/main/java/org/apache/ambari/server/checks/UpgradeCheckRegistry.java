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
import java.io.FilenameFilter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

import com.google.inject.Singleton;

/**
 * The {@link UpgradeCheckRegistry} contains the ordered list of all pre-upgrade
 * checks. This will order the checks according to
 * {@link PreUpgradeCheckComparator}.
 */
@Singleton
public class UpgradeCheckRegistry {
  private static Logger LOG = LoggerFactory.getLogger(UpgradeCheckRegistry.class);

  /**
   * The list of upgrade checks to run through.
   */
  private Set<AbstractCheckDescriptor> m_upgradeChecks = new TreeSet<AbstractCheckDescriptor>(
      new PreUpgradeCheckComparator());

  /**
   * Register an upgrade check.
   *
   * @param upgradeCheck
   *          the check to register (not {@code null}).
   */
  public void register(AbstractCheckDescriptor upgradeCheck) {
    m_upgradeChecks.add(upgradeCheck);
  }

  /**
   * Gets an ordered list of all of the upgrade checks.
   *
   * @return
   */
  public List<AbstractCheckDescriptor> getUpgradeChecks() {
    return new ArrayList<AbstractCheckDescriptor>(m_upgradeChecks);
  }

  public List<AbstractCheckDescriptor> getServiceLevelUpgradeChecks(UpgradePack upgradePack, Map<String, ServiceInfo> services) {
    List<String> prerequisiteChecks = upgradePack.getPrerequisiteChecks();
    List<String> missingChecks = new ArrayList<String>();
    for (String prerequisiteCheck : prerequisiteChecks) {
      if (!isRegistered(prerequisiteCheck)) {
        missingChecks.add(prerequisiteCheck);
      }
    }

    List<AbstractCheckDescriptor> checks = new ArrayList<>(missingChecks.size());
    if (missingChecks.isEmpty()) {
      return checks;
    }

    List<URL> urls = new ArrayList<URL>();
    for (ServiceInfo service : services.values()) {
      File dir = service.getChecksFolder();
      File[] jars = dir.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          return name.endsWith(".jar");
        }
      });
      for (File jar : jars) {
        try {
          URL url = jar.toURI().toURL();
          urls.add(url);
          LOG.debug("Adding service check jar to classpath: {}", url.toString());
        }
        catch (Exception e) {
          LOG.error("Failed to add service check jar to classpath: {}", jar.getAbsolutePath(), e);
        }
      }
    }

    ClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]), ClassUtils.getDefaultClassLoader());
    for (String prerequisiteCheck : missingChecks) {
      Class<?> clazz = null;
      try {
        clazz = ClassUtils.resolveClassName(prerequisiteCheck, classLoader);
      }
      catch (IllegalArgumentException illegalArgumentException) {
        LOG.error("Unable to find upgrade check {}", prerequisiteCheck, illegalArgumentException);
      }
      try {
        if (clazz != null) {
          AbstractCheckDescriptor upgradeCheck = (AbstractCheckDescriptor) clazz.newInstance();
          checks.add(upgradeCheck);
        }
      } catch (Exception exception) {
        LOG.error("Unable to create upgrade check {}", prerequisiteCheck, exception);
      }
    }
    return checks;
  }

  private boolean isRegistered(String prerequisiteCheck) {
    for (AbstractCheckDescriptor descriptor: m_upgradeChecks){
      if (prerequisiteCheck.equals(descriptor.getClass().getName())){
        return true;
      }
    }
    return false;
  }

  /**
   * Gets an ordered and filtered list of the upgrade checks.
   * @param upgradePack Upgrade pack object with the list of required checks to be included
   * @return
   */
  public List<AbstractCheckDescriptor> getFilteredUpgradeChecks(UpgradePack upgradePack){
    List<String> prerequisiteChecks = upgradePack.getPrerequisiteChecks();
    List<AbstractCheckDescriptor> resultCheckDescriptor = new ArrayList<AbstractCheckDescriptor>();
    for (AbstractCheckDescriptor descriptor: m_upgradeChecks){
      if (descriptor.isRequired(upgradePack.getType())) {
        resultCheckDescriptor.add(descriptor);
      } else if (prerequisiteChecks.contains(descriptor.getClass().getName())){
        resultCheckDescriptor.add(descriptor);
      }
    }
    return resultCheckDescriptor;
  }

  /**
   * THe {@link PreUpgradeCheckComparator} class is used to compare
   * {@link AbstractCheckDescriptor} based on their {@link UpgradeCheck}
   * annotations.
   */
  private static final class PreUpgradeCheckComparator implements
      Comparator<AbstractCheckDescriptor> {

    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(AbstractCheckDescriptor check1, AbstractCheckDescriptor check2) {
      Class<? extends AbstractCheckDescriptor> clazz1 = check1.getClass();
      Class<? extends AbstractCheckDescriptor> clazz2 = check2.getClass();

      UpgradeCheck annotation1 = clazz1.getAnnotation(UpgradeCheck.class);
      UpgradeCheck annotation2 = clazz2.getAnnotation(UpgradeCheck.class);

      UpgradeCheckGroup group1 = UpgradeCheckGroup.DEFAULT;
      UpgradeCheckGroup group2 = UpgradeCheckGroup.DEFAULT;
      Float groupOrder1 = Float.valueOf(group1.getOrder());
      Float groupOrder2 = Float.valueOf(group2.getOrder());

      Float order1 = 1.0f;
      Float order2 = 1.0f;

      if (null != annotation1) {
        group1 = annotation1.group();
        groupOrder1 = Float.valueOf(group1.getOrder());
        order1 = Float.valueOf(annotation1.order());
      }

      if (null != annotation2) {
        group2 = annotation2.group();
        groupOrder2 = Float.valueOf(group2.getOrder());
        order2 = Float.valueOf(annotation2.order());
      }

      int groupComparison = groupOrder1.compareTo(groupOrder2);
      if (groupComparison != 0) {
        return groupComparison;
      }

      int orderComparison = order1.compareTo(order2);
      if (orderComparison != 0) {
        return orderComparison;
      }

      return clazz1.getName().compareTo(clazz2.getName());
    }
  }
}
