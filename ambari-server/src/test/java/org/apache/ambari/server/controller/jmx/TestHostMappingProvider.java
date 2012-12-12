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

package org.apache.ambari.server.controller.jmx;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class TestHostMappingProvider {

  private static Map<String, String> HOST_MAPPING = new HashMap<String, String>();

  static {
    HOST_MAPPING.put("domu-12-31-39-0e-34-e1.compute-1.internal", "ec2-50-17-129-192.compute-1.amazonaws.com");
    HOST_MAPPING.put("ip-10-190-186-15.ec2.internal",             "ec2-23-21-8-226.compute-1.amazonaws.com");
    HOST_MAPPING.put("domu-12-31-39-14-ee-b3.compute-1.internal", "ec2-23-23-71-42.compute-1.amazonaws.com");
    HOST_MAPPING.put("ip-10-110-157-51.ec2.internal",             "ec2-107-22-121-67.compute-1.amazonaws.com");
  }

  public static Map<String, String> getHostMap() {
    return HOST_MAPPING;
  }
}
