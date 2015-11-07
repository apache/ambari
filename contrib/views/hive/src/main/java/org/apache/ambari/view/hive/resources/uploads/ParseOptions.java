/**
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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive.resources.uploads;

import java.util.HashMap;

public class ParseOptions {
  final public static String OPTIONS_FILE_TYPE = "FILE_TYPE";
  final public static String OPTIONS_HEADER = "HEADER";
  final public static String OPTIONS_NUMBER_OF_PREVIEW_ROWS = "NUMBER_OF_PREVIEW_ROWS";

  final public static String FILE_TYPE_CSV = "CSV";
  final public static String FILE_TYPE_JSON = "JSON";
  final public static String XML = "XML";

  final public static String HEADER_FIRST_RECORD = "FIRST_RECORD";
  final public static String HEADER_PROVIDED_BY_USER = "PROVIDED_BY_USER";

  final public static String HEADERS = "HEADERS";

  private HashMap<String, Object> options = new HashMap<String, Object>();

  public void setOption(String key, Object value) {
    this.options.put(key, value);
  }

  public Object getOption(String key) {
    return this.options.get(key);
  }
}
