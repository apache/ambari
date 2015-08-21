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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.inject.Singleton;
import org.apache.ambari.server.state.stack.UpgradePack;

/**
 * The {@link UpgradeCheckRegistry} contains the ordered list of all pre-upgrade
 * checks. This will order the checks according to
 * {@link PreUpgradeCheckComparator}.
 */
@Singleton
public class UpgradeCheckRegistry {

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

  /**
   * Gets an ordered and filtered list of the upgrade checks.
   * @param upgradePack Upgrade pack object with the list of required checks to be included
   * @return
   */
  public List<AbstractCheckDescriptor> getFilteredUpgradeChecks(UpgradePack upgradePack){
    List<String> prerequisiteChecks = upgradePack.getPrerequisiteChecks();
    List<AbstractCheckDescriptor> resultCheckDescriptor = new ArrayList<AbstractCheckDescriptor>();
    for (AbstractCheckDescriptor descriptor: m_upgradeChecks){
      if (descriptor.isRequired()){
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
