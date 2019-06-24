package org.apache.ambari.infra.job.archive;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

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
public class SolrParametrizedStringTest {

  private static final Map<String, Object> PARAMETERS_1 = new HashMap<String, Object>() {{ put("id", "1"); put("name", "User"); put("product", "Computer"); }};
  private static final Map<String, Object> PARAMETERS_START = new HashMap<String, Object>() {{ put("price", "1000"); }};
  private static final Map<String, Object> PARAMETERS_END = new HashMap<String, Object>() {{ put("price", "2000"); }};

  @Test
  public void testToStringEmptyStringResultsEmptyString() {
    assertThat(new SolrParametrizedString("").set(PARAMETERS_1).toString(), is(""));
  }

  @Test
  public void testParameterlessStringResultsItself() {
    assertThat(new SolrParametrizedString("Hello World!").set(PARAMETERS_1).toString(), is("Hello World!"));
  }

  @Test
  public void testParametersAreReplacedIfFoundInString() {
    assertThat(new SolrParametrizedString("Hello ${name}!").set(PARAMETERS_1).toString(), is("Hello User!"));
  }

  @Test
  public void testWhenStringContainsPrefixedParamtersOnlyPrefixedParametersAreSet() {
    assertThat(new SolrParametrizedString("The ${product} price is between $${start.price} and $${end.price}.")
            .set(PARAMETERS_1)
            .set("start", PARAMETERS_START)
            .set("end", PARAMETERS_END).toString(), is("The Computer price is between $1000 and $2000."));
  }
}
