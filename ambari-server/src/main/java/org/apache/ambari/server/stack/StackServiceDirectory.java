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

package org.apache.ambari.server.stack;

import org.apache.ambari.server.AmbariException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;


/**
 * Encapsulates IO operations on a stack service directory.
 */
public class StackServiceDirectory extends ServiceDirectory {

  /**
   * logger instance
   */
  private static final Logger LOG = LoggerFactory.getLogger(StackServiceDirectory.class);

  /**
   * Constructor.
   *
   * @param servicePath     path of the service directory
   * @throws org.apache.ambari.server.AmbariException if unable to parse the service directory
   */
  public StackServiceDirectory(String servicePath) throws AmbariException {
    super(servicePath);
  }

  @Override
  /**
   * Parse stack service directory.
   * packageDir Format: stacks/<stackName>/<stackVersion>/services/<serviceName>/package
   * Example:
   *  directory: "/var/lib/ambari-server/resources/stacks/HDP/2.0.6/services/HDFS"
   *  packageDir: "stacks/HDP/2.0.6/services/HDFS/package"
   * @throws AmbariException if unable to parse the service directory
   */
  protected void parsePath() throws AmbariException {
    File serviceDir = new File(getAbsolutePath());
    File stackVersionDir = serviceDir.getParentFile().getParentFile();
    File stackDir = stackVersionDir.getParentFile();

    String stackId = String.format("%s-%s", stackDir.getName(), stackVersionDir.getName());

    File absPackageDir = new File(getAbsolutePath() + File.separator + PACKAGE_FOLDER_NAME);
    if (absPackageDir.isDirectory()) {
      packageDir = absPackageDir.getPath().substring(stackDir.getParentFile().getParentFile().getPath().length() + 1);
      LOG.debug(String.format("Service package folder for service %s for stack %s has been resolved to %s",
          serviceDir.getName(), stackId, packageDir));
    } else {
      LOG.debug(String.format("Service package folder %s for service %s for stack %s does not exist.",
          absPackageDir, serviceDir.getName(), stackId));
    }
    parseMetaInfoFile();
  }
}
