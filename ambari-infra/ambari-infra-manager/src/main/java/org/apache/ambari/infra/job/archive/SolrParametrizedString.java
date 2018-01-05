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
package org.apache.ambari.infra.job.archive;

import org.apache.solr.client.solrj.util.ClientUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SolrParametrizedString {
  private static final String PARAMETER_PATTERN = "\\$\\{%s[a-z0-9A-Z]+}";
  private static final Pattern NO_PREFIX_PARAMETER_PATTERN = Pattern.compile(String.format(PARAMETER_PATTERN, ""));

  private final String string;

  public SolrParametrizedString(String string) {
    this.string = string;
  }

  private Set<String> collectParamNames(Pattern regExPattern) {
    Matcher matcher = regExPattern.matcher(string);
    Set<String> parameters = new HashSet<>();
    while (matcher.find())
      parameters.add(matcher.group().replace("${", "").replace("}", ""));
    return parameters;
  }

  @Override
  public String toString() {
    return string;
  }

  public SolrParametrizedString set(Map<String, String> parameterMap) {
    return set(NO_PREFIX_PARAMETER_PATTERN, null, parameterMap);
  }

  public SolrParametrizedString set(String prefix, Map<String, String> parameterMap) {
    String dottedPrefix = prefix + ".";
    return set(Pattern.compile(String.format(PARAMETER_PATTERN, dottedPrefix)), dottedPrefix, parameterMap);
  }

  private SolrParametrizedString set(Pattern regExPattern, String prefix, Map<String, String> parameterMap) {
    String newString = string;
    for (String paramName : collectParamNames(regExPattern)) {
      String paramSuffix = prefix == null ? paramName : paramName.replace(prefix, "");
      if (parameterMap.get(paramSuffix) != null)
        newString = newString.replace(String.format("${%s}", paramName), getValue(parameterMap, paramSuffix));
    }
    return new SolrParametrizedString(newString);
  }

  private String getValue(Map<String, String> parameterMap, String paramSuffix) {
    String value = parameterMap.get(paramSuffix);
    if ("*".equals(value))
      return value;
    return ClientUtils.escapeQueryChars(value);
  }
}
