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
package org.apache.ambari.logfeeder.util;

import org.apache.log4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;

public class AWSUtil {
  private static final Logger LOG = Logger.getLogger(AWSUtil.class);

  private AWSUtil() {
    throw new UnsupportedOperationException();
  }

  public static AWSCredentials createAWSCredentials(String accessKey, String secretKey) {
    if (accessKey != null && secretKey != null) {
      LOG.debug("Creating aws client as per new accesskey and secretkey");
      AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
      return awsCredentials;
    } else {
      return null;
    }
  }
}
