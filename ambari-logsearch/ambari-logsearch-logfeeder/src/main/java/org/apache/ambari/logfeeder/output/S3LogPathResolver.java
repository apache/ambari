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

package org.apache.ambari.logfeeder.output;

import org.apache.ambari.logfeeder.common.LogFeederConstants;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.ambari.logfeeder.util.PlaceholderUtil;

import java.util.HashMap;

/**
 * A utility class that resolves variables like hostname, IP address and cluster name in S3 paths.
 */
public class S3LogPathResolver {

  /**
   * Construct a full S3 path by resolving variables in the path name including hostname, IP address
   * and cluster name
   * @param baseKeyPrefix The prefix which can contain the variables.
   * @param keySuffix The suffix appended to the prefix after variable expansion
   * @param cluster The name of the cluster
   * @return full S3 path.
   */
  public String getResolvedPath(String baseKeyPrefix, String keySuffix, String cluster) {
    HashMap<String, String> contextParam = buildContextParam(cluster);
    String resolvedKeyPrefix = PlaceholderUtil.replaceVariables(baseKeyPrefix, contextParam);
    return resolvedKeyPrefix + LogFeederConstants.S3_PATH_SEPARATOR + keySuffix;
  }

  private HashMap<String, String> buildContextParam(String cluster) {
    HashMap<String, String> contextParam = new HashMap<>();
    contextParam.put("host", LogFeederUtil.hostName);
    contextParam.put("ip", LogFeederUtil.ipAddress);
    contextParam.put("cluster", cluster);
    return contextParam;
  }

}
