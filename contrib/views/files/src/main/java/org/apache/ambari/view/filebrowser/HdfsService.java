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

package org.apache.ambari.view.filebrowser;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.filebrowser.utils.MisconfigurationFormattedException;
import org.apache.ambari.view.filebrowser.utils.ServiceFormattedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base Hdfs service
 */
public abstract class HdfsService {

  protected static final Logger logger = LoggerFactory.getLogger(HdfsService.class);

  protected final ViewContext context;

  /**
   * Constructor
   * @param context View Context instance
   */
  public HdfsService(ViewContext context) {
    this.context = context;
  }

  /**
   * Wrapper for json mapping of bool response
   */
  @XmlRootElement
  public static class BoolResult{
    public boolean success;
    public BoolResult(boolean success){
      this.success = success;
    }
  }

  private HdfsApi _api = null;

  /**
   * Ger HdfsApi instance
   * @param context View Context instance
   * @return HdfsApi business delegate
   */
  public HdfsApi getApi(ViewContext context) {
    if (_api == null) {
      Thread.currentThread().setContextClassLoader(null);
      String defaultFs = context.getProperties().get("dataworker.defaultFs");
      if (defaultFs == null)
        throw new MisconfigurationFormattedException("dataworker.defaultFs");
      try {
        _api = new HdfsApi(defaultFs, getUsername(context));
      } catch (Exception ex) {
        throw new ServiceFormattedException("HdfsApi connection failed. Check \"dataworker.defaultFs\" property", ex);
      }
    }
    return _api;
  }

  /**
   * Get username to use in HDFS
   * @param context View Context instance
   * @return user name
   */
  public String getUsername(ViewContext context) {
    String username = context.getProperties().get("dataworker.username");
    if (username == null || username.isEmpty())
      username = context.getUsername();
    return username;
  }
}
