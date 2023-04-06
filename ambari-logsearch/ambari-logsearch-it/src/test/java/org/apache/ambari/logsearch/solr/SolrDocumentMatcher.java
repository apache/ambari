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
package org.apache.ambari.logsearch.solr;

import org.apache.solr.common.SolrDocument;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class SolrDocumentMatcher extends TypeSafeDiagnosingMatcher<SolrDocument> {

  public static SolrDocumentMatcher solrDoc(String expectedId) {
    return new SolrDocumentMatcher(expectedId);
  }

  private final String expectedId;

  private SolrDocumentMatcher(String expectedId) {
    this.expectedId = expectedId;
  }

  @Override
  protected boolean matchesSafely(SolrDocument item, Description mismatchDescription) {
    if (!item.containsKey("id")) {
      mismatchDescription.appendText("SolrDocument without 'id' field.");
      return false;
    }
    if (!expectedId.equals(item.get("id"))) {
      describe(mismatchDescription, item.get("id"));
      return false;
    }
    return true;
  }

  @Override
  public void describeTo(Description description) {
    describe(description, expectedId);
  }

  private void describe(Description description, Object id) {
    description.appendText("SolrDocument[id=");
    description.appendValue(id);
    description.appendText("]");
  }
}
