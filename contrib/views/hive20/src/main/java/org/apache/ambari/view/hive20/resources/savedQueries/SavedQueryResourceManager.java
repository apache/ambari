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

package org.apache.ambari.view.hive20.resources.savedQueries;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive20.persistence.utils.FilteringStrategy;
import org.apache.ambari.view.hive20.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive20.resources.PersonalCRUDResourceManager;
import org.apache.ambari.view.hive20.utils.*;
import org.apache.ambari.view.utils.hdfs.HdfsApiException;
import org.apache.ambari.view.utils.hdfs.HdfsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Object that provides CRUD operations for query objects
 */
public class SavedQueryResourceManager extends PersonalCRUDResourceManager<SavedQuery> {
  private final static Logger LOG =
      LoggerFactory.getLogger(SavedQueryResourceManager.class);

  private SharedObjectsFactory sharedObjectsFactory;

  /**
   * Constructor
   * @param context View Context instance
   */
  public SavedQueryResourceManager(ViewContext context, SharedObjectsFactory sharedObjectsFactory) {
    super(SavedQuery.class, sharedObjectsFactory, context);
    this.sharedObjectsFactory = sharedObjectsFactory;
  }

  @Override
  public SavedQuery create(SavedQuery object) {
    String query = object.getShortQuery();
    object.setShortQuery(makeShortQuery(query));
    object = super.create(object);
    try {
      createDefaultQueryFile(object, query);

    } catch (ServiceFormattedException e) {
      cleanupAfterErrorAndThrowAgain(object, e);
    }
    return object;
  }

  private void createDefaultQueryFile(SavedQuery object, String query) {
    String userScriptsPath = context.getProperties().get("scripts.dir");
    if (userScriptsPath == null) {
      String msg = "scripts.dir is not configured!";
      LOG.error(msg);
      throw new MisconfigurationFormattedException("scripts.dir");
    }

    String normalizedName = String.format("hive-query-%s", object.getId());
    String timestamp = new SimpleDateFormat("yyyy-MM-dd_hh-mm").format(new Date());
    String baseFileName = String.format(userScriptsPath +
        "/%s-%s", normalizedName, timestamp);

    String newFilePath = null;
    try {
      newFilePath = HdfsUtil.findUnallocatedFileName(sharedObjectsFactory.getHdfsApi(), baseFileName, ".hql");
      HdfsUtil.putStringToFile(sharedObjectsFactory.getHdfsApi(), newFilePath, query);
    } catch (HdfsApiException e) {
      throw new ServiceFormattedException(e);
    }

    object.setQueryFile(newFilePath);
    storageFactory.getStorage().store(SavedQuery.class, object);
  }

  @Override
  public SavedQuery read(Object id) throws ItemNotFound {
    SavedQuery savedQuery = super.read(id);
    return savedQuery;
  }

  private void emptyShortQueryField(SavedQuery query) {
    query.setShortQuery("");
    storageFactory.getStorage().store(SavedQuery.class, query);
  }

  /**
   * Generate short preview of query.
   * Remove SET settings like "set hive.execution.engine=tez;" from beginning
   * and trim to 42 symbols.
   * @param query full query
   * @return shortened query
   */
  protected static String makeShortQuery(String query) {
    query = query.replaceAll("(?i)set\\s+[\\w\\-.]+(\\s*)=(\\s*)[\\w\\-.]+(\\s*);", "");
    query = query.trim();
    return query.substring(0, (query.length() > 42) ? 42 : query.length());
  }

  @Override
  public SavedQuery update(SavedQuery object, String id) throws ItemNotFound {
    String query = object.getShortQuery();
    object.setShortQuery(makeShortQuery(query));
    object = super.update(object, id);
    try {
      createDefaultQueryFile(object, query);

    } catch (ServiceFormattedException e) {
      cleanupAfterErrorAndThrowAgain(object, e);
    }
    return object;
  }

  @Override
  public List<SavedQuery> readAll(FilteringStrategy filteringStrategy) {
    List<SavedQuery> queries = super.readAll(filteringStrategy);
    return queries;
  }

  @Override
  public void delete(Object resourceId) throws ItemNotFound {
    super.delete(resourceId);
  }
}
