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

package org.apache.ambari.view.slider.rest.client;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties({"keyName", "matchers", "xPathExpression", "xPathExpressionComputed"})
public class Metric {
  private static final Logger logger = LoggerFactory
      .getLogger(Metric.class);
  private static String SEPARATOR = ".";
  private static char SEPARATOR_REPLACED = '#';
  private String metric;
  private boolean pointInTime;
  private boolean temporal;
  @JsonIgnore
  private String keyName = null;
  @JsonIgnore
  private List<List<String>> matchers = null;
  @JsonIgnore
  private XPathExpression xPathExpression = null;
  @JsonIgnore
  private boolean xPathExpressionComputed = false;

  private Metric() {
  }

  protected Metric(String metric, boolean pointInTime, boolean temporal) {
    this.metric = metric;
    this.pointInTime = pointInTime;
    this.temporal = temporal;
  }

  public String getMetric() {
    return metric;
  }

  public void setMetric(String metric) {
    this.metric = metric;
  }

  public boolean isPointInTime() {
    return pointInTime;
  }

  public void setPointInTime(boolean pointInTime) {
    this.pointInTime = pointInTime;
  }

  public boolean isTemporal() {
    return temporal;
  }

  public void setTemporal(boolean temporal) {
    this.temporal = temporal;
  }

  @JsonIgnore
  public XPathExpression getxPathExpression() {
    if (!xPathExpressionComputed) {
      XPathFactory xPathfactory = XPathFactory.newInstance();
      XPath xpath = xPathfactory.newXPath();
      XPathExpression schemaPath = null;
      try {
        schemaPath = xpath.compile(metric);
      } catch (XPathExpressionException e) {
        logger.info(String.format("Unable to compile %s into xpath expression", metric));
      }
      xPathExpression = schemaPath;
      xPathExpressionComputed = true;
    }

    return xPathExpression;
  }

  @JsonIgnore
  public String getJmxBeanKeyName() {
    if (keyName == null) {
      int firstIndex = metric.indexOf(SEPARATOR);
      if (firstIndex > 0) {
        keyName = metric.substring(0, firstIndex).replace(SEPARATOR_REPLACED, '.');
      }
    }
    return keyName;
  }

  /**
   * Matcher is of the form a.b.c... They can be matched as a -> b-> c or a.b -> c or a -> b.c etc. The matcher returns
   * all possibilities in priority order
   *
   * @return
   */
  @JsonIgnore
  public List<List<String>> getMatchers() {
    if (matchers == null) {
      List<List<String>> tmpMatchers = new ArrayList<List<String>>();
      int matcherStartIndex = metric.indexOf(SEPARATOR);
      if (matcherStartIndex > 0) {
        String allTagsStr = metric.substring(matcherStartIndex + 1);
        String[] tags = allTagsStr.split("\\.");
        if (tags.length > 0) {
          extractMatchers(tags, -1, tmpMatchers, null);
        }
      }

      matchers = tmpMatchers;
    }
    return matchers;
  }

  public void extractMatchers(String[] tags, int index, List<List<String>> matchers, ArrayList<String> currentSet) {
    if (tags.length == index + 1) {
      matchers.add(currentSet);
    } else {
      if (index == -1) {
        currentSet = new ArrayList<String>();
        currentSet.add(tags[0]);
        extractMatchers(tags, 0, matchers, currentSet);
      } else {
        ArrayList<String> mergeAndProceed = new ArrayList<String>(currentSet);
        mergeAndProceed.add(tags[index + 1]);
        extractMatchers(tags, index + 1, matchers, mergeAndProceed);

        ArrayList<String> appendAndProceed = new ArrayList<String>(currentSet);
        int lastIndex = appendAndProceed.size() - 1;
        appendAndProceed.set(lastIndex, appendAndProceed.get(lastIndex) + "." + tags[index + 1]);
        extractMatchers(tags, index + 1, matchers, appendAndProceed);
      }
    }
  }
}
