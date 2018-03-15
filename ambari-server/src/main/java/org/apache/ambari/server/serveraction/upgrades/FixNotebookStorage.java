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

package org.apache.ambari.server.serveraction.upgrades;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.utils.VersionUtils;

import com.google.inject.Inject;

/**
 * During stack upgrade,
 * if stack HDP >=2.6.3 and old value org.apache.zeppelin.notebook.repo.VFSNotebookRepo
 * update zeppelin.notebook.storage
 * to org.apache.zeppelin.notebook.repo.FileSystemNotebookRepo
 * if stack HDP >=2.6.3 and no zeppelin.config.fs.dir
 * set zeppelin.config.fs.dir to conf
 */
public class FixNotebookStorage extends AbstractServerAction {

  public static final String ZEPPELIN_NOTEBOOK_STORAGE = "zeppelin.notebook.storage";
  public static final String ZEPPELIN_CONF = "zeppelin-config";
  public static final String ORG_APACHE_ZEPPELIN_NOTEBOOK_REPO_VFSNOTEBOOK_REPO = "org.apache.zeppelin.notebook.repo.VFSNotebookRepo";
  public static final String REC_VERSION = "2.6.3.0";
  public static final String ORG_APACHE_ZEPPELIN_NOTEBOOK_REPO_FILE_SYSTEM_NOTEBOOK_REPO = "org.apache.zeppelin.notebook.repo.FileSystemNotebookRepo";
  public static final String ZEPPELIN_CONFIG_FS_DIR = "zeppelin.config.fs.dir";
  public static final String ZEPPELIN_CONFIG_FS_DIR_VALUE = "conf";


  @Inject
  private Clusters clusters;

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {
    String clusterName = getExecutionCommand().getClusterName();
    Cluster cluster = clusters.getCluster(clusterName);
    UpgradeEntity upgrade = cluster.getUpgradeInProgress();
    RepositoryVersionEntity repositoryVersionEntity = upgrade.getRepositoryVersion();
    String targetVersion = repositoryVersionEntity.getVersion();

    Config config = cluster.getDesiredConfigByType(ZEPPELIN_CONF);
    if (config == null || VersionUtils.compareVersions(targetVersion, REC_VERSION) < 0) {
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
          String.format("%s change not required", ZEPPELIN_NOTEBOOK_STORAGE), "");
    }
    Map<String, String> properties = config.getProperties();
    String oldContent = properties.get(ZEPPELIN_NOTEBOOK_STORAGE);
    String newContent = ORG_APACHE_ZEPPELIN_NOTEBOOK_REPO_FILE_SYSTEM_NOTEBOOK_REPO;

    String output = "";
    if (!properties.containsKey(ZEPPELIN_CONFIG_FS_DIR)) {
      properties.put(ZEPPELIN_CONFIG_FS_DIR, ZEPPELIN_CONFIG_FS_DIR_VALUE);
      output += String.format("set %s to %s\n",
          ZEPPELIN_NOTEBOOK_STORAGE, ORG_APACHE_ZEPPELIN_NOTEBOOK_REPO_FILE_SYSTEM_NOTEBOOK_REPO);
    }
    if (ORG_APACHE_ZEPPELIN_NOTEBOOK_REPO_VFSNOTEBOOK_REPO.equals(oldContent)) {
      properties.put(ZEPPELIN_NOTEBOOK_STORAGE, newContent);
      output += String.format("set %s to %s\n",
          ZEPPELIN_CONFIG_FS_DIR, ZEPPELIN_CONFIG_FS_DIR_VALUE);
    }
    if (output.isEmpty()) {
      output = "change not required";
    }
    config.setProperties(properties);
    config.save();
    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
        output, "");
  }
}
