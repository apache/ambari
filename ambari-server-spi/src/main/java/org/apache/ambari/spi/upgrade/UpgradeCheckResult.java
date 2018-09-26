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
package org.apache.ambari.spi.upgrade;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Contains information about performed prerequisite check. Newly initialized
 * {@link UpgradeCheckResult} instances are always set to
 * {@link UpgradeCheckStatus#PASS}.
 */
public class UpgradeCheckResult {
  private UpgradeCheck m_upgradeCheck;
  private UpgradeCheckStatus m_status = UpgradeCheckStatus.PASS;
  private String m_failReason = "";
  private LinkedHashSet<String> m_failedOn = new LinkedHashSet<>();
  private List<Object> m_failedDetail = new ArrayList<>();

  public UpgradeCheckResult(UpgradeCheck check) {
    m_upgradeCheck = check;
  }

  public UpgradeCheckResult(UpgradeCheck check, UpgradeCheckStatus status) {
    m_upgradeCheck = check;
    m_status = status;
  }

  public String getId() {
    return m_upgradeCheck.getCheckDescrption().name();
  }

  public String getDescription() {
    return m_upgradeCheck.getCheckDescrption().getText();
  }

  public UpgradeCheckStatus getStatus() {
    return m_status;
  }

  public void setStatus(UpgradeCheckStatus status) {
    m_status = status;
  }

  public String getFailReason() {
    return m_failReason;
  }

  public void setFailReason(String failReason) {
    m_failReason = failReason;
  }

  public LinkedHashSet<String> getFailedOn() {
    return m_failedOn;
  }

  public List<Object> getFailedDetail() {
    return m_failedDetail;
  }

  public void setFailedOn(LinkedHashSet<String> failedOn) {
    m_failedOn = failedOn;
  }

  public UpgradeCheckType getType() {
    return m_upgradeCheck.getType();
  }
}