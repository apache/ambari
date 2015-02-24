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

package org.apache.ambari.view.hive.resources.savedQueries;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive.persistence.utils.FilteringStrategy;
import org.apache.ambari.view.hive.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive.resources.PersonalCRUDResourceManager;
import org.apache.ambari.view.hive.utils.*;
import org.apache.ambari.view.hive.utils.HdfsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Object that provides CRUD operations for query objects
 */
public class SavedQueryResourceManager extends PersonalCRUDResourceManager<SavedQuery> {
  private final static Logger LOG =
      LoggerFactory.getLogger(SavedQueryResourceManager.class);

  /**
   * Constructor
   * @param context View Context instance
   */
  private SavedQueryResourceManager(ViewContext context) {
    super(SavedQuery.class, context);
  }

  //TODO: move all context-singletones to ContextController or smth like that
  private static Map<String, SavedQueryResourceManager> viewSingletonObjects = new HashMap<String, SavedQueryResourceManager>();
  public static SavedQueryResourceManager getInstance(ViewContext context) {
    if (!viewSingletonObjects.containsKey(context.getInstanceName()))
      viewSingletonObjects.put(context.getInstanceName(), new SavedQueryResourceManager(context));
    return viewSingletonObjects.get(context.getInstanceName());
  }
  static Map<String, SavedQueryResourceManager> getViewSingletonObjects() {
    return viewSingletonObjects;
  }

  @Override
  public SavedQuery create(SavedQuery object) {
    object = super.create(object);
    try {

      if (object.getQueryFile() == null || object.getQueryFile().isEmpty()) {
        createDefaultQueryFile(object);
      }

    } catch (ServiceFormattedException e) {
      cleanupAfterErrorAndThrowAgain(object, e);
    }
    return object;
  }

  private void createDefaultQueryFile(SavedQuery object) {
    String userScriptsPath = context.getProperties().get("scripts.dir");
    if (userScriptsPath == null) {
      String msg = "scripts.dir is not configured!";
      LOG.error(msg);
      throw new MisconfigurationFormattedException("scripts.dir");
    }

    String normalizedName = String.format("hive-query-%d", object.getId());
    String timestamp = new SimpleDateFormat("yyyy-MM-dd_hh-mm").format(new Date());
    String baseFileName = String.format(userScriptsPath +
        "/%s-%s", normalizedName, timestamp);

    String newFilePath = HdfsUtil.findUnallocatedFileName(context, baseFileName, ".hql");
    HdfsUtil.putStringToFile(context, newFilePath, "");

    object.setQueryFile(newFilePath);
    getStorage().store(SavedQuery.class, object);
  }

  @Override
  public SavedQuery read(Integer id) throws ItemNotFound {
    SavedQuery savedQuery = super.read(id);
    fillShortQueryField(savedQuery);
    return savedQuery;
  }

  private void fillShortQueryField(SavedQuery savedQuery) {
    if (savedQuery.getQueryFile() != null) {
      FilePaginator paginator = new FilePaginator(savedQuery.getQueryFile(), context);
      String query = null;
      try {
        query = paginator.readPage(0);
      } catch (IOException e) {
        LOG.error("Can't read query file " + savedQuery.getQueryFile());
        return;
      } catch (InterruptedException e) {
        LOG.error("Can't read query file " + savedQuery.getQueryFile());
        return;
      }
      savedQuery.setShortQuery(query.substring(0, (query.length() > 42)?42:query.length()));
    }
    getStorage().store(SavedQuery.class, savedQuery);
  }

  @Override
  public List<SavedQuery> readAll(FilteringStrategy filteringStrategy) {
    return super.readAll(filteringStrategy);
  }

  @Override
  public void delete(Integer resourceId) throws ItemNotFound {
    super.delete(resourceId);
  }
}
