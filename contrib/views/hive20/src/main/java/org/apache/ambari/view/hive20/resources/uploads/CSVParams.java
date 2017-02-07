/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive20.resources.uploads;

import java.io.Serializable;

public class CSVParams implements Serializable {

  public static final char DEFAULT_DELIMITER_CHAR = ',';
  public static final char DEFAULT_ESCAPE_CHAR = '\\';
  public static final char DEFAULT_QUOTE_CHAR = '"';

  private Character csvDelimiter;
  private Character csvEscape;
  private Character csvQuote;

  public CSVParams() {
  }

  public CSVParams(Character csvDelimiter, Character csvQuote, Character csvEscape) {
    this.csvDelimiter = csvDelimiter;
    this.csvQuote = csvQuote;
    this.csvEscape = csvEscape;
  }

  public Character getCsvDelimiter() {
    return csvDelimiter;
  }

  public void setCsvDelimiter(Character csvDelimiter) {
    this.csvDelimiter = csvDelimiter;
  }

  public Character getCsvEscape() {
    return csvEscape;
  }

  public void setCsvEscape(Character csvEscape) {
    this.csvEscape = csvEscape;
  }

  public Character getCsvQuote() {
    return csvQuote;
  }

  public void setCsvQuote(Character csvQuote) {
    this.csvQuote = csvQuote;
  }

  @Override
  public String toString() {
    return "CSVParams{" +
      "csvDelimiter='" + csvDelimiter + '\'' +
      ", csvEscape='" + csvEscape + '\'' +
      ", csvQuote='" + csvQuote + '\'' +
      '}';
  }
}
