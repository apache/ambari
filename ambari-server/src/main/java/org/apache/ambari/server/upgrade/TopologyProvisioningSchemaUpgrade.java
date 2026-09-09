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
package org.apache.ambari.server.upgrade;

import java.sql.SQLException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/** Adds durable, retry-safe metadata to topology provisioning requests. */
@Singleton
public class TopologyProvisioningSchemaUpgrade {
  static final String TABLE_NAME = "topology_request";
  static final String REPOSITORY_VERSION_ID_COLUMN = "repository_version_id";
  static final String SPECIFICATION_HASH_COLUMN = "specification_hash";
  static final String PROVISIONING_STATE_COLUMN = "provisioning_state";
  static final String REPOSITORY_VERSION_FK = "FK_topology_request_repo_ver";

  private final DBAccessor dbAccessor;

  @Inject
  public TopologyProvisioningSchemaUpgrade(DBAccessor dbAccessor) {
    this.dbAccessor = dbAccessor;
  }

  public void execute() throws AmbariException, SQLException {
    if (!dbAccessor.tableExists(TABLE_NAME)) {
      throw new AmbariException(
          "Cannot add durable topology intent because the topology_request table does not exist after versioned upgrades");
    }
    if (!dbAccessor.tableHasColumn(TABLE_NAME, REPOSITORY_VERSION_ID_COLUMN)) {
      dbAccessor.addColumn(TABLE_NAME,
          new DBColumnInfo(REPOSITORY_VERSION_ID_COLUMN, Long.class, null, null, true));
    }
    if (!dbAccessor.tableHasColumn(TABLE_NAME, SPECIFICATION_HASH_COLUMN)) {
      dbAccessor.addColumn(TABLE_NAME,
          new DBColumnInfo(SPECIFICATION_HASH_COLUMN, String.class, 64, null, true));
    }
    if (!dbAccessor.tableHasColumn(TABLE_NAME, PROVISIONING_STATE_COLUMN)) {
      dbAccessor.addColumn(TABLE_NAME,
          new DBColumnInfo(PROVISIONING_STATE_COLUMN, String.class, 32, null, true));
    }
    if (!dbAccessor.tableHasForeignKey(TABLE_NAME, REPOSITORY_VERSION_FK)) {
      dbAccessor.addFKConstraint(TABLE_NAME, REPOSITORY_VERSION_FK,
          REPOSITORY_VERSION_ID_COLUMN, "repo_version", "repo_version_id", false);
    }
  }
}
