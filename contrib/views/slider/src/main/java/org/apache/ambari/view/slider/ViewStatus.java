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

package org.apache.ambari.view.slider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewStatus {
  public static class Validation {
    public static enum TYPE {
      ERROR, WARN
    }

    public String message;
    public String type;

    public Validation(String message, String type) {
      this.message = message;
      this.type = type;
    }
    public Validation(String message) {
      this.message = message;
      this.type = TYPE.ERROR.name();
    }
  }

  private String version;
  private Map<String, String> parameters = new HashMap<String, String>();
  private List<Validation> validations = new ArrayList<ViewStatus.Validation>();

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, String> parameters) {
    this.parameters = parameters;
  }

  public List<Validation> getValidations() {
    return validations;
  }

  public void setValidations(List<Validation> validations) {
    this.validations = validations;
  }
}
