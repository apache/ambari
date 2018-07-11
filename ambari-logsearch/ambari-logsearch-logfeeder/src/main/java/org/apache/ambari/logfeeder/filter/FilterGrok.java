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

import com.google.gson.reflect.TypeToken;
import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.exception.GrokException;
import org.apache.ambari.logfeeder.conf.LogFeederProps;
import org.apache.ambari.logfeeder.plugin.common.MetricData;
import org.apache.ambari.logfeeder.plugin.filter.Filter;
import org.apache.ambari.logfeeder.plugin.input.InputMarker;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.ambari.logsearch.config.api.model.inputconfig.FilterGrokDescriptor;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.InputStream;
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

public class FilterGrok extends Filter<LogFeederProps> {
  private static final Logger LOG = Logger.getLogger(FilterGrok.class);

  private static final String GROK_PATTERN_FILE = "grok-patterns";

  private String messagePattern = null;
  private String multilinePattern = null;

  private Grok grokMultiline = null;
  private Grok grokMessage = null;

  private StringBuilder strBuff = null;
  private String currMultilineJsonStr = null;

  private InputMarker savedInputMarker = null;

  private String sourceField = null;
  private boolean removeSourceField = true;

  private Set<String> namedParamList = new HashSet<String>();
  private Set<String> multiLineamedParamList = new HashSet<String>();

  private Type jsonType = new TypeToken<Map<String, String>>() {}.getType();

  private MetricData grokErrorMetric = new MetricData("filter.error.grok", false);

  private boolean skipOnError = false;

  @Override
  public void init(LogFeederProps logFeederProps) throws Exception {
    super.init(logFeederProps);

    try {
      messagePattern = escapePattern(((FilterGrokDescriptor)getFilterDescriptor()).getMessagePattern());
      multilinePattern = escapePattern(((FilterGrokDescriptor)getFilterDescriptor()).getMultilinePattern());
      sourceField = getFilterDescriptor().getSourceField();
      removeSourceField = BooleanUtils.toBooleanDefaultIfNull(getFilterDescriptor().isRemoveSourceField(), removeSourceField);
      skipOnError = ((FilterGrokDescriptor) getFilterDescriptor()).isSkipOnError();

      LOG.info("init() done. grokPattern=" + messagePattern + ", multilinePattern=" + multilinePattern + ", " +
      getShortDescription());
      if (StringUtils.isEmpty(messagePattern)) {
        LOG.error("message_pattern is not set for filter.");
        return;
      }
      extractNamedParams(messagePattern, namedParamList);

      grokMessage = new Grok();
      loadPatterns(grokMessage);
      grokMessage.compile(messagePattern);
      if (((FilterGrokDescriptor)getFilterDescriptor()).isDeepExtract()) {
        extractNamedParams(grokMessage.getNamedRegexCollection());
      } else {
        extractNamedParams(messagePattern, namedParamList);
      }
      if (!StringUtils.isEmpty(multilinePattern)) {
        extractNamedParams(multilinePattern, multiLineamedParamList);

        grokMultiline = new Grok();
        loadPatterns(grokMultiline);
        grokMultiline.compile(multilinePattern);
      }
    } catch (Throwable t) {
      LOG.fatal("Caught exception while initializing Grok. multilinePattern=" + multilinePattern + ", messagePattern="
          + messagePattern, t);
      grokMessage = null;
      grokMultiline = null;
    }

  }

  private String escapePattern(String inPattern) {
    String inStr = inPattern;
    if (inStr != null) {
      if (inStr.contains("(?m)") && !inStr.contains("(?s)")) {
        inStr = inStr.replaceFirst("(?m)", "(?s)");
      }
    }
    return inStr;
  }

  private void extractNamedParams(String patternStr, Set<String> paramList) {
    String grokRegEx = "%\\{" +
        "(?<name>" + "(?<pattern>[A-z0-9]+)" + "(?::(?<subname>[A-z0-9_:]+))?" + ")" +
        "(?:=(?<definition>" + "(?:" + "(?:[^{}]+|\\.+)+" + ")+" + ")" + ")?" +
        "\\}";

    Pattern pattern = Pattern.compile(grokRegEx);
    java.util.regex.Matcher matcher = pattern.matcher(patternStr);
    while (matcher.find()) {
      String subname = matcher.group(3);
      if (subname != null) {
        paramList.add(subname);
      }
    }
  }

  private void extractNamedParams(Map<String, String> namedRegexCollection) {
    if (namedRegexCollection != null) {
      for (String paramValue : namedRegexCollection.values()) {
        if (paramValue.toLowerCase().equals(paramValue)) {
          namedParamList.add(paramValue);
        }
      }
    }
  }

