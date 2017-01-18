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

import com.google.common.base.Optional;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.commons.hdfs.ViewPropertyHelper;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.ambari.view.utils.hdfs.HdfsApiException;
import org.apache.ambari.view.utils.hdfs.HdfsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HdfsApiSupplier implements ContextSupplier<Optional<HdfsApi>> {
  public static final String VIEW_CONF_KEYVALUES = "view.conf.keyvalues";

  protected final Logger LOG =
    LoggerFactory.getLogger(getClass());

  private static final Map<String, HdfsApi> hdfsApiMap = new ConcurrentHashMap<>();
  private final Object lock = new Object();

  @Override
  public Optional<HdfsApi> get(ViewContext context) {
    try {
      if(!hdfsApiMap.containsKey(getKey(context))) {
        synchronized (lock) {
          if(!hdfsApiMap.containsKey(getKey(context))) {
            LOG.debug("Creating HDFSApi instance for Viewname: {}, Instance Name: {}", context.getViewName(), context.getInstanceName());
            Optional<Map<String, String>> props = ViewPropertyHelper.getViewConfigs(context, VIEW_CONF_KEYVALUES);
            HdfsApi api;
            if(props.isPresent()){
              api = HdfsUtil.connectToHDFSApi(context, props.get());
            }else{
              api = HdfsUtil.connectToHDFSApi(context);
            }
            hdfsApiMap.put(getKey(context), api);
            return Optional.of(api);
          }
        }
      }
      return Optional.of(hdfsApiMap.get(getKey(context)));
    } catch (HdfsApiException e) {
      LOG.error("Cannot get the HDFS API", e);
      return Optional.absent();
    }
  }

  private String getKey(ViewContext context) {
    return context.getUsername() + ":" + context.getInstanceName();
  }
}
