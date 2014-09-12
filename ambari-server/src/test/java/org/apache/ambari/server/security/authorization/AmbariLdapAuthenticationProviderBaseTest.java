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

package org.apache.ambari.server.security.authorization;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMockSupport;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AmbariLdapAuthenticationProviderBaseTest extends EasyMockSupport {

  private static final Log logger = LogFactory.getLog(AmbariLdapAuthenticationProviderBaseTest.class);

  protected static void createCleanApacheDSContainerWorkDir() throws IOException{
    // Set ApacheDsContainer's work dir under the current folder (Jenkins' job workspace) instead of the default /tmp/apacheds-spring-security. See AMBARI-7180
    SimpleDateFormat sdf = new SimpleDateFormat("HHmmss");
    String timestamp = sdf.format(new Date());
    final String workParent = new File(".").getAbsolutePath() + File.separator + "target" + File.separator + timestamp;
    new File(workParent).mkdirs();
    // The folder structure looks like {job-root}/target/{timestamp}/apacheds-spring-security
    final String apacheDSWorkDir = workParent + File.separator + "apacheds-spring-security";
    FileUtils.deleteDirectory(new File(apacheDSWorkDir));
    System.setProperty("apacheDSWorkDir", apacheDSWorkDir );
    logger.info("System property apacheDSWorkDir was set to " + apacheDSWorkDir);

  }

}
