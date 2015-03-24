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
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.io.IOUtils;

/**
 * Class that encapsulates OS family logic
 */
@Singleton
public class OsFamily {
    public final static String UBUNTU_FAMILY = "ubuntu";
    
    private final String os_pattern = "([^\\d]*)([\\d]*)";
    private final String OS_DISTRO = "distro";
    private final String OS_VERSION = "versions";
    private final String LOAD_CONFIG_MSG = "Could not load OS family definition from %s file";
    private final String FILE_NAME = "os_family.json";
    private final Logger LOG = LoggerFactory.getLogger(OsFamily.class);

    private Map<String, Map<String, Set<String>>> osMap = null;

  /**
   * Initialize object
   * @param conf Configuration instance
   */
    public OsFamily(Configuration conf){
      init(conf.getSharedResourcesDirPath());
    }

  /**
   * Initialize object
   * @param properties list of properties
   */
    public OsFamily(Properties properties){
      init(properties.getProperty(Configuration.SHARED_RESOURCES_DIR_KEY));
    }

    private void init(String SharedResourcesPath){
      FileInputStream inputStream = null;
      try {
        File f = new File(SharedResourcesPath, FILE_NAME);
        if (!f.exists()) throw new Exception();
        inputStream = new FileInputStream(f);

        Type type = new TypeToken<Map<String, Map<String, Set<String>>>>() {}.getType();
        Gson gson = new Gson();
        osMap = gson.fromJson(new InputStreamReader(inputStream), type);
      } catch (Exception e) {
        LOG.error(String.format(LOAD_CONFIG_MSG, new File(SharedResourcesPath, FILE_NAME).toString()));
        throw new RuntimeException(e);
      } finally {
        IOUtils.closeQuietly(inputStream);
      }
    }

    /**
     * Separate os name from os major version
     * @param os the os
     * @return separated os name and os version
     */
    private Map<String,String> parse_os(String os){
      Map<String,String> pos = new HashMap<String,String>();

      Pattern r = Pattern.compile(os_pattern);
      Matcher m = r.matcher(os);

      if (m.find()){
        pos.put(OS_DISTRO, m.group(1));
        pos.put(OS_VERSION, m.group(2));
      } else {
        pos.put(OS_DISTRO, os);
        pos.put(OS_VERSION, "");
      }
      return pos;
    }

    /**
     * Gets the array of compatible OS types
     * @param os the os
     * @return all types that are compatible with the supplied type
     */
    public Set<String> findTypes(String os) {
      Map<String,String>  pos = parse_os(os);
      for ( String family : osMap.keySet()) {
        Map<String, Set<String>> fam = osMap.get(family);
        if (fam.get(OS_DISTRO).contains(pos.get(OS_DISTRO)) && fam.get(OS_VERSION).contains(pos.get(OS_VERSION))){
          Set<String> data=new HashSet<String>();
          for (String item: fam.get(OS_DISTRO)) data.add(item + pos.get(OS_VERSION));
            return Collections.unmodifiableSet(data);
        }
      }
      return Collections.emptySet();
    }

    /**
     * Finds the family for the specific OS + it's version number
     * @param os the OS
     * @return the family, or <code>null</code> if not defined
     */
    public String find(String os) {
      Map<String,String>  pos = parse_os(os);
      for ( String family : osMap.keySet()) {
        Map<String, Set<String>> fam = osMap.get(family);
        if (fam.get(OS_DISTRO).contains(pos.get(OS_DISTRO)) && fam.get(OS_VERSION).contains(pos.get(OS_VERSION))){
          return family + pos.get(OS_VERSION);
        }
      }
      return null;
    }
    
    /**
     * Finds the family for the specific OS
     * @param os the OS
     * @return the family, or <code>null</code> if not defined
     */
    public String find_family(String os) {
      Map<String,String>  pos = parse_os(os);
      for ( String family : osMap.keySet()) {
        Map<String, Set<String>> fam = osMap.get(family);
        if (fam.get(OS_DISTRO).contains(pos.get(OS_DISTRO)) && fam.get(OS_VERSION).contains(pos.get(OS_VERSION))){
          return family;
        }
      }
      return null;
    }

    /**
     * Form list of all supported os types
     * @return one dimension list with os types
     */
    public Set<String> os_list(){
      Set<String> r= new HashSet<String>();
      for ( String family : osMap.keySet()) {
        Map<String, Set<String>> fam = osMap.get(family);
        for (String version: fam.get(OS_VERSION)){
          Set<String> data=new HashSet<String>();
          for (String item: fam.get(OS_DISTRO)) data.add(item + version);
          r.addAll(data);
        }
      }
      return r;
    }
}