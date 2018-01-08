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

import org.apache.ambari.infra.job.CloseableIterator;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimeZone;

public class SolrDocumentIterator implements CloseableIterator<Document> {

  public static final String SOLR_DATE_FORMAT_TEXT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
  private static final DateFormat SOLR_DATE_FORMAT = new SimpleDateFormat(SOLR_DATE_FORMAT_TEXT);

  static {
    SOLR_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  private final Iterator<SolrDocument> documentIterator;
  private final CloudSolrClient client;


  public SolrDocumentIterator(QueryResponse response, CloudSolrClient client) {
    documentIterator = response.getResults().iterator();
    this.client = client;
  }

  @Override
  public Document next() {
    if (!documentIterator.hasNext())
      return null;
    
    SolrDocument document = documentIterator.next();
    HashMap<String, String> fieldMap = new HashMap<>();
    for (String key : document.getFieldNames()) {
      fieldMap.put(key, toString(document.get(key)));
    }

    return new Document(fieldMap);
  }

  private String toString(Object value) {
    if (value == null) {
      return null;
    }
    else if (value instanceof Date) {
      return SOLR_DATE_FORMAT.format(value);
    }
    else {
      return value.toString();
    }
  }

  @Override
  public void close() {
    try {
      client.close();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public boolean hasNext() {
    return documentIterator.hasNext();
  }
}
