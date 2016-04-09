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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class VNameValueList extends VList {
  private static final long serialVersionUID = 1L;
  List<VNameValue> vNameValues = new ArrayList<VNameValue>();

  public VNameValueList() {
    super();
  }

  public VNameValueList(List<VNameValue> objList) {
    super(objList);
    this.vNameValues = objList;
  }

  /**
   * @return the vNameValues
   */
  public List<VNameValue> getVNameValues() {
    return vNameValues;
  }

  /**
   * @param vNameValues
   *            the vNameValues to set
   */
  public void setVNameValues(List<VNameValue> vNameValues) {
    this.vNameValues = vNameValues;
  }

  @Override
  public int getListSize() {
    if (vNameValues != null) {
      return vNameValues.size();
    }
    return 0;
  }

  @Override
  public List<?> getList() {
    // TODO Auto-generated method stub
    return null;
  }

//  @Override
//  public List<VNameValue> getList() {
//    return vNameValues;
//  }

}
