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

package org.apache.ambari.view.hive20.resources.udfs;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive20.persistence.IStorageFactory;
import org.apache.ambari.view.hive20.persistence.utils.FilteringStrategy;
import org.apache.ambari.view.hive20.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive20.resources.PersonalCRUDResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Object that provides CRUD operations for udf objects
 */
public class UDFResourceManager extends PersonalCRUDResourceManager<UDF> {
  private final static Logger LOG =
      LoggerFactory.getLogger(UDFResourceManager.class);

  /**
   * Constructor
   * @param context View Context instance
   */
  public UDFResourceManager(IStorageFactory storageFactory, ViewContext context) {
    super(UDF.class, storageFactory, context);
  }

  @Override
  public UDF read(Object id) throws ItemNotFound {
    return super.read(id);
  }

  @Override
  public List<UDF> readAll(FilteringStrategy filteringStrategy) {
    return super.readAll(filteringStrategy);
  }

  @Override
  public UDF create(UDF object) {
    return super.create(object);
  }

  @Override
  public void delete(Object resourceId) throws ItemNotFound {
    super.delete(resourceId);
  }
}
