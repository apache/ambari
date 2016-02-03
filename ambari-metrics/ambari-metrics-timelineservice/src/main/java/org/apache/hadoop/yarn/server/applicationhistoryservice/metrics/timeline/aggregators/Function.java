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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators;

import java.util.Arrays;

/**
 * Is used to determine metrics aggregate table.
 *
 * @see org.apache.hadoop.yarn.server.applicationhistoryservice.webapp.TimelineWebServices#getTimelineMetric
 * @see org.apache.hadoop.yarn.server.applicationhistoryservice.webapp.TimelineWebServices#getTimelineMetrics
 */
public class Function {
  public static Function DEFAULT_VALUE_FUNCTION = new Function(ReadFunction.VALUE, null);
  private static final String SUFFIX_SEPARATOR = "\\._";

  private ReadFunction readFunction = ReadFunction.VALUE;
  private PostProcessingFunction postProcessingFunction = null;

  public Function() {
  }

  public Function(ReadFunction readFunction,
                  PostProcessingFunction ppFunction){
    if (readFunction!=null){
      this.readFunction = readFunction ;
    }
    this.postProcessingFunction = ppFunction;
  }

  /**
   * Segregate post processing function eg: rate from aggregate function,
   * example: avg, in any order
   * @param metricName metric name from request
   * @return @Function
   */
  public static Function fromMetricName(String metricName) {
    // gets postprocessing, and aggregation function
    // ex. Metric._rate._avg
    String[] parts = metricName.split(SUFFIX_SEPARATOR);

    ReadFunction readFunction = ReadFunction.VALUE;
    PostProcessingFunction ppFunction = null;

    if (parts.length <= 1) {
      return new Function(readFunction, null);
    }
    if (parts.length > 3) {
      throw new IllegalArgumentException("Invalid number of functions specified.");
    }

    // Parse functions
    boolean isSuccessful = false; // Best effort
    for (String part : parts) {
      if (ReadFunction.isPresent(part)) {
        readFunction = ReadFunction.getFunction(part);
        isSuccessful = true;
      }
      if (PostProcessingFunction.isPresent(part)) {
        ppFunction = PostProcessingFunction.getFunction(part);
        isSuccessful = true;
      }
    }

    // Throw exception if parsing failed
    if (!isSuccessful) {
      throw new FunctionFormatException("Could not parse provided functions: " +
        "" + Arrays.asList(parts));
    }

    return new Function(readFunction, ppFunction);
  }

  public String getSuffix(){
    return (postProcessingFunction == null)? readFunction.getSuffix() :
      postProcessingFunction.getSuffix() + readFunction.getSuffix();
  }

  public ReadFunction getReadFunction() {
    return readFunction;
  }

  @Override
  public String toString() {
    return "Function{" +
      "readFunction=" + readFunction +
      ", postProcessingFunction=" + postProcessingFunction +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Function)) return false;

    Function function = (Function) o;

    return postProcessingFunction == function.postProcessingFunction
      && readFunction == function.readFunction;

  }

  @Override
  public int hashCode() {
    int result = readFunction.hashCode();
    result = 31 * result + (postProcessingFunction != null ?
      postProcessingFunction.hashCode() : 0);
    return result;
  }

  public enum PostProcessingFunction {
    NONE(""),
    RATE("._rate");

    PostProcessingFunction(String suffix){
      this.suffix = suffix;
    }

    private String suffix = "";

    public String getSuffix(){
      return suffix;
    }

    public static boolean isPresent(String functionName) {
      try {
        PostProcessingFunction.valueOf(functionName.toUpperCase());
      } catch (IllegalArgumentException e) {
        return false;
      }
      return true;
    }

    public static PostProcessingFunction getFunction(String functionName) throws FunctionFormatException {
      if (functionName == null) {
        return NONE;
      }

      try {
        return PostProcessingFunction.valueOf(functionName.toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new FunctionFormatException("Function should be ._rate", e);
      }
    }
  }

  public enum ReadFunction {
    VALUE(""),
    AVG("._avg"),
    MIN("._min"),
    MAX("._max"),
    SUM("._sum");

    private final String suffix;

    ReadFunction(String suffix){
      this.suffix = suffix;
    }

    public String getSuffix() {
      return suffix;
    }

    public static boolean isPresent(String functionName) {
      try {
        ReadFunction.valueOf(functionName.toUpperCase());
      } catch (IllegalArgumentException e) {
        return false;
      }
      return true;
    }

    public static ReadFunction getFunction(String functionName) throws FunctionFormatException {
      if (functionName == null) {
        return VALUE;
      }
      try {
        return ReadFunction.valueOf(functionName.toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new FunctionFormatException(
          "Function should be sum, avg, min, max. Got " + functionName, e);
      }
    }
  }

  public static class FunctionFormatException extends IllegalArgumentException {
    public FunctionFormatException(String message) {
      super(message);
    }

    public FunctionFormatException(String message, Throwable cause) {
      super(message, cause);
    }
  }

}
