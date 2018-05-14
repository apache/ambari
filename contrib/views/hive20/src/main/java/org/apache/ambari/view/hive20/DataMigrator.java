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

package org.apache.ambari.view.hive20;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.migration.ViewDataMigrationContext;
import org.apache.ambari.view.migration.ViewDataMigrationException;
import org.apache.ambari.view.migration.ViewDataMigrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * Data migrator that maps persistence entities names
 * from Hive 1 to entities of Hive 2.
 */
public class DataMigrator implements ViewDataMigrator {
  private final static Logger LOG =
      LoggerFactory.getLogger(DataMigrator.class);

  /**
   * The view context of target migration instance.
   */
  @Inject
  private ViewContext viewContext;

  /**
   * The migration context.
   */
  @Inject
  private ViewDataMigrationContext migrationContext;

  private static final Map<String, Class> hive1EntitiesMapping;
  static
  {
    hive1EntitiesMapping = new HashMap<>();

    hive1EntitiesMapping.put("org.apache.ambari.view.hive2.resources.jobs.viewJobs.JobImpl",
        org.apache.ambari.view.hive20.resources.jobs.viewJobs.JobImpl.class);
    hive1EntitiesMapping.put("org.apache.ambari.view.hive2.resources.savedQueries.SavedQuery",
        org.apache.ambari.view.hive20.resources.savedQueries.SavedQuery.class);
    hive1EntitiesMapping.put("org.apache.ambari.view.hive2.resources.udfs.UDF",
        org.apache.ambari.view.hive20.resources.udfs.UDF.class);
    hive1EntitiesMapping.put("org.apache.ambari.view.hive2.resources.resources.FileResourceItem",
        org.apache.ambari.view.hive20.resources.resources.FileResourceItem.class);
    hive1EntitiesMapping.put("org.apache.ambari.view.hive2.TestBean",
        org.apache.ambari.view.hive20.TestBean.class);
  }

  @Override
  public boolean beforeMigration() throws ViewDataMigrationException {
    return isHive15();
  }

  @Override
  public void afterMigration() throws ViewDataMigrationException {
  }

  @Override
  public void migrateEntity(Class originEntityClass, Class currentEntityClass) throws ViewDataMigrationException {
    if (isHive15()) {
      currentEntityClass = hive1EntitiesMapping.get(originEntityClass.getCanonicalName());
      if (currentEntityClass == null) {
        LOG.debug("Mapping was not found for class " + originEntityClass.getCanonicalName());
        return;
      }

      migrationContext.copyAllObjects(originEntityClass, currentEntityClass);

    } else {
      LOG.warn("Unknown migration policy for class");
    }
  }

  @Override
  public void migrateInstanceData() throws ViewDataMigrationException {
    migrationContext.copyAllInstanceData();
  }

  private boolean isHive15() {
    return migrationContext.getOriginDataVersion() == 1;
  }
}
