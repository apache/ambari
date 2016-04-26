/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.huetoambarimigration.model;

import java.util.Date;

public class PojoPig {

  private Date dt;
  private String script;
  private String Status;
  private String title;
  private String dir;

  public Date getDt() {
    return dt;
  }

  public void setDt(Date dt) {
    this.dt = dt;
  }

  public String getScript() {
    return script;
  }

  public void setScript(String script) {
    this.script = script;
  }

  public String getStatus() {
    return Status;
  }

  public void setStatus(String status) {
    Status = status;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDir() {
    return dir;
  }

  public void setDir(String dir) {
    this.dir = dir;
  }


}
