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
import java.util.Set;
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
  private OsFamily os_family;

  public LatestRepoCallable(String latestSourceUri, File stackRepoFolder, StackInfo stack, OsFamily os_family) {
    this.sourceUri = latestSourceUri;
    this.stackRepoFolder = stackRepoFolder;
    this.stack = stack;
    this.os_family = os_family;
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

        LOG.info("Loading latest URL info for stack " + stack.getName() + "-" +
                stack.getVersion() + " from " + sourceUri);
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
          LOG.info("Loading latest URL info for stack " + stack.getName() + "-" +
                  stack.getVersion() + " from " + jsonFile);
          latestUrlMap = gson.fromJson(new FileReader(jsonFile), type);
        }
      }
    } catch (Exception e) {
      LOG.info("Could not load the URI for stack " + stack.getName() + "-" +
              stack.getVersion() + " from " + sourceUri + " (" + e.getMessage() + ")" +
              ". Using default repository values.");
      throw e;
    }


    if (null != latestUrlMap) {
      for (RepositoryInfo ri : stack.getRepositories()) {
        if (latestUrlMap.containsKey(ri.getRepoId())) {
          Map<String, Object> valueMap = latestUrlMap.get(ri.getRepoId());
          if (valueMap.containsKey("latest")) {

            @SuppressWarnings("unchecked")
            Map<String, String> osMap = (Map<String, String>) valueMap.get("latest");

            String baseUrl = resolveOsUrl(ri.getOsType(), osMap);
            if (null != baseUrl) {
              // !!! in the case where <name>.repo is defined with the base url, strip that off.
              // Agents do the reverse action (take the base url, and append <name>.repo)

              String repo_file_format;
              
              if(os_family.isUbuntuFamily(ri.getOsType())) {
                repo_file_format = "list";
              } else {
                repo_file_format = "repo";
              }

              String repoFileName = stack.getName().toLowerCase() + "." + repo_file_format;
              int idx = baseUrl.toLowerCase().indexOf(repoFileName);

              if (-1 != idx && baseUrl.toLowerCase().endsWith(repoFileName)) {
                baseUrl = baseUrl.substring(0, idx);
              }

              if ('/' == baseUrl.charAt(baseUrl.length()-1)) {
                baseUrl = baseUrl.substring(0, baseUrl.length()-1);
              }

              ri.setLatestBaseUrl(baseUrl);
              if (ri.getBaseUrl() != null && !ri.isBaseUrlFromSaved()) {
                // Override baseUrl with the latestBaseUrl.
                ri.setBaseUrl(baseUrl);
              }
            }
          }
        }
      }
    }

    return null;
  }

  /**
   * Resolves a base url given that certain OS types can be used interchangeably.
   * @param os the target os to find
   * @param osMap the map of os-to-baseurl
   * @return the url for an os.
   */
  private String resolveOsUrl(String os, Map<String, String> osMap) {

    // !!! look for the OS directly
    if (osMap.containsKey(os))
      return osMap.get(os);

    // !!! os not found, find and return the first compatible one
    Set<String> possibleTypes = os_family.findTypes(os);

    for (String type : possibleTypes) {
      if (osMap.containsKey(type))
        return osMap.get(type);
    }

    return null;
  }

}
