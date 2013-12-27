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
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents the repository file <code>$STACK_VERSION/metainfo.xml</code>.
 */
@XmlRootElement(name="reposinfo")
@XmlAccessorType(XmlAccessType.FIELD)
public class RepositoryXml {
  @XmlElement(name="os")
  private List<Os> oses = new ArrayList<Os>();
  
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
    @XmlAttribute(name="type")
    private String type;
    
    @XmlElement(name="repo")
    private List<Repo> repos;

    private Os() {
    }
    
    /**
     * @return the os type
     */
    public String getType() {
      return type;
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
    private String baseurl;
    private String mirrorslist;
    private String repoid;
    private String reponame;

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
    
    
  }  
  
}

