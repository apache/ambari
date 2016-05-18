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

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class VNameValue implements java.io.Serializable {
  private static final long serialVersionUID = 1L;

  /**
   * Name
   */
  protected String name;
  /**
   * Value
   */
  protected String value;

  /**
   * Default constructor. This will set all the attributes to default value.
   */
  public VNameValue() {
  }

  /**
   * @param name
   *            the key
   * @param value
   *            the value
   */
  public VNameValue(String name, String value) {

    this.name = name;
    this.value = value;
  }

  /**
   * This method sets the value to the member attribute <b>name</b>. You
   * cannot set null to the attribute.
   * 
   * @param name
   *            Value to set member attribute <b>name</b>
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the value for the member attribute <b>name</b>
   * 
   * @return String - value of member attribute <b>name</b>.
   */
  public String getName() {
    return this.name;
  }

  /**
   * This method sets the value to the member attribute <b>value</b>. You
   * cannot set null to the attribute.
   * 
   * @param value
   *            Value to set member attribute <b>value</b>
   */
  public void setValue(String value) {
    if(value.contains(".") && (value.contains("e") || value.contains("E"))){
      this.value=getExponentialValueReplaced(value);
    }else{
      this.value = value;
    }
  }

  /**
   * Returns the value for the member attribute <b>value</b>
   * 
   * @return String - value of member attribute <b>value</b>.
   */
  public String getValue() {
    return this.value;
  }

  /**
   * This return the bean content in string format
   * 
   * @return formatedStr
   */
  public String toString() {
    String str = "VNameValue={";
    str += super.toString();
    str += "name={" + name + "} ";
    str += "value={" + value + "} ";
    str += "}";
    return str;
  }
  
  private String getExponentialValueReplaced(String value) {
    try{
      Double number = Double.parseDouble(value);
      String newValue = String.format("%.0f", number);
      return newValue;
      
    }catch(Exception e){
      return value;
    }
  }
}