  private boolean loadPatterns(Grok grok) {
    InputStreamReader grokPatternsReader = null;
    LOG.info("Loading pattern file " + GROK_PATTERN_FILE);
    try {
      InputStream fileInputStream = getClass().getClassLoader().getResourceAsStream(GROK_PATTERN_FILE);
      if (fileInputStream == null) {
        LOG.fatal("Couldn't load grok-patterns file " + GROK_PATTERN_FILE + ". Things will not work");
        return false;
      }
      grokPatternsReader = new InputStreamReader(fileInputStream);
    } catch (Throwable t) {
      LOG.fatal("Error reading grok-patterns file " + GROK_PATTERN_FILE + " from classpath. Grok filtering will not work.", t);
      return false;
    }
    try {
      grok.addPatternFromReader(grokPatternsReader);
    } catch (GrokException e) {
      LOG.fatal("Error loading patterns from grok-patterns reader for file " + GROK_PATTERN_FILE, e);
      return false;
    }

    return true;
  }

  @Override
  public void apply(String inputStr, InputMarker inputMarker) throws Exception {
    if (grokMessage == null) {
      return;
    }

    if (grokMultiline != null) {
      String jsonStr = grokMultiline.capture(inputStr);
      if (!"{}".equals(jsonStr) || skipOnError) {
        if (strBuff != null) {
          Map<String, Object> jsonObj = Collections.synchronizedMap(new HashMap<String, Object>());
          try {
            LogFeederUtil.fillMapWithFieldDefaults(jsonObj, inputMarker, false);
            applyMessage(strBuff.toString(), jsonObj, currMultilineJsonStr);
          } finally {
            strBuff = null;
            savedInputMarker = null;
          }
        }
        currMultilineJsonStr = jsonStr;
      }

      if (strBuff == null) {
        strBuff = new StringBuilder();
      } else {
        strBuff.append("\r\n");
      }
      strBuff.append(inputStr);
      savedInputMarker = inputMarker;
    } else {
      savedInputMarker = inputMarker;
      Map<String, Object> jsonObj = Collections.synchronizedMap(new HashMap<String, Object>());
      LogFeederUtil.fillMapWithFieldDefaults(jsonObj, inputMarker, false);
      applyMessage(inputStr, jsonObj, null);
    }
  }

  @Override
  public void apply(Map<String, Object> jsonObj, InputMarker inputMarker) throws Exception {
    if (sourceField != null) {
      savedInputMarker = inputMarker;
      LogFeederUtil.fillMapWithFieldDefaults(jsonObj, inputMarker, false);
      applyMessage((String) jsonObj.get(sourceField), jsonObj, null);
      if (removeSourceField) {
        jsonObj.remove(sourceField);
      }
    }
  }

  private void applyMessage(String inputStr, Map<String, Object> jsonObj, String multilineJsonStr) throws Exception {
    String jsonStr = grokMessage.capture(inputStr);

    boolean parseError = false;
    if ("{}".equals(jsonStr) && !skipOnError) {
      parseError = true;
      logParseError(inputStr);

      if (multilineJsonStr == null) {
        // TODO: Should we just add this as raw message in solr?
        return;
      }
    }

    if (parseError) {
      jsonStr = multilineJsonStr;
    }
    Map<String, String> jsonSrc = LogFeederUtil.getGson().fromJson(jsonStr, jsonType);
    for (String namedParam : namedParamList) {
      if (jsonSrc.get(namedParam) != null) {
        jsonObj.put(namedParam, jsonSrc.get(namedParam));
      }
    }
    if (parseError) {
      @SuppressWarnings("unchecked")
      List<String> tagsList = (List<String>) jsonObj.get("tags");
      if (tagsList == null) {
        tagsList = new ArrayList<String>();
        jsonObj.put("tags", tagsList);
      }
      tagsList.add("error_grok_parsing");
      if (sourceField == null) {
        // For now let's put the raw message in log_message, so it is will be searchable
        jsonObj.put("log_message", inputStr);
      }
    }
    super.apply(jsonObj, savedInputMarker);
    statMetric.value++;
  }

  private void logParseError(String inputStr) {
    grokErrorMetric.value++;
    String logMessageKey = this.getClass().getSimpleName() + "_PARSEERROR";
    int inputStrLength = inputStr != null ? inputStr.length() : 0;
    LogFeederUtil.logErrorMessageByInterval(logMessageKey, "Error parsing string. length=" + inputStrLength + ", input=" +
        getInput().getShortDescription() + ". First upto 100 characters=" + StringUtils.abbreviate(inputStr, 100), null, LOG,
        Level.WARN);
  }

  @Override
  public void flush() {
    if (strBuff != null) {
      Map<String, Object> jsonObj = Collections.synchronizedMap(new HashMap<String, Object>());
      try {
        applyMessage(strBuff.toString(), jsonObj, currMultilineJsonStr);
      } catch (Exception e) {
        LOG.error(e.getLocalizedMessage(), e.getCause());
      }
      strBuff = null;
      savedInputMarker = null;
    }
    super.flush();
  }

  @Override
  public String getShortDescription() {
    return "filter:filter=grok,regex=" + messagePattern;
  }

  @Override
  public void addMetricsContainers(List<MetricData> metricsList) {
    super.addMetricsContainers(metricsList);
    metricsList.add(grokErrorMetric);
  }

  @Override
  public void logStat() {
    super.logStat();
    logStatForMetric(grokErrorMetric, "Stat: Grok Errors");
  }
}
