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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;

public enum AWSUtil {
  INSTANCE;
  private static final Logger LOG = Logger.getLogger(AWSUtil.class);

  public String getAwsUserName(String accessKey, String secretKey) {
    String username = null;
    AWSCredentials awsCredentials = createAWSCredentials(accessKey, secretKey);
    AmazonIdentityManagementClient amazonIdentityManagementClient;
    if (awsCredentials != null) {
      amazonIdentityManagementClient = new AmazonIdentityManagementClient(
          awsCredentials);
    } else {
      // create default client
      amazonIdentityManagementClient = new AmazonIdentityManagementClient();
    }
    try {
      username = amazonIdentityManagementClient.getUser().getUser()
          .getUserName();
    } catch (AmazonServiceException e) {
      if (e.getErrorCode().compareTo("AccessDenied") == 0) {
        String arn = null;
        String msg = e.getMessage();
        int arnIdx = msg.indexOf("arn:aws");
        if (arnIdx != -1) {
          int arnSpace = msg.indexOf(" ", arnIdx);
          // should be similar to "arn:aws:iam::111111111111:user/username"
          arn = msg.substring(arnIdx, arnSpace);
        }
        if (arn != null) {
          String[] arnParts = arn.split(":");
          if (arnParts != null && arnParts.length > 5) {
            username = arnParts[5];
            if (username != null) {
              username = username.replace("user/", "");
            }
          }
        }
      }
    } catch (Exception exception) {
      LOG.error(
          "Error in getting username :" + exception.getLocalizedMessage(),
          exception.getCause());
    }
    return username;
  }

  public AWSCredentials createAWSCredentials(String accessKey, String secretKey) {
    if (accessKey != null && secretKey != null) {
      LOG.debug("Creating aws client as per new accesskey and secretkey");
      AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey,
          secretKey);
      return awsCredentials;
    } else {
      return null;
    }
  }
}
