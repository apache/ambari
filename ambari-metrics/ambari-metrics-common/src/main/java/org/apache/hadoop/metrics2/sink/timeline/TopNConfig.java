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

package org.apache.hadoop.metrics2.sink.timeline;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "topnconfig")
@XmlAccessorType(XmlAccessType.NONE)
@InterfaceAudience.Public
@InterfaceStability.Unstable
public class TopNConfig {
  Integer topN;
  String topNFunction;
  Boolean isBottomN;

  public TopNConfig(Integer topN, String topNFunction, Boolean isBottomN) {
    this.setTopN(topN);
    this.setTopNFunction(topNFunction);
    this.setIsBottomN(isBottomN);
  }

  @XmlElement(name = "topn")
  public Integer getTopN() {
    return topN;
  }

  public void setTopN(Integer topN) {
    this.topN = topN;
  }

  @XmlElement(name = "topnfunction")
  public String getTopNFunction() {
    return topNFunction;
  }

  public void setTopNFunction(String topNFunction) {
    this.topNFunction = topNFunction;
  }

  @XmlElement(name = "isbottomn")
  public Boolean getIsBottomN() {
    return isBottomN;
  }

  public void setIsBottomN(Boolean isBottomN) {
    this.isBottomN = isBottomN;
  }
}
