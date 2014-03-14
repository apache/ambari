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
package org.apache.ambari.server.state.stack;

import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.StackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Encapsulates the work to resolve the latest repo information for a stack.
 * This class must be used AFTER the stack has created its owned repositories.
 */
public class LatestRepoCallable implements Callable<Void> {
  private static final int LOOKUP_CONNECTION_TIMEOUT = 2000;
  private static final int LOOKUP_READ_TIMEOUT = 1000;
  
  private final static Logger LOG = LoggerFactory.getLogger(LatestRepoCallable.class);
  
  private String sourceUri = null;
  private File stackRepoFolder = null;
  private StackInfo stack = null;
  
  public LatestRepoCallable(String latestSourceUri, File stackRepoFolder, StackInfo stack) {
    this.sourceUri = latestSourceUri;
    this.stackRepoFolder = stackRepoFolder;
    this.stack = stack;
  }

  @Override
  public Void call() throws Exception {
    
    Type type = new TypeToken<Map<String, Map<String, Object>>>(){}.getType();
    Gson gson = new Gson();
    
    Map<String, Map<String, Object>> latestUrlMap = null;
    
    try {
      if (sourceUri.startsWith("http")) {
        
        URLStreamProvider streamProvider = new URLStreamProvider(
            LOOKUP_CONNECTION_TIMEOUT, LOOKUP_READ_TIMEOUT,
            null, null, null);
        
        LOG.info("Loading latest URL info from " + sourceUri);
        latestUrlMap = gson.fromJson(new InputStreamReader(
            streamProvider.readFrom(sourceUri)), type);
      } else {
        File jsonFile = null;
        if (sourceUri.charAt(0) == '.') {
          jsonFile = new File(stackRepoFolder, sourceUri);
        } else {
          jsonFile = new File(sourceUri);
        }
        
        if (jsonFile.exists()) {
          LOG.info("Loading latest URL info from " + jsonFile);
          latestUrlMap = gson.fromJson(new FileReader(jsonFile), type);
        }
      }
    } catch (Exception e) {
      LOG.error("Could not load the URI " + sourceUri + " (" + e.getMessage() + ")");
      throw e;
    }
    
    
    if (null != latestUrlMap) {
      for (RepositoryInfo ri : stack.getRepositories()) {
        if (latestUrlMap.containsKey(ri.getRepoId())) {
          Map<String, Object> valueMap = latestUrlMap.get(ri.getRepoId());
          if (valueMap.containsKey("latest")) {
            
            @SuppressWarnings("unchecked")
            Map<String, String> osMap = (Map<String, String>) valueMap.get("latest");
            if (osMap.containsKey(ri.getOsType())) {
              ri.setLatestBaseUrl(osMap.get(ri.getOsType()));
            }
          }
        }
      }
    }
    
    return null;
  }

}
