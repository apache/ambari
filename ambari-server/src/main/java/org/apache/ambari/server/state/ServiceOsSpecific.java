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
package org.apache.ambari.server.state;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;
/**
 * Represents service os-specific details (like repositories and packages). 
 * Represents <code>osSpecific</code>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceOsSpecific {

  private String osType;
  private Repo repo;


  @XmlElementWrapper(name="packages")
  @XmlElements(@XmlElement(name="package"))
  private List<Package> packages = new ArrayList<Package>();


  public String getOsType() {
    return osType;
  }


  public Repo getRepo() {
    return repo;
  }


  public List<Package> getPackages() {
    return packages;
  }

  /**
   * The <code>repo</code> tag. It has different set of fields compared to
   * <link>org.apache.ambari.server.state.RepositoryInfo</link>,
   * that's why we need another class
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Repo {

    @SerializedName("baseUrl")
    private String baseurl;
    @SerializedName("mirrorsList")
    private String mirrorslist;
    @SerializedName("repoId")
    private String repoid;
    @SerializedName("repoName")
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



  /**
   * The <code>package</code> tag.
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Package {
    private String type;
    private String name;

    public String getType() {
      return type;
    }

    public String getName() {
      return name;
    }

    private Package() { }
  }


}

