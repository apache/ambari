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

package org.apache.ambari.view.pig.services;

import com.google.inject.Inject;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.ViewResourceHandler;
import org.apache.ambari.view.pig.persistence.Storage;
import org.apache.ambari.view.pig.utils.HdfsApi;
import org.apache.ambari.view.pig.persistence.utils.StorageUtil;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.ws.WebServiceException;
import java.io.IOException;
import java.util.HashMap;


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
    if (this.storage == null) {
      storage = StorageUtil.getStorage(context);
    }
    return storage;
  }

  protected void setStorage(Storage storage) {
    this.storage = storage;
  }

  private static HdfsApi hdfsApi = null;

  /**
   * Returns HdfsApi object
   * @param context View Context instance
   * @return Hdfs business delegate object
   */
  public static HdfsApi getHdfsApi(ViewContext context) {
    if (hdfsApi == null) {
      Thread.currentThread().setContextClassLoader(null);

      String defaultFS = context.getProperties().get("dataworker.defaultFs");
      if (defaultFS == null) {
        String message = "dataworker.defaultFs is not configured!";
        LOG.error(message);
        throw new WebServiceException(message);
      }

      try {
        hdfsApi = new HdfsApi(defaultFS, getHdfsUsername(context));
        LOG.info("HdfsApi connected OK");
      } catch (IOException e) {
        String message = "HdfsApi IO error: " + e.getMessage();
        LOG.error(message);
        throw new WebServiceException(message, e);
      } catch (InterruptedException e) {
        String message = "HdfsApi Interrupted error: " + e.getMessage();
        LOG.error(message);
        throw new WebServiceException(message, e);
      }
    }
    return hdfsApi;
  }

  public static String getHdfsUsername(ViewContext context) {
    String userName = context.getProperties().get("dataworker.hdfs.username");
    if (userName == null)
      userName = context.getUsername();
    return userName;
  }

  protected HdfsApi getHdfsApi()  {
    return getHdfsApi(context);
  }

  /**
   * Set HdfsApi delegate
   * @param api HdfsApi instance
   */
  public static void setHdfsApi(HdfsApi api)  {
    hdfsApi = api;
  }

  protected static Response badRequestResponse(String message) {
    HashMap<String, Object> response = new HashMap<String, Object>();
    response.put("message", message);
    response.put("status", 400);
    return Response.status(400).entity(new JSONObject(response)).type(MediaType.APPLICATION_JSON).build();
  }

  protected static Response serverErrorResponse(String message) {
    HashMap<String, Object> response = new HashMap<String, Object>();
    response.put("message", message);
    response.put("status", 500);
    return Response.status(500).entity(new JSONObject(response)).type(MediaType.APPLICATION_JSON).build();
  }

  protected static Response notFoundResponse(String message) {
    HashMap<String, Object> response = new HashMap<String, Object>();
    response.put("message", message);
    response.put("status", 404);
    return Response.status(404).entity(new JSONObject(response)).type(MediaType.APPLICATION_JSON).build();
  }
}
