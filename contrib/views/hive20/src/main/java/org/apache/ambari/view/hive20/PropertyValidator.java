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

package org.apache.ambari.view.hive20;

import org.apache.ambari.view.ClusterType;
import org.apache.ambari.view.ViewInstanceDefinition;
import org.apache.ambari.view.utils.ambari.ValidatorUtils;
import org.apache.ambari.view.validation.ValidationResult;
import org.apache.ambari.view.validation.Validator;

public class PropertyValidator implements Validator {

  public static final String WEBHDFS_URL = "webhdfs.url";
  public static final String HIVE_PORT = "hive.port";
  public static final String YARN_ATS_URL = "yarn.ats.url";
  public static final String HIVE_SESSION_PARAMS = "hive.session.params";
  public static final String USE_HIVE_INTERACTIVE_MODE = "use.hive.interactive.mode";

  @Override
  public ValidationResult validateInstance(ViewInstanceDefinition viewInstanceDefinition, ValidationContext validationContext) {
    return null;
  }

  @Override
  public ValidationResult validateProperty(String property, ViewInstanceDefinition viewInstanceDefinition, ValidationContext validationContext) {
    // Validate non cluster associated properties
    if (property.equals(HIVE_SESSION_PARAMS)) {
      String auth = viewInstanceDefinition.getPropertyMap().get(HIVE_SESSION_PARAMS);

      if (auth != null && !auth.isEmpty()) {
        for(String param : auth.split(";")) {
          String[] keyvalue = param.split("=");
          if (keyvalue.length != 2) {
            return new InvalidPropertyValidationResult(false, "Can not parse session param " + param + " in " + auth);
          }
        }
      }
    }

    if (property.equals(USE_HIVE_INTERACTIVE_MODE)) {
      String value = viewInstanceDefinition.getPropertyMap().get(USE_HIVE_INTERACTIVE_MODE);
      if (!("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value))) {
        return new InvalidPropertyValidationResult(false, "Must be 'true' or 'false'");
      }
    }

    // if associated with cluster(local or remote), no need to validate associated properties
    ClusterType clusterType = viewInstanceDefinition.getClusterType();
    if (clusterType == ClusterType.LOCAL_AMBARI || clusterType == ClusterType.REMOTE_AMBARI) {
      return ValidationResult.SUCCESS;
    }

    // Cluster associated properties
    if (property.equals(WEBHDFS_URL)) {
      String webhdfsUrl = viewInstanceDefinition.getPropertyMap().get(WEBHDFS_URL);
      if (!ValidatorUtils.validateHdfsURL(webhdfsUrl)) {
        return new InvalidPropertyValidationResult(false, "Must be valid URL");
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
