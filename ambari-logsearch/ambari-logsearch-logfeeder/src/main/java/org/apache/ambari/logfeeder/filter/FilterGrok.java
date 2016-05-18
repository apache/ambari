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

package org.apache.ambari.logfeeder.filter;

import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.exception.GrokException;

import org.apache.ambari.logfeeder.LogFeederUtil;
import org.apache.ambari.logfeeder.MetricCount;
import org.apache.ambari.logfeeder.input.InputMarker;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.gson.reflect.TypeToken;

public class FilterGrok extends Filter {
  static private Logger logger = Logger.getLogger(FilterGrok.class);

  private static final String GROK_PATTERN_FILE = "grok-patterns";

  String messagePattern = null;
  String multilinePattern = null;

  Grok grokMultiline = null;
  Grok grokMessage = null;

  StringBuilder strBuff = null;
  String currMultilineJsonStr = null;

  InputMarker firstInputMarker = null;
  InputMarker savedInputMarker = null;

  String sourceField = null;
  boolean removeSourceField = true;

  Set<String> namedParamList = new HashSet<String>();
  Set<String> multiLineamedParamList = new HashSet<String>();

  Type jsonType = new TypeToken<Map<String, String>>() {
  }.getType();

  public MetricCount grokErrorMetric = new MetricCount();

  @Override
  public void init() throws Exception {
    super.init();

    try {
      grokErrorMetric.metricsName = "filter.error.grok";
      // Get the Grok file patterns
      messagePattern = escapePattern(getStringValue("message_pattern"));
      multilinePattern = escapePattern(getStringValue("multiline_pattern"));
      sourceField = getStringValue("source_field");
      removeSourceField = getBooleanValue("remove_source_field",
        removeSourceField);

      logger.info("init() done. grokPattern=" + messagePattern
        + ", multilinePattern=" + multilinePattern + ", "
        + getShortDescription());
      if (StringUtils.isEmpty(messagePattern)) {
        logger.error("message_pattern is not set for filter.");
        return;
      }
      extractNamedParams(messagePattern, namedParamList);

      grokMessage = new Grok();
      // grokMessage.addPatternFromReader(r);
      loadPatterns(grokMessage);
      grokMessage.compile(messagePattern);
      if (!StringUtils.isEmpty(multilinePattern)) {
        extractNamedParams(multilinePattern, multiLineamedParamList);

        grokMultiline = new Grok();
        loadPatterns(grokMultiline);
        grokMultiline.compile(multilinePattern);
      }
    } catch (Throwable t) {
      logger.fatal(
        "Caught exception while initializing Grok. multilinePattern="
          + multilinePattern + ", messagePattern="
          + messagePattern, t);
      grokMessage = null;
      grokMultiline = null;
    }

  }

  /**
   * @param stringValue
   * @return
   */
  private String escapePattern(String inPattern) {
    String inStr = inPattern;
    if (inStr != null) {
      if (inStr.contains("(?m)") && !inStr.contains("(?s)")) {
        inStr = inStr.replaceFirst("(?m)", "(?s)");
      }
      // inStr = inStr.replaceAll("\\[", "\\\\[");
      // inStr = inStr.replaceAll("\\]", "\\\\]");
      // inStr = inStr.replaceAll("\\(", "\\\\(");
      // inStr = inStr.replaceAll("\\)", "\\\\)");
    }
    return inStr;
  }

  private void extractNamedParams(String patternStr, Set<String> paramList) {
    String grokRegEx = "%\\{" + "(?<name>" + "(?<pattern>[A-z0-9]+)"
      + "(?::(?<subname>[A-z0-9_:]+))?" + ")" + "(?:=(?<definition>"
      + "(?:" + "(?:[^{}]+|\\.+)+" + ")+" + ")" + ")?" + "\\}";

    Pattern pattern = Pattern.compile(grokRegEx);
    java.util.regex.Matcher matcher = pattern.matcher(patternStr);
    while (matcher.find()) {
      String subname = matcher.group(3);
      if (subname != null) {
        paramList.add(subname);
      }
    }
  }

