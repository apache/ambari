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

package org.apache.ambari.view.hive2.internal;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive2.persistence.DataStoreStorage;
import org.apache.ambari.view.hive2.persistence.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A supplier for data storage
 * Duplicated to keep the API uniform
 */
public class DataStorageSupplier implements ContextSupplier<Storage> {

  protected final Logger LOG =
    LoggerFactory.getLogger(getClass());

  @Override
  public Storage get(ViewContext context) {
    LOG.debug("Creating storage instance for Viewname: {}, Instance Name: {}", context.getViewName(), context.getInstanceName());
    return new DataStoreStorage(context);
  }
}
