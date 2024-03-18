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

package org.apache.hadoop.metrics2.sink.kafka;

import java.util.regex.Pattern;

import com.yammer.metrics.core.MetricName;

import static java.util.Optional.ofNullable;

public class MetricNameBuilder {
  private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");

  private String group = "jvm";
  private String type;
  private String name;

  private final String replacement;

  static MetricNameBuilder builder() {
    return new MetricNameBuilder();
  }

  MetricNameBuilder() {
    this(null);
  }

  MetricNameBuilder(String replacement) {
    this.replacement = ofNullable(replacement).orElse("_");
  }

  public MetricNameBuilder group(String group) {
    this.group = group;
    return this;
  }

  public MetricNameBuilder type(String type) {
    this.type = replaceWhiteSpaces(type);
    return this;
  }

  public MetricNameBuilder name(String name) {
    this.name = ofNullable(this.name).orElse("") + replaceWhiteSpaces(name);
    return this;
  }

  private String replaceWhiteSpaces(String value) {
    return ofNullable(value)
      .map(val -> WHITESPACE.matcher(val).replaceAll(replacement))
      .orElse("");
  }

  public MetricName build() {
    return new MetricName(this.group, type, name, null, createMBeanName());
  }

  private String createMBeanName() {
    final StringBuilder builder = new StringBuilder();
    builder.append(group);
    builder.append(":type=");
    builder.append(type);
    if (name.length() > 0) {
      builder.append(",name=");
      builder.append(name);
    }
    return builder.toString();
  }

}