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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.DBAccessor;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Enforces the schema invariant that a host belongs to at most one cluster.
 *
 * This upgrade is deliberately independent of the versioned catalog path so
 * an existing database already marked with the current development version can
 * receive the additive constraint by rerunning {@code ambari-server upgrade}.
 */
@Singleton
public class HostMembershipSchemaUpgrade {
  static final String TABLE_NAME = "clusterhostmapping";
  static final String HOST_ID_COLUMN = "host_id";
  static final String UNIQUE_CONSTRAINT = "UQ_clusterhostmapping_host_id";
  static final String DUPLICATE_QUERY =
      "SELECT host_id, COUNT(*) FROM clusterhostmapping GROUP BY host_id HAVING COUNT(*) > 1 ORDER BY host_id";

  private static final int MAX_REPORTED_DUPLICATES = 20;

  private final DBAccessor dbAccessor;

  @Inject
  public HostMembershipSchemaUpgrade(DBAccessor dbAccessor) {
    this.dbAccessor = dbAccessor;
  }

  public void execute() throws AmbariException, SQLException {
    if (!dbAccessor.tableExists(TABLE_NAME) || !dbAccessor.tableHasColumn(TABLE_NAME, HOST_ID_COLUMN)) {
      throw new AmbariException(String.format(
          "Cannot enforce exclusive host membership because %s.%s does not exist after versioned schema upgrades",
          TABLE_NAME, HOST_ID_COLUMN));
    }

    List<String> duplicateMappings = findDuplicateMappings();
    if (!duplicateMappings.isEmpty()) {
      throw duplicateMappingException(duplicateMappings, null);
    }

    try {
      dbAccessor.addUniqueConstraint(TABLE_NAME, UNIQUE_CONSTRAINT, HOST_ID_COLUMN);
    } catch (SQLException e) {
      duplicateMappings = findDuplicateMappings();
      if (!duplicateMappings.isEmpty()) {
        throw duplicateMappingException(duplicateMappings, e);
      }
      throw e;
    }

  }

  private List<String> findDuplicateMappings() throws SQLException {
    List<String> duplicates = new ArrayList<>();
    int duplicateCount = 0;
    try (Statement statement = dbAccessor.getConnection().createStatement();
        ResultSet resultSet = statement.executeQuery(DUPLICATE_QUERY)) {
      while (resultSet.next()) {
        duplicateCount++;
        if (duplicates.size() < MAX_REPORTED_DUPLICATES) {
          duplicates.add(String.format("host_id=%d (%d mappings)", resultSet.getLong(1), resultSet.getLong(2)));
        }
      }
    }
    if (duplicateCount > MAX_REPORTED_DUPLICATES) {
      duplicates.add(String.format("and %d more hosts", duplicateCount - MAX_REPORTED_DUPLICATES));
    }
    return duplicates;
  }

  private AmbariException duplicateMappingException(List<String> duplicates, SQLException cause) {
    String message = String.format(
        "Cannot enforce exclusive host membership: ClusterHostMapping contains hosts assigned to multiple clusters: %s. " +
            "Remove the conflicting mappings explicitly, verify the intended owner for each host, and rerun ambari-server upgrade. " +
            "No mappings were changed.",
        String.join(", ", duplicates));
    return cause == null ? new AmbariException(message) : new AmbariException(message, cause);
  }
}
