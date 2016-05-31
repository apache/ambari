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

package org.apache.ambari.view.hive2.resources.savedQueries;

import org.apache.ambari.view.hive2.persistence.utils.PersonalResource;
import org.apache.commons.beanutils.BeanUtils;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Bean to represent saved query
 */
public class SavedQuery implements Serializable, PersonalResource {
  private String queryFile;
  private String dataBase;
  private String title;
  private String shortQuery;

  private String id;
  private String owner;

  public SavedQuery() {}
  public SavedQuery(Map<String, Object> stringObjectMap) throws InvocationTargetException, IllegalAccessException {
    BeanUtils.populate(this, stringObjectMap);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getOwner() {
    return owner;
  }

  @Override
  public void setOwner(String owner) {
    this.owner = owner;
  }

  public String getQueryFile() {
    return queryFile;
  }

  public void setQueryFile(String queryFile) {
    this.queryFile = queryFile;
  }

  public String getDataBase() {
    return dataBase;
  }

  public void setDataBase(String dataBase) {
    this.dataBase = dataBase;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getShortQuery() {
    return shortQuery;
  }

  public void setShortQuery(String shortQuery) {
    this.shortQuery = shortQuery;
  }
}
