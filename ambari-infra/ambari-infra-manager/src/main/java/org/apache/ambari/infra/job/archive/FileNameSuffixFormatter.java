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

import static java.util.Objects.requireNonNull;
import static org.apache.ambari.infra.job.archive.SolrDocumentIterator.SOLR_DATE_FORMAT_TEXT;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class FileNameSuffixFormatter {
  public static final DateTimeFormatter SOLR_DATETIME_FORMATTER = DateTimeFormatter.ofPattern(SOLR_DATE_FORMAT_TEXT);

  public static FileNameSuffixFormatter from(ArchivingProperties properties) {
    return new FileNameSuffixFormatter(properties.getFileNameSuffixColumn(), properties.getFileNameSuffixDateFormat());
  }


  private final String columnName;

  private final DateTimeFormatter dateFormat;

  public FileNameSuffixFormatter(String columnName, String dateTimeFormat) {
    this.columnName = columnName;
    dateFormat = isBlank(dateTimeFormat) ? null : DateTimeFormatter.ofPattern(dateTimeFormat);
  }

  public String format(Document document) {
    requireNonNull(document, "Can not format file name suffix: input document is null!");

    if (isBlank(document.getString(columnName)))
      throw new IllegalArgumentException("The specified document does not have a column " + columnName + " or it's value is blank!");

    return format(document.getString(columnName));
  }

  public String format(String value) {
    if (isBlank(value))
      throw new IllegalArgumentException("The specified value is blank!");

    if (dateFormat == null)
      return value;
    OffsetDateTime date = OffsetDateTime.parse(value, SOLR_DATETIME_FORMATTER);
    return date.format(dateFormat);
  }
}