  private boolean loadPatterns(Grok grok) {
    InputStreamReader grokPatternsReader = null;
    logger.info("Loading pattern file " + GROK_PATTERN_FILE);
    try {
      BufferedInputStream fileInputStream = (BufferedInputStream) this
        .getClass().getClassLoader()
        .getResourceAsStream(GROK_PATTERN_FILE);
      if (fileInputStream == null) {
        logger.fatal("Couldn't load grok-patterns file "
          + GROK_PATTERN_FILE + ". Things will not work");
        return false;
      }
      grokPatternsReader = new InputStreamReader(fileInputStream);
    } catch (Throwable t) {
      logger.fatal("Error reading grok-patterns file " + GROK_PATTERN_FILE
        + " from classpath. Grok filtering will not work.", t);
      return false;
    }
    try {
      grok.addPatternFromReader(grokPatternsReader);
    } catch (GrokException e) {
      logger.fatal(
        "Error loading patterns from grok-patterns reader for file "
          + GROK_PATTERN_FILE, e);
      return false;
    }

    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.ambari.logfeeder.filter.Filter#apply(java.lang.String)
   */
  @Override
  public void apply(String inputStr, InputMarker inputMarker) {
    if (grokMessage == null) {
      return;
    }

    if (grokMultiline != null) {
      // Check if new line
      String jsonStr = grokMultiline.capture(inputStr);
      if (!"{}".equals(jsonStr)) {
        // New line
        if (strBuff != null) {
          savedInputMarker.beginLineNumber = firstInputMarker.lineNumber;
          // Construct JSON object and add only the interested named
          // parameters
          Map<String, Object> jsonObj = Collections
            .synchronizedMap(new HashMap<String, Object>());
          try {
            // Handle message parsing
            applyMessage(strBuff.toString(), jsonObj,
              currMultilineJsonStr);
          } finally {
            strBuff = null;
            savedInputMarker = null;
            firstInputMarker = null;
          }
        }
        currMultilineJsonStr = jsonStr;
      }

      if (strBuff == null) {
        strBuff = new StringBuilder();
        firstInputMarker = inputMarker;
      } else {
        // strBuff.append(System.lineSeparator());
        strBuff.append('\r');
        strBuff.append('\n');
      }
      strBuff.append(inputStr);
      savedInputMarker = inputMarker;
    } else {
      savedInputMarker = inputMarker;
      Map<String, Object> jsonObj = Collections
        .synchronizedMap(new HashMap<String, Object>());
      applyMessage(inputStr, jsonObj, null);
    }
  }

  @Override
  public void apply(Map<String, Object> jsonObj, InputMarker inputMarker) {
    if (sourceField != null) {
      savedInputMarker = inputMarker;
      applyMessage((String) jsonObj.get(sourceField), jsonObj, null);
      if (removeSourceField) {
        jsonObj.remove(sourceField);
      }
    }
  }

  /**
   * @param inputStr
   * @param jsonObj
   */
  private void applyMessage(String inputStr, Map<String, Object> jsonObj,
                            String multilineJsonStr) {
    String jsonStr = grokParse(inputStr);

    boolean parseError = false;
    if ("{}".equals(jsonStr)) {
      parseError = true;
      // Error parsing string.
      logParseError(inputStr);

      if (multilineJsonStr == null) {
        // TODO: Should we just add this as raw message in solr?
        return;
      }
    }

    if (parseError) {
      jsonStr = multilineJsonStr;
    }
    Map<String, String> jsonSrc = LogFeederUtil.getGson().fromJson(jsonStr,
      jsonType);
    for (String namedParam : namedParamList) {
      if (jsonSrc.get(namedParam) != null) {
        jsonObj.put(namedParam, jsonSrc.get(namedParam));
      }
    }
    if (parseError) {
      // Add error tags
      @SuppressWarnings("unchecked")
      List<String> tagsList = (List<String>) jsonObj.get("tags");
      if (tagsList == null) {
        tagsList = new ArrayList<String>();
        jsonObj.put("tags", tagsList);
      }
      tagsList.add("error_grok_parsing");
      if (sourceField == null) {
        // For now let's put the raw message in log_message, so it is
        // will be searchable
        jsonObj.put("log_message", inputStr);
      }
    }

    super.apply(jsonObj, savedInputMarker);
    statMetric.count++;
  }

  public String grokParse(String inputStr) {
    String jsonStr = grokMessage.capture(inputStr);
    return jsonStr;
  }

  private void logParseError(String inputStr) {
    grokErrorMetric.count++;
    final String LOG_MESSAGE_KEY = this.getClass().getSimpleName()
      + "_PARSEERROR";
    int inputStrLength = inputStr != null ? inputStr.length() : 0;
    LogFeederUtil.logErrorMessageByInterval(
      LOG_MESSAGE_KEY,
      "Error parsing string. length=" + inputStrLength
        + ", input=" + input.getShortDescription()
        + ". First upto 100 characters="
        + LogFeederUtil.subString(inputStr, 100), null, logger,
      Level.WARN);
  }

  @Override
  public void flush() {
    if (strBuff != null) {
      // Handle message parsing
      Map<String, Object> jsonObj = Collections
        .synchronizedMap(new HashMap<String, Object>());
      applyMessage(strBuff.toString(), jsonObj, currMultilineJsonStr);
      strBuff = null;
      savedInputMarker = null;
    }
    super.flush();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.ambari.logfeeder.ConfigBlock#getShortDescription()
   */
  @Override
  public String getShortDescription() {
    return "filter:filter=grok,regex=" + messagePattern;
  }

  @Override
  public void addMetricsContainers(List<MetricCount> metricsList) {
    super.addMetricsContainers(metricsList);
    metricsList.add(grokErrorMetric);
  }

  @Override
  public void logStat() {
    super.logStat();
    // Printing stat for grokErrors
    logStatForMetric(grokErrorMetric, "Stat: Grok Errors");

  }

}
