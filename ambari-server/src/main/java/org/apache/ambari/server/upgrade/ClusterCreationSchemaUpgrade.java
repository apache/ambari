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
import java.util.Locale;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/** Adds the immutable owner and draft identity used for retry-safe cluster creation. */
@Singleton
public class ClusterCreationSchemaUpgrade {
  static final String TABLE_NAME = "clusters";
  static final String CREATOR_USER_ID_COLUMN = "creator_user_id";
  static final String CREATION_DRAFT_ID_COLUMN = "creation_draft_id";
  static final String UNIQUE_INDEX = "uq_clusters_creation_draft";
  static final String DUPLICATE_QUERY =
      "SELECT creator_user_id, creation_draft_id, COUNT(*) FROM clusters "
          + "WHERE creator_user_id IS NOT NULL AND creation_draft_id IS NOT NULL "
          + "GROUP BY creator_user_id, creation_draft_id HAVING COUNT(*) > 1 "
          + "ORDER BY creator_user_id, creation_draft_id";

  private static final int MAX_REPORTED_DUPLICATES = 20;

  private final DBAccessor dbAccessor;

  @Inject
  public ClusterCreationSchemaUpgrade(DBAccessor dbAccessor) {
    this.dbAccessor = dbAccessor;
  }

  public void execute() throws AmbariException, SQLException {
    if (!dbAccessor.tableExists(TABLE_NAME)) {
      throw new AmbariException(
          "Cannot add cluster creation identity because the clusters table does not exist after versioned upgrades");
    }
    if (!dbAccessor.tableHasColumn(TABLE_NAME, CREATOR_USER_ID_COLUMN)) {
      dbAccessor.addColumn(TABLE_NAME,
          new DBColumnInfo(CREATOR_USER_ID_COLUMN, Integer.class, null, null, true));
    }
    if (!dbAccessor.tableHasColumn(TABLE_NAME, CREATION_DRAFT_ID_COLUMN)) {
      dbAccessor.addColumn(TABLE_NAME,
          new DBColumnInfo(CREATION_DRAFT_ID_COLUMN, String.class, 36, null, true));
    }

    List<String> duplicates = findDuplicates();
    if (!duplicates.isEmpty()) {
      throw duplicateException(duplicates, null);
    }
    if (dbAccessor.tableHasIndex(TABLE_NAME, false, UNIQUE_INDEX)) {
      return;
    }

    try {
      if (isSqlServer()) {
        dbAccessor.executeQuery("CREATE UNIQUE INDEX " + UNIQUE_INDEX + " ON " + TABLE_NAME
            + " (creator_user_id, creation_draft_id) WHERE creator_user_id IS NOT NULL "
            + "AND creation_draft_id IS NOT NULL");
      } else {
        dbAccessor.createIndex(UNIQUE_INDEX, TABLE_NAME, true,
            CREATOR_USER_ID_COLUMN, CREATION_DRAFT_ID_COLUMN);
      }
    } catch (SQLException e) {
      duplicates = findDuplicates();
      if (!duplicates.isEmpty()) {
        throw duplicateException(duplicates, e);
      }
      throw e;
    }
  }

  private boolean isSqlServer() throws SQLException {
    String product = dbAccessor.getConnection().getMetaData().getDatabaseProductName();
    String normalized = product == null ? "" : product.toLowerCase(Locale.ROOT);
    return normalized.contains("microsoft") || normalized.contains("sql server");
  }

  private List<String> findDuplicates() throws SQLException {
    List<String> duplicates = new ArrayList<>();
    int duplicateCount = 0;
    try (Statement statement = dbAccessor.getConnection().createStatement();
        ResultSet resultSet = statement.executeQuery(DUPLICATE_QUERY)) {
      while (resultSet.next()) {
        duplicateCount++;
        if (duplicates.size() < MAX_REPORTED_DUPLICATES) {
          duplicates.add(String.format("creator_user_id=%d, creation_draft_id=%s (%d clusters)",
              resultSet.getInt(1), resultSet.getString(2), resultSet.getLong(3)));
        }
      }
    }
    if (duplicateCount > MAX_REPORTED_DUPLICATES) {
      duplicates.add(String.format("and %d more identities", duplicateCount - MAX_REPORTED_DUPLICATES));
    }
    return duplicates;
  }

  private AmbariException duplicateException(List<String> duplicates, SQLException cause) {
    String message = String.format(
        "Cannot enforce unique cluster creation identity: multiple clusters use the same owner and draft: %s. "
            + "Identify the correct cluster, clear the duplicate creation identity columns explicitly, and rerun "
            + "ambari-server upgrade. No clusters were removed or renamed.",
        String.join(", ", duplicates));
    return cause == null ? new AmbariException(message) : new AmbariException(message, cause);
  }
}
