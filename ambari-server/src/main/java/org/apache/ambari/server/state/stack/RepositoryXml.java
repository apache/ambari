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
package org.apache.ambari.server.state.stack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.apache.ambari.server.stack.Validable;

/**
 * Represents the repository file <code>$STACK_VERSION/repos/repoinfo.xml</code>.
 */
@XmlRootElement(name="reposinfo")
@XmlAccessorType(XmlAccessType.FIELD)
public class RepositoryXml implements Validable{

  @XmlElement(name="latest")
  private String latestUri;
  @XmlElement(name="os")
  private List<Os> oses = new ArrayList<Os>();

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
  private Set<String> errorSet = new HashSet<String>();
  
  @Override
  public void setErrors(String error) {
    errorSet.add(error);
  }

  @Override
  public Collection getErrors() {
    return errorSet;
  } 
 
  @Override
  public void setErrors(Collection error) {
    this.errorSet.addAll(error);
  }  
  
  /**
   * @return the latest URI defined, if any.
   */
  public String getLatestURI() {
    return latestUri;
  }
  
  /**
   * @return the list of <code>os</code> elements.
   */
  public List<Os> getOses() {
    return oses;
  }
  
  /**
   * The <code>os</code> tag.
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Os {
    @XmlAttribute(name="family")
    private String family;
    
    @XmlElement(name="repo")
    private List<Repo> repos;

    private Os() {
    }
    
    /**
     * @return the os family
     */
    public String getFamily() {
      return family;
    }
    
    /**
     * @return the list of repo elements
     */
    public List<Repo> getRepos() {
      return repos;
    }
  }

  /**
   * The <code>repo</code> tag.
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Repo {
    private String baseurl = null;
    private String mirrorslist = null;
    private String repoid = null;
    private String reponame = null;
    private String repocomponents = null;
    private String latest = null;

    private Repo() {
    }
    
    /**
     * @return the base url
     */
    public String getBaseUrl() {
      return (null == baseurl || baseurl.isEmpty()) ? null : baseurl;
    }

    /**
     * @return the mirrorlist field
     */
    public String getMirrorsList() {
      return (null == mirrorslist || mirrorslist.isEmpty()) ? null : mirrorslist;
    }
    
    /**
     * @return the repo id
     */
    public String getRepoId() {
      return repoid;
    }
    
    /**
     * @return the repo name
     */
    public String getRepoName() {
      return reponame;
    }
    
    /**
     * @return the repocomponents
     */
    public String getRepoComponents() {
      return repocomponents;
    }

    public String getLatestUri() {
      return latest;
    }
    
    
  }  
  
}

