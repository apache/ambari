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
package org.apache.ambari.view.hive20.internal.parsers;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class ParserUtilsTest {

  @Test
  public void parseColumnDataTypeDecimalTest(){
    String columnDataTypeString = " decimal(10,2) ";
    List<String> list = ParserUtils.parseColumnDataType(columnDataTypeString);
    Assert.assertEquals("Must contain 3 elements : ", 3, list.size());
    Assert.assertEquals("Failed to find datatype. ", "decimal", list.get(0));
    Assert.assertEquals("Failed to find precision. ", "10", list.get(1));
    Assert.assertEquals("Failed to find scale. ", "2", list.get(2));
  }

  @Test
  public void parseColumnDataTypeDecimalWithSpaceTest(){
    String columnDataTypeString = " decimal ( 10 ,   2 ) ";
    List<String> list = ParserUtils.parseColumnDataType(columnDataTypeString);
    Assert.assertEquals("Must contain 3 elements : ", 3, list.size());
    Assert.assertEquals("Failed to find datatype. ", "decimal", list.get(0));
    Assert.assertEquals("Failed to find precision. ", "10", list.get(1));
    Assert.assertEquals("Failed to find scale. ", "2", list.get(2));
  }

  @Test
  public void parseColumnDataTypeVarcharTest(){
    String columnDataTypeString = " VARCHAR( 10)  ";
    List<String> list = ParserUtils.parseColumnDataType(columnDataTypeString);
    Assert.assertEquals("Must contain 2 elements : ", 3, list.size());
    Assert.assertEquals("Failed to find datatype. ", "VARCHAR", list.get(0));
    Assert.assertEquals("Failed to find precision. ", "10", list.get(1));
    Assert.assertNull("Scale should be null. ", list.get(2));
  }

  @Test
  public void parseColumnDataTypeBooleanTest(){
    String columnDataTypeString = " BOOLEAN  ";
    List<String> list = ParserUtils.parseColumnDataType(columnDataTypeString);
    Assert.assertEquals("Must contain 1 elements : ", 3, list.size());
    Assert.assertEquals("Failed to find datatype. ", "BOOLEAN", list.get(0));
    Assert.assertNull("Precision should be null. ", list.get(1));
    Assert.assertNull("Scale should be null. ", list.get(2));
  }
}