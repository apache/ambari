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

package org.apache.ambari.common.rest.entities.agent;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {})
public class ServerStatus {
  public State state;
  
  public ServerStatus() {  
  }
  
  public ServerStatus(State state) {
    this.state = state;
  }
  
  public State getState() {
    return state;
  }
  
  public void setState(State state) {
    this.state = state;
  }
  
  public static enum State {
    START, STARTING, STARTED, STOP, STOPPING, STOPPED;
    public static class ServerStateAdaptor extends XmlAdapter<String, State> {
      @Override
      public String marshal(State obj) throws Exception {
        return obj.toString();
      }

      @Override
      public State unmarshal(String str) throws Exception {
        for (State j : State.class.getEnumConstants()) {
          if (j.toString().equals(str)) {
            return j;
          }
        }
        throw new Exception("Can't convert " + str + " to "
          + State.class.getName());
      }

    }
  }
}
