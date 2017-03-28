/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive20.internal.parsers;

import org.apache.ambari.view.hive20.client.DatabaseMetadataWrapper;
import org.apache.ambari.view.hive20.exceptions.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

public class DatabaseMetadataExtractor {
  private static final Logger LOG = LoggerFactory.getLogger(DatabaseMetadataExtractor.class);

  private final DatabaseMetaData databaseMetaData;

  public DatabaseMetadataExtractor(DatabaseMetaData databaseMetaData) {
    this.databaseMetaData = databaseMetaData;
  }

  public DatabaseMetadataWrapper extract() throws ServiceException {
    try {
      return new DatabaseMetadataWrapper(databaseMetaData.getDatabaseMajorVersion(), databaseMetaData.getDatabaseMinorVersion());
    } catch (SQLException e) {
      LOG.error("Error occurred while fetching version from database metadata.", e);
      throw new ServiceException(e);
    }
  }
}
