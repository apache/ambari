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

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

public class HdfsUploader extends AbstractFileAction {
  private static final Logger LOG = LoggerFactory.getLogger(HdfsUploader.class);

  private final Configuration configuration;
  private final HdfsProperties properties;

  public HdfsUploader(Configuration configuration, HdfsProperties properties) {
    this.properties = properties;
    this.configuration = configuration;

    if (new ClassPathResource("core-site.xml").exists()) {
      LOG.info("Hdfs core-site.xml is found in the classpath.");
    }
    else {
      LOG.warn("Hdfs core-site.xml is not found in the classpath. Using defaults.");
    }
    if (new ClassPathResource("hdfs-site.xml").exists()) {
      LOG.info("Hdfs hdfs-site.xml is found in the classpath.");
    }
    else {
      LOG.warn("Hdfs hdfs-site.xml is not found in the classpath. Using defaults.");
    }
    if (isNotBlank(properties.getHdfsEndpoint())) {
      LOG.info("Hdfs endpoint is defined in Infra Manager properties. Setting fs.defaultFS to {}", properties.getHdfsEndpoint());
      this.configuration.set("fs.defaultFS", properties.getHdfsEndpoint());
    }

    UserGroupInformation.setConfiguration(configuration);
  }

  @Override
  protected File onPerform(File inputFile) {
    try {
      if ("kerberos".equalsIgnoreCase(configuration.get("hadoop.security.authentication")))
        UserGroupInformation.loginUserFromKeytab(properties.getHdfsKerberosPrincipal(), properties.getHdfsKerberosKeytabPath());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    try (FileSystem fileSystem = FileSystem.get(configuration)) {

      Path destination = new Path(properties.getHdfsDestinationDirectory(), inputFile.getName());
      if (fileSystem.exists(destination)) {
        throw new UnsupportedOperationException(String.format("File '%s' already exists!", destination));
      }

      fileSystem.copyFromLocalFile(new Path(inputFile.getAbsolutePath()), destination);
      fileSystem.setPermission(destination, properties.getHdfsFilePermission());

      return inputFile;
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
