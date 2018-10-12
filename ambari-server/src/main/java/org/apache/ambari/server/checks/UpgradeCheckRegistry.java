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
package org.apache.ambari.server.checks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.ambari.annotations.UpgradeCheckInfo;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.stack.upgrade.UpgradePack;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.spi.upgrade.UpgradeCheck;
import org.apache.ambari.spi.upgrade.UpgradeCheckGroup;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * The {@link UpgradeCheckRegistry} contains the ordered list of all pre-upgrade
 * checks. This will order the checks according to
 * {@link PreUpgradeCheckComparator}.
 */
public class UpgradeCheckRegistry {
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCheckRegistry.class);

  @Inject
  private Provider<AmbariMetaInfo> metainfoProvider;

  /**
   * The list of upgrade checks provided by the system.
   */
  private final Set<UpgradeCheck> m_builtInChecks = new TreeSet<>(
    new PreUpgradeCheckComparator());

  /**
   * The upgrade checks discovered on a per-stack, per-upgrade pack basis.
   */
  private final Map<UpgradePack, Set<UpgradeCheck>> m_pluginChecks = new HashMap<>();

  /**
   * Register an upgrade check.
   *
   * @param upgradeCheck
   *          the check to register (not {@code null}).
   */
  public void register(UpgradeCheck upgradeCheck) {
    m_builtInChecks.add(upgradeCheck);
  }

  /**
   * Gets an ordered list of all of the upgrade checks.
   *
   * @return
   */
  public List<UpgradeCheck> getUpgradeChecks() {
    return new ArrayList<>(m_builtInChecks);
  }

  /**
   * Gets an ordered and filtered list of the upgrade checks.
   * @param upgradePack Upgrade pack object with the list of required checks to be included
   * @return
   */
  @SuppressWarnings("unchecked")
  public List<UpgradeCheck> getFilteredUpgradeChecks(UpgradePack upgradePack) throws AmbariException {
    List<UpgradeCheck> upgradeChecks = new ArrayList<>();
    upgradeChecks.addAll(m_builtInChecks);

    Set<UpgradeCheck> pluginChecks = m_pluginChecks.get(upgradePack);

    // see if the upgrade checks have been processes for this pack yet
    if (null == pluginChecks) {
      pluginChecks = new TreeSet<>(new PreUpgradeCheckComparator());
      m_pluginChecks.put(upgradePack, pluginChecks);

      List<String> pluginCheckClassNames = upgradePack.getPrerequisiteChecks();
      if (null != pluginCheckClassNames && !pluginCheckClassNames.isEmpty()) {

        StackId ownerStackId = upgradePack.getOwnerStackId();
        StackInfo stackInfo = metainfoProvider.get().getStack(ownerStackId);

        ClassLoader classLoader = stackInfo.getLibraryClassLoader();
        if (null != classLoader) {
          for (String pluginCheckClassName : pluginCheckClassNames) {
            try {
              Class<? extends UpgradeCheck> upgradeCheckClass = (Class<? extends UpgradeCheck>) classLoader.loadClass(
                  pluginCheckClassName);

              pluginChecks.add(upgradeCheckClass.newInstance());
              LOG.info("Registered pre-upgrade check {} for stack {}", upgradeCheckClass,
                  ownerStackId);
            } catch (Exception exception) {
              LOG.error("Unable to load the upgrade check {}", pluginCheckClassName, exception);
            }
          }
        } else {
          LOG.error(
              "Unable to perform the following upgrade checks because no libraries could be loaded for the {} stack: {}",
              ownerStackId, StringUtils.join(pluginCheckClassNames, ","));
        }
      }
    }

    final Set<UpgradeCheck> combinedUpgradeChecks = new TreeSet<>(new PreUpgradeCheckComparator());
    combinedUpgradeChecks.addAll(m_builtInChecks);
    combinedUpgradeChecks.addAll(pluginChecks);

    return new LinkedList<>(combinedUpgradeChecks);
  }

  /**
   * Gets whether the upgrade check is required for the specified
   * {@link UpgradeType}. Checks which are marked as required do not need to be
   * explicitely declared in the {@link UpgradePack} to be run.
   *
   * @return {@code true} if it is required, {@code false} otherwise.
   */
  public static boolean isRequired(UpgradeCheck upgradeCheck, UpgradeType upgradeType) {
    UpgradeType[] upgradeTypes = upgradeCheck.getClass().getAnnotation(UpgradeCheckInfo.class).required();
    for (UpgradeType requiredType : upgradeTypes) {
      if (upgradeType == requiredType) {
        return true;
      }
    }

    return false;
  }


  /**
   * THe {@link PreUpgradeCheckComparator} class is used to compare
   * {@link UpgradeCheck} based on their {@link UpgradeCheck}
   * annotations.
   */
  private static final class PreUpgradeCheckComparator implements
      Comparator<UpgradeCheck> {

    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(UpgradeCheck check1, UpgradeCheck check2) {
      Class<? extends UpgradeCheck> clazz1 = check1.getClass();
      Class<? extends UpgradeCheck> clazz2 = check2.getClass();

      UpgradeCheckInfo annotation1 = clazz1.getAnnotation(UpgradeCheckInfo.class);
      UpgradeCheckInfo annotation2 = clazz2.getAnnotation(UpgradeCheckInfo.class);

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
