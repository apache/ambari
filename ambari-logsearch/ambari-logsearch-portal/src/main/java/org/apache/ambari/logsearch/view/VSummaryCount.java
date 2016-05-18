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

package org.apache.ambari.logsearch.view;

import java.util.List;

public class VSummaryCount {

  protected String level;

  protected List<String> cricticalMsg;

  protected List<String> compName;

  protected List<Long> countMsg;

  public String getLevel() {
    return level;
  }

  public void setLevel(String level) {
    this.level = level;
  }

  public List<String> getCricticalMsg() {
    return cricticalMsg;
  }

  public void setCricticalMsg(List<String> cricticalMsg) {
    this.cricticalMsg = cricticalMsg;
  }

  public List<String> getCompName() {
    return compName;
  }

  public void setCompName(List<String> compName) {
    this.compName = compName;
  }

  public List<Long> getCountMsg() {
    return countMsg;
  }

  public void setCountMsg(List<Long> countMsg) {
    this.countMsg = countMsg;
  }

}
