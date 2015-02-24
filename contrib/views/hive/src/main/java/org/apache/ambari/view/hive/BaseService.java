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

package org.apache.ambari.view.hive;

import com.google.inject.Inject;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive.persistence.Storage;
import org.apache.ambari.view.hive.persistence.utils.StorageUtil;
import org.apache.ambari.view.hive.utils.HdfsApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Parent service
 */
public class BaseService {
  @Inject
  protected ViewContext context;

  protected final static Logger LOG =
      LoggerFactory.getLogger(BaseService.class);

  private Storage storage;
  protected Storage getStorage() {
    if (storage == null) {
      storage = StorageUtil.getInstance(context).getStorage();
    }
    return storage;
  }

  private HdfsApi hdfsApi = null;
  protected HdfsApi getHdfsApi()  {
    if (hdfsApi == null)
      hdfsApi = HdfsApi.getInstance(context);
    return hdfsApi;
  }

  public BaseService() {
//    Thread.currentThread().setContextClassLoader(null);
  }
}
