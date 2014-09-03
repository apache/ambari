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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Class that encapsulates OS family logic
 */
public class OsFamily {

  private static final String FILE_NAME = "os_family.json";
  private static final Logger LOG = LoggerFactory.getLogger(OsFamily.class);
  
  private static Map<String, Set<String>> osMap = new HashMap<String, Set<String>>();
  
  static {
    try {
      InputStream inputStream = OsFamily.class.getClassLoader().getResourceAsStream(FILE_NAME);
      if (null == inputStream)
        inputStream = ClassLoader.getSystemResourceAsStream(FILE_NAME);
      
      Type type = new TypeToken<Map<String, Set<String>>>(){}.getType();
      Gson gson = new Gson();
      osMap = gson.fromJson(new InputStreamReader(inputStream), type);
      
    } catch (Exception e) {
      LOG.error("Could not load OS family definition, using defaults");
      osMap.put("centos5", new HashSet<String>());
      osMap.put("centos6", new HashSet<String>());
      osMap.put("suse11", new HashSet<String>());
      osMap.put("ubuntu12", new HashSet<String>());
      
      Collections.addAll(osMap.get("centos5"), "centos5", "redhat5", "oraclelinux5", "rhel5");
      Collections.addAll(osMap.get("centos6"), "centos6", "redhat6", "oraclelinux6", "rhel6");
      Collections.addAll(osMap.get("suse11"), "suse11", "sles11", "opensuse11");
      Collections.addAll(osMap.get("ubuntu12"), "ubuntu12");
    }
  }


  /**
   * Gets the array of compatible OS types
   * @param os the os
   * @return all types that are compatible with the supplied type
   */
  static Set<String> findTypes(String os) {
    for (Entry<String, Set<String>> entry : osMap.entrySet()) {
      for (String type : entry.getValue()) {
        if (type.equals(os))
          return Collections.unmodifiableSet(entry.getValue());
      }
    }
    return Collections.emptySet();
  }

  /**
   * Finds the family for the specific OS
   * @param os the OS
   * @return the family, or <code>null</code> if not defined
   */
  public static String find(String os) {
    for (Entry<String, Set<String>> entry : osMap.entrySet()) {
      for (String type : entry.getValue()) {
        if (type.equals(os))
          return entry.getKey();
      }
    }
    
    return null;
  }

}
