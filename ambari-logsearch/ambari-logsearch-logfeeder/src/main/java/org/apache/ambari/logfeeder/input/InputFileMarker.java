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
package org.apache.ambari.logfeeder.input;

import org.apache.ambari.logfeeder.plugin.input.Input;
import org.apache.ambari.logfeeder.plugin.input.InputMarker;

import java.util.HashMap;
import java.util.Map;

public class InputFileMarker implements InputMarker {

  private final Input input;
  private final String base64FileKey;
  private final Integer lineNumber;

  private final Map<String, Object> properties = new HashMap<>();

  public InputFileMarker(Input input, String base64FileKey, Integer lineNumber) {
    this.input = input;
    this.base64FileKey = base64FileKey;
    this.lineNumber = lineNumber;
    properties.put("line_number", lineNumber);
    properties.put("base64_file_key", base64FileKey);
  }

  @Override
  public Input getInput() {
    return this.input;
  }

  @Override
  public Map<String, Object> getAllProperties() {
    return properties;
  }

  public String getBase64FileKey() {
    return base64FileKey;
  }

  public int getLineNumber() {
    return lineNumber;
  }
}
