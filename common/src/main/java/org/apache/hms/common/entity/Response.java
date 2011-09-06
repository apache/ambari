/*
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

package org.apache.hms.common.entity;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.hms.common.entity.RestSource;

@XmlRootElement
public class Response extends RestSource {
  @XmlElement(name="exit_code")
  public int code;
  @XmlElement
  public String output;
  @XmlElement
  public String error;
  
  public int getCode() {
    return code;
  }
  
  public String getOutput() {
    return output;
  }
  
  public String getError() {
    return error;
  }
  
  public void setCode(int code) {
    this.code = code;  
  }
  
  public void setOutput(String output) {
    this.output = output;
  }
  
  public void setError(String error) {
    this.error = error;
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("exit code:");
    sb.append(code);
    sb.append("\nstdout:\n");
    sb.append(output);
    sb.append("\nstderr:\n");
    sb.append(error);
    sb.append("\n");
    return sb.toString();
  }
}
