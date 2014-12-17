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
 * Encapsulates IO operations on a common services directory.
 */
public class CommonServiceDirectory extends ServiceDirectory {
  /**
   * logger instance
   */
  private static final Logger LOG = LoggerFactory.getLogger(CommonServiceDirectory.class);

  /**
   * Constructor.
   *
   * @param servicePath     path of the service directory
   * @throws org.apache.ambari.server.AmbariException if unable to parse the service directory
   */
  public CommonServiceDirectory(String servicePath) throws AmbariException {
    super(servicePath);
  }

  @Override
  /**
   * Parse common service directory
   * packageDir Format: common-services/<serviceName>/<serviceVersion>/package
   * Example:
   *  directory: "/var/lib/ambari-server/resources/common-services/HDFS/1.0"
   *  packageDir: "common-services/HDFS/1.0/package"
   *
   * @throws AmbariException
   */
  protected void parsePath() throws AmbariException {
    File serviceVersionDir = new File(getAbsolutePath());
    File serviceDir = serviceVersionDir.getParentFile();

    String serviceId = String.format("%s/%s", serviceDir.getName(), serviceVersionDir.getName());

    File absPackageDir = new File(getAbsolutePath() + File.separator + PACKAGE_FOLDER_NAME);
    if(absPackageDir.isDirectory()) {
      packageDir = absPackageDir.getPath().substring(serviceDir.getParentFile().getParentFile().getPath().length() + 1);
      LOG.debug(String.format("Service package folder for common service %s has been resolved to %s",
          serviceId, packageDir));
    } else {
      LOG.debug(String.format("Service package folder %s for common service %s does not exist.",
          absPackageDir, serviceId ));
    }
    parseMetaInfoFile();
  }
}
