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
package org.apache.ambari.server.resources;

import java.io.File;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Resource manager.
 */
@Singleton
public class ResourceManager {
  private static Log LOG = LogFactory.getLog(ResourceManager.class);
	
  @Inject Configuration configs;
  /**
  * Returns resource file.
  * @param resourcePath relational path to file
  * @return resource file
  */
  public File getResource(String resourcePath) {
    String resDir = configs.getConfigsMap().get(Configuration.RESOURCES_DIR_KEY);
    String resourcePathIndep = resourcePath.replace("/", File.separator);
    File resourceFile = new File(resDir + File.separator + resourcePathIndep);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Resource requested from ResourceManager"
          + ", resourceDir=" + resDir
          + ", resourcePath=" + resourcePathIndep
          + ", fileExists=" + resourceFile.exists());
    }
    return resourceFile;
  }
}
