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

package org.apache.ambari.view.hive;

import org.apache.ambari.view.ViewInstanceDefinition;
import org.apache.ambari.view.utils.ambari.ValidatorUtils;
import org.apache.ambari.view.validation.ValidationResult;
import org.apache.ambari.view.validation.Validator;

public class PropertyValidator implements Validator {

  public static final String WEBHDFS_URL = "webhdfs.url";
  public static final String HIVE_PORT = "hive.port";
  public static final String YARN_ATS_URL = "yarn.ats.url";
  public static final String HIVE_AUTH = "hive.auth";

  @Override
  public ValidationResult validateInstance(ViewInstanceDefinition viewInstanceDefinition, ValidationContext validationContext) {
    return null;
  }

  @Override
  public ValidationResult validateProperty(String property, ViewInstanceDefinition viewInstanceDefinition, ValidationContext validationContext) {
    // Validate non cluster associated properties
    if (property.equals(HIVE_AUTH)) {
      String auth = viewInstanceDefinition.getPropertyMap().get(HIVE_AUTH);

      if (auth != null && !auth.isEmpty()) {
        for(String param : auth.split(";")) {
          String[] keyvalue = param.split("=");
          if (keyvalue.length != 2) {
            return new InvalidPropertyValidationResult(false, "Can not parse authentication param " + param + " in " + auth);
          }
        }
      }
    }

    // if associated with cluster, no need to validate associated properties
    String cluster = viewInstanceDefinition.getClusterHandle();
    if (cluster != null) {
      return ValidationResult.SUCCESS;
    }

    // Cluster associated properties
    if (property.equals(WEBHDFS_URL)) {
      String webhdfsUrl = viewInstanceDefinition.getPropertyMap().get(WEBHDFS_URL);
      if (!ValidatorUtils.validateHdfsURL(webhdfsUrl)) {
        return new InvalidPropertyValidationResult(false, "Must be valid URL");
      }
    }

    if (property.equals(HIVE_PORT)) {
      String hivePort = viewInstanceDefinition.getPropertyMap().get(HIVE_PORT);
      if (hivePort != null) {
        try {
          int port = Integer.valueOf(hivePort);
          if (port < 1 || port > 65535) {
            return new InvalidPropertyValidationResult(false, "Must be from 1 to 65535");
          }
        } catch (NumberFormatException e) {
          return new InvalidPropertyValidationResult(false, "Must be integer");
        }
      }
    }

    if (property.equals(YARN_ATS_URL)) {
      String atsUrl = viewInstanceDefinition.getPropertyMap().get(YARN_ATS_URL);
      if (!ValidatorUtils.validateHttpURL(atsUrl)) {
        return new InvalidPropertyValidationResult(false, "Must be valid URL");
      }
    }

    return ValidationResult.SUCCESS;
  }

  public static class InvalidPropertyValidationResult implements ValidationResult {
    private boolean valid;
    private String detail;

    public InvalidPropertyValidationResult(boolean valid, String detail) {
      this.valid = valid;
      this.detail = detail;
    }

    @Override
    public boolean isValid() {
      return valid;
    }

    @Override
    public String getDetail() {
      return detail;
    }
  }

}
