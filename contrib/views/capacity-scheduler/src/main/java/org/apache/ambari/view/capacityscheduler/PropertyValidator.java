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

package org.apache.ambari.view.capacityscheduler;

import org.apache.ambari.view.ViewInstanceDefinition;
import org.apache.ambari.view.validation.ValidationResult;
import org.apache.ambari.view.validation.Validator;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class PropertyValidator implements Validator {

  public static final String AMBARI_SERVER_URL = "ambari.server.url";

  @Override
  public ValidationResult validateInstance(ViewInstanceDefinition viewInstanceDefinition, ValidationContext validationContext) {
    return null;
  }

  @Override
  public ValidationResult validateProperty(String property, ViewInstanceDefinition viewInstanceDefinition, ValidationContext validationContext) {
    if (viewInstanceDefinition.getClusterHandle() != null) {
      return ValidationResult.SUCCESS;
    }

    if (property.equals(AMBARI_SERVER_URL)) {
      String ambariServerUrl = viewInstanceDefinition.getPropertyMap().get(AMBARI_SERVER_URL);

      if (!ambariServerUrl.matches("^https?://[\\w\\d\\.]+:\\d+/api/v1/clusters/\\w+$")) {
        return new InvalidPropertyValidationResult(false,
            "URL should contain protocol, hostname, port, cluster name, e.g. http://ambari.server:8080/api/v1/clusters/MyCluster");
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
