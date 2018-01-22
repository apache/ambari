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
package org.apache.ambari.server.state.stack;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.ambari.server.stack.Validable;
import org.apache.ambari.server.state.OsSpecific;

/**
 * Represents the stack <code>metainfo.xml</code> file.
 */
@XmlRootElement(name="metainfo")
@XmlAccessorType(XmlAccessType.FIELD)
public class StackMetainfoXml implements Validable{

  public String getMinJdk() {
    return minJdk;
  }

  public void setMinJdk(String minJdk) {
    this.minJdk = minJdk;
  }

  public String getMaxJdk() {
    return maxJdk;
  }

  public void setMaxJdk(String maxJdk) {
    this.maxJdk = maxJdk;
  }

  @XmlElement(name="minJdk")
  private String minJdk = null;

  @XmlElement(name="maxJdk")
  private String maxJdk = null;

  @XmlElement(name="extends")
  private String extendsVersion = null;

  public void setExtendsVersion(String extendsVersion) {
    this.extendsVersion = extendsVersion;
  }
  
  @XmlElement(name="versions")
  private Version version = new Version();

  @XmlTransient
  private boolean valid = true;

  /**
   * 
   * @return valid xml flag
   */
  @Override
  public boolean isValid() {
    return valid;
  }

  /**
   * 
   * @param valid set validity flag
   */
  @Override
  public void setValid(boolean valid) {
    this.valid = valid;
  }
  
  @XmlTransient
  private Set<String> errorSet = new HashSet<>();
  
  @Override
  public void addError(String error) {
    errorSet.add(error);
  }

  @Override
  public Collection<String> getErrors() {
    return errorSet;
  }   

  @Override
  public void addErrors(Collection<String> errors) {
    this.errorSet.addAll(errors);
  }
  
  /**
   * @return the parent stack version number
   */
  public String getExtends() {
    return extendsVersion;
  }
  
  /**
   * @return gets the version
   */
  public Version getVersion() {
    return version;
  }

  public void setVersion(Version version) {
    this.version = version;
  }

  /**
   * Internal list of os-specific details (loaded from xml). Added at schema ver 2
   */
  @XmlElementWrapper(name="osSpecifics")
  @XmlElements(@XmlElement(name="osSpecific"))
  private List<OsSpecific> osSpecifics;

  /**
   * Map of of os-specific details that is exposed (and initialised from list)
   * at getter.
   * Added at schema ver 2
   */
  @XmlTransient
  private volatile Map<String, OsSpecific> stackOsSpecificsMap;

  public Map<String,OsSpecific> getOsSpecifics() {
    if (stackOsSpecificsMap == null) {
      synchronized (this) { // Double-checked locking pattern
        if (stackOsSpecificsMap == null) {
          Map<String, OsSpecific> tmpMap =
              new TreeMap<>();
          if (osSpecifics != null) {
            for (OsSpecific osSpecific : osSpecifics) {
              tmpMap.put(osSpecific.getOsFamily(), osSpecific);
            }
          }
          stackOsSpecificsMap = tmpMap;
        }
      }
    }
    return stackOsSpecificsMap;
  }
  public void setOsSpecifics(Map<String, OsSpecific> stackOsSpecificsMap) {
    this.stackOsSpecificsMap = stackOsSpecificsMap;
  }

  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Version {
    public Version() {
    }
    private boolean active = false;
    private String upgrade = null;
    
    /**
     * @return <code>true</code> if the stack is active
     */
    public boolean isActive() {
      return active;
    }

    public void setActive(boolean active) {
      this.active = active;
    }
    
    /**
     * @return the upgrade version number, if set
     */
    public String getUpgrade() {
      return upgrade;
    }
    
    
  }  
  
}



