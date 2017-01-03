/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive20.internal.parsers;

import org.apache.ambari.view.hive20.client.Row;
import org.apache.ambari.view.hive20.internal.dto.ViewInfo;

import java.util.List;
import java.util.Map;

/**
 * Parses the view Information from the describe formatted output.
 */
public class ViewInfoParser extends AbstractTableMetaParser<ViewInfo>{

  public ViewInfoParser() {
    super("# View Information", null, "");
  }

  @Override
  public ViewInfo parse(List<Row> rows) {
    ViewInfo info = new ViewInfo();
    Map<String, Object> parsedSection = parseSection(rows);
    if(parsedSection.size() == 0) {
      return null; // View Information is not present
    }
    info.setOriginalText(getString(parsedSection, "View Original Text:"));
    info.setExtendedText(getString(parsedSection, "View Expanded Text:"));
    return info;
  }
}
