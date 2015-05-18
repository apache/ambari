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

package org.apache.ambari.server.upgrade;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.utils.VersionUtils;

import java.sql.SQLException;

/**
 * Final upgrade catalog which simply updates database version (in case if no db changes between releases)
 */
public class FinalUpgradeCatalog extends AbstractUpgradeCatalog {

  @Inject
  public FinalUpgradeCatalog(Injector injector) {
    super(injector);
  }

  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    //noop
  }

  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
    //noop
  }

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    //noop
  }

  @Override
  public String getTargetVersion() {
    return getFinalVersion();
  }

  @Override
  public boolean isFinal() {
    return true;
  }

  private String getFinalVersion() {
    return VersionUtils.getVersionSubstring(configuration.getServerVersion());
  }
}
