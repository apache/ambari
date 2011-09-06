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

package org.apache.hms.common.entity.manifest;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hms.common.entity.action.Action;
import org.apache.hms.common.entity.action.ScriptAction;

@XmlAccessorType(XmlAccessType.FIELD) 
@XmlType(name="", propOrder = {})
@XmlRootElement
public class ConfigManifest  extends Manifest {
  private static Log LOG = LogFactory.getLog(ConfigManifest.class);

  @XmlElement
  private List<Action> actions;
  
  public List<Action> getActions() {
    return actions;
  }
  
  public void setActions(List<Action> actions) {
    this.actions = actions;
  }

  public void expand(NodesManifest nodes) {
    List<Role> roles = nodes.getRoles();
    Pattern p = Pattern.compile("\\$\\{(.*?)\\}");
    int index = 0;
    for(Action action: actions) {
      if(action instanceof ScriptAction) {
        String[] params = ((ScriptAction) action).getParameters();
        String[] expandedParams = new String[params.length];
        int i = 0;
        for(String param : params) {
          expandedParams[i]=param;          
          Matcher m = p.matcher(param);
          while(m.find()) {
            String token = m.group(1);
            for(Role role : roles) {
              if(role.name.equals(token)) {
                String[] hosts = role.getHosts();
                String replacement = StringUtils.join(hosts, ",");
                expandedParams[i] = param.replace(m.group(0), replacement);
              }
            }
          }
          i++;
        }
        ((ScriptAction) action).setParameters(expandedParams);
        actions.set(index, action);
      }
      index++;
    }    
  }
}
