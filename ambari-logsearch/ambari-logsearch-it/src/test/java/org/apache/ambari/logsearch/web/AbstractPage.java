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
package org.apache.ambari.logsearch.web;

import org.jbehave.web.selenium.WebDriverPage;
import org.jbehave.web.selenium.WebDriverProvider;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public abstract class AbstractPage extends WebDriverPage {

  public AbstractPage(WebDriverProvider driverProvider) {
    super(driverProvider);
  }

  public void found(String text) {
    found(getPageSource(), text);
  }

  public void found(String pageSource, String text) {
    if (!pageSource.contains(escapeHtml(text))) {
      fail("Text: '" + text + "' not found in page '" + pageSource + "'");
    }
  }

  public void found(List<String> texts) {
    for (String text : texts) {
      found(text);
    }
  }

  public void notFound(String text) {
    notFound(getPageSource(), text);
  }

  public void notFound(String pageSource, String text) {
    assertThat(pageSource.contains(escapeHtml(text)), is(false));
  }

  private String escapeHtml(String text) {
    return text.replace("<", "&lt;").replace(">", "&gt;");
  }
}
