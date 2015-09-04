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

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SliderAppJmxHelper {

  private static final Logger logger = LoggerFactory
      .getLogger(SliderAppJmxHelper.class);
  private static final String NAME_KEY = "name";
  private static final String PORT_KEY = "tag.port";
  private static final String FORPORT_KEY = "ForPort";
  private static final String JSON_METRIC_START = "$['";
  private static final String JSON_METRIC_END = "']";

  public static JMXTypes jmxTypeExpected(Map<String, Metric> metrics) {
    JMXTypes retVal = null;
    for (Metric metric : metrics.values()) {
      if (retVal == null) {
        retVal = getMetricType(metric.getMetric());
        continue;
      } else {
        if (retVal != getMetricType(metric.getMetric())) {
          retVal = null;
          break;
        }
      }
    }
    return retVal;
  }

  public static void extractMetricsFromJmxBean(InputStream jmxStream, String jmxUrl,
                                               Map<String, String> jmxProperties,
                                               Map<String, Metric> metrics) {
    ObjectMapper jmxObjectMapper = new ObjectMapper();
    jmxObjectMapper.configure(DeserializationConfig.Feature.USE_ANNOTATIONS, false);
    ObjectReader jmxObjectReader = jmxObjectMapper.reader(JMXMetricHolder.class);
    JMXMetricHolder metricHolder = null;
    try {
      metricHolder = jmxObjectReader.readValue(jmxStream);
    } catch (IOException e) {
      logger.error(String.format("Malformed jmx data from %s. Error %s", jmxUrl, e.getMessage()));
    }

    Map<String, Map<String, Object>> categories = new HashMap<String, Map<String, Object>>();

    for (Map<String, Object> bean : metricHolder.getBeans()) {
      String category = getCategory(bean);
      if (category != null) {
        categories.put(category, bean);
      }
    }

    addJmxPropertiesFromBeans(jmxProperties, categories, metrics);
  }

  public static void extractMetricsFromJmxJson(InputStream jmxStream, String jmxUrl,
                                               Map<String, String> jmxProperties,
                                               Map<String, Metric> metrics)
      throws IOException, ParseException {
    JSONParser parser = new JSONParser();
    Object obj = parser.parse(IOUtils.toString(jmxStream));
    JSONObject jsonObject = (JSONObject) obj;
    for (String key : metrics.keySet()) {
      Metric metric = metrics.get(key);
      String jsonKey = extractJsonKeySingleLevel(metric.getMetric());
      Object value = jsonObject.get(jsonKey);
      if (value != null) {
        jmxProperties.put(key, value.toString());
      }
    }
  }

  private static String extractJsonKeySingleLevel(String metricKey) {
    String jsonKey = metricKey;
    if (metricKey != null) {
      if (metricKey.startsWith(JSON_METRIC_START) && metricKey.endsWith(JSON_METRIC_END)) {
        jsonKey = metricKey.substring(JSON_METRIC_START.length(), metricKey.length() - JSON_METRIC_END.length());
      }
    }

    return jsonKey;
  }

  public static void extractMetricsFromJmxXML(InputStream jmxStream, String jmxUrl,
                                              Map<String, String> jmxProperties,
                                              Map<String, Metric> metrics)
      throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(jmxStream);
    for (String key : metrics.keySet()) {
      Metric metric = metrics.get(key);
      XPathExpression xPathExpression = metric.getxPathExpression();
      if (xPathExpression != null) {
        String value = xPathExpression.evaluate(doc);
        if (value != null) {
          jmxProperties.put(key, value.toString().trim());
        }
      }
    }
  }

  private static String getCategory(Map<String, Object> bean) {
    if (bean.containsKey(NAME_KEY)) {
      String name = (String) bean.get(NAME_KEY);

      if (bean.containsKey(PORT_KEY)) {
        String port = (String) bean.get(PORT_KEY);
        name = name.replace(FORPORT_KEY + port, "");
      }
      return name;
    }
    return null;
  }

  protected static void addJmxPropertiesFromBeans(Map<String, String> jmxProperties,
                                                  Map<String, Map<String, Object>> categories,
                                                  Map<String, Metric> relevantMetrics) {
    for (String metricName : relevantMetrics.keySet()) {
      Metric metric = relevantMetrics.get(metricName);
      String beanName = metric.getJmxBeanKeyName();
      Object value = categories.get(beanName);
      if (value instanceof Map) {
        Map<?, ?> map = (Map<?, ?>) value;
        for (List<String> matcher : metric.getMatchers()) {
          boolean foundMetrics = false;
          for (int matchIndex = 0; matchIndex < matcher.size(); matchIndex++) {
            String matchKey = matcher.get(matchIndex);
            value = map.get(matchKey);
            if (value instanceof Map) {
              map = (Map<?, ?>) value;
              continue;
            } else {
              if (value != null && matchIndex == matcher.size() - 1) {
                jmxProperties.put(metricName, value.toString());
                foundMetrics = true;
              } else {
                break;
              }
            }
          }
          if (foundMetrics) {
            break;
          }
        }
      }
    }
  }

  private static JMXTypes getMetricType(String metricKey) {
    assert metricKey != null;
    if (metricKey.startsWith("/")) {
      return JMXTypes.XML;
    } else if (metricKey.startsWith("$")) {
      return JMXTypes.JSON;
    } else {
      return JMXTypes.JMX_BEAN;
    }
  }

  public enum JMXTypes {
    JMX_BEAN,
    JSON,
    XML
  }
}
