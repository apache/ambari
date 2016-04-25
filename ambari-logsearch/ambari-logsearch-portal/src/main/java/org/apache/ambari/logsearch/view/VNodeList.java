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
public class VNodeList extends VList {
  private static final long serialVersionUID = 1L;
  protected List<VNode> vNodeList = new ArrayList<VNode>();

  public List<VNode> getvNodeList() {
    return vNodeList;
  }

  public void setvNodeList(List<VNode> vNodeList) {
    this.vNodeList = vNodeList;
  }

  @Override
  public int getListSize() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public List<VNode> getList() {
    // TODO Auto-generated method stub
    return null;
  }

}
