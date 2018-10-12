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

import org.apache.ambari.annotations.UpgradeCheckInfo;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.spi.upgrade.CheckQualification;
import org.apache.ambari.spi.upgrade.UpgradeCheck;
import org.apache.ambari.spi.upgrade.UpgradeCheckRequest;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.apache.commons.lang.ArrayUtils;

/**
 * The {@link UpgradeTypeQualification} class is used to determine if the check
 * is required to run based on the {@link UpgradeType}. {@link UpgradeCheck}s
 * which do not specify any {@link UpgradeType}s will always return
 * {@code true}.
 */
 public final class UpgradeTypeQualification implements CheckQualification {

  private final Class<? extends UpgradeCheck> m_checkClass;

  /**
   * Constructor.
   *
   * @param checkClass
   *          the class of the check which is being considered for
   *          applicability.
   */
  public UpgradeTypeQualification(Class<? extends UpgradeCheck> checkClass) {
    m_checkClass = checkClass;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isApplicable(UpgradeCheckRequest request) throws AmbariException {
    UpgradeType upgradeType = request.getUpgradeType();

    UpgradeCheckInfo annotation = m_checkClass.getAnnotation(UpgradeCheckInfo.class);
    if (null == annotation) {
      return false;
    }

    // empty upgrade type or matching should return true
    UpgradeType[] upgradeTypes = annotation.required();
    if (ArrayUtils.isEmpty(upgradeTypes) || ArrayUtils.contains(upgradeTypes, upgradeType)) {
      return true;
    }

    return false;
  }
}