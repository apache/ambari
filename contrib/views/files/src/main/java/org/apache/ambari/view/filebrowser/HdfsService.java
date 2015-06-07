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
import org.apache.ambari.view.filebrowser.utils.ServiceFormattedException;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.ambari.view.utils.hdfs.HdfsUtil;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
    public String message;
    public BoolResult(boolean success){
      this.success = success;
    }

    public BoolResult(boolean success, String message){
      this.success = success;
      this.message = message;
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
      try {
        _api = HdfsUtil.connectToHDFSApi(context);
      } catch (Exception ex) {
        throw new ServiceFormattedException("HdfsApi connection failed. Check \"webhdfs.url\" property", ex);
      }
    }
    return _api;
  }

  private static Map<String, String> getHdfsAuthParams(ViewContext context) {
    String auth = context.getProperties().get("webhdfs.auth");
    Map<String, String> params = new HashMap<String, String>();
    if (auth == null || auth.isEmpty()) {
      auth = "auth=SIMPLE";
    }
    for(String param : auth.split(";")) {
      String[] keyvalue = param.split("=");
      if (keyvalue.length != 2) {
        logger.error("Can not parse authentication param " + param + " in " + auth);
        continue;
      }
      params.put(keyvalue[0], keyvalue[1]);
    }
    return params;
  }

  /**
   * Get doAs username to use in HDFS
   * @param context View Context instance
   * @return user name
   */
  public String getDoAsUsername(ViewContext context) {
    String username = context.getProperties().get("webhdfs.username");
    if (username == null || username.isEmpty())
      username = context.getUsername();
    return username;
  }

  /**
   * Get proxyuser username to use in HDFS
   * @param context View Context instance
   * @return user name
   */
  public String getRealUsername(ViewContext context) {
    String username = context.getProperties().get("webhdfs.proxyuser");
    if (username == null || username.isEmpty())
      try {
        username = UserGroupInformation.getCurrentUser().getShortUserName();
      } catch (IOException e) {
        throw new ServiceFormattedException("HdfsApi connection failed. Can't get current user", e);
      }
    return username;
  }
}
