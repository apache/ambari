/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.infra.job.archive;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import org.apache.hadoop.fs.permission.FsPermission;

public class HdfsProperties {
  private static final String DEFAULT_FILE_PERMISSION = "640";

  private final String hdfsEndpoint;
  private final String hdfsDestinationDirectory;
  private final FsPermission hdfsFilePermission;
  private final String hdfsKerberosPrincipal;
  private final String hdfsKerberosKeytabPath;

  public HdfsProperties(String hdfsEndpoint, String hdfsDestinationDirectory, FsPermission hdfsFilePermission, String hdfsKerberosPrincipal, String hdfsKerberosKeytabPath) {
    this.hdfsEndpoint = hdfsEndpoint;
    this.hdfsDestinationDirectory = hdfsDestinationDirectory;
    this.hdfsFilePermission = hdfsFilePermission == null ? new FsPermission(DEFAULT_FILE_PERMISSION) : hdfsFilePermission;
    this.hdfsKerberosPrincipal = hdfsKerberosPrincipal;
    this.hdfsKerberosKeytabPath = hdfsKerberosKeytabPath;
  }

  public String getHdfsEndpoint() {
    return hdfsEndpoint;
  }

  public String getHdfsDestinationDirectory() {
    return hdfsDestinationDirectory;
  }

  public FsPermission getHdfsFilePermission() {
    return hdfsFilePermission;
  }

  public String getHdfsKerberosPrincipal() {
    return hdfsKerberosPrincipal;
  }

  public String getHdfsKerberosKeytabPath() {
    return hdfsKerberosKeytabPath;
  }

  @Override
  public String toString() {
    return "HdfsProperties{" +
            "hdfsEndpoint='" + hdfsEndpoint + '\'' +
            ", hdfsDestinationDirectory='" + hdfsDestinationDirectory + '\'' +
            ", hdfsFilePermission=" + hdfsFilePermission +
            ", hdfsKerberosPrincipal='" + hdfsKerberosPrincipal + '\'' +
            ", hdfsKerberosKeytabPath='" + hdfsKerberosKeytabPath + '\'' +
            '}';
  }

  public void validate() {
    if (isBlank(hdfsDestinationDirectory))
      throw new IllegalArgumentException("The property hdfsDestinationDirectory can not be null or empty string!");

    if (isNotBlank(hdfsKerberosPrincipal) && isBlank(hdfsKerberosKeytabPath))
      throw new IllegalArgumentException("The property hdfsKerberosPrincipal is specified but hdfsKerberosKeytabPath is blank!");

    if (isBlank(hdfsKerberosPrincipal) && isNotBlank(hdfsKerberosKeytabPath))
      throw new IllegalArgumentException("The property hdfsKerberosKeytabPath is specified but hdfsKerberosPrincipal is blank!");
  }
}
