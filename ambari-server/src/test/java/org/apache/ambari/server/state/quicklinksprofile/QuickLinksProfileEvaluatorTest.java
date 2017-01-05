/*
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

package org.apache.ambari.server.state.quicklinksprofile;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.ambari.server.state.quicklinks.Link;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class QuickLinksProfileEvaluatorTest {

  static final String AUTHENTICATED = "authenticated";
  static final String SSO = "sso";
  static final String NAMENODE = "NAMENODE";
  static final String HDFS = "HDFS";
  static final String NAMENODE_UI = "namenode_ui";


  private Link namenodeUi;

  public QuickLinksProfileEvaluatorTest() {
    namenodeUi = new Link();
    namenodeUi.setComponentName(NAMENODE);
    namenodeUi.setName(NAMENODE_UI);
    namenodeUi.setAttributes(ImmutableList.of(AUTHENTICATED));
  }

  /**
   * Test to prove that {@link QuickLinksProfileEvaluator} can accept quicklink profiles with null values.
   */
  @Test
  public void testNullsAreAccepted() throws Exception {
    QuickLinksProfile profile = new QuickLinksProfile();
    QuickLinksProfileEvaluator evaluator = new QuickLinksProfileEvaluator(profile);
    assertFalse("Link should be hidden as there are no applicable filters", evaluator.isVisible(HDFS, namenodeUi));

    Service service = Service.create(HDFS, null, null);
    profile = QuickLinksProfile.create(null, ImmutableList.of(service));
    evaluator = new QuickLinksProfileEvaluator(profile);
    assertFalse("Link should be hidden as there are no applicable filters", evaluator.isVisible(HDFS, namenodeUi));
  }

  /**
   * Test to prove that {@link Link}'s with unset {@code componentName} fields are handled properly.
   */
  @Test
  public void testLinkWithNoComponentField() throws Exception {
    Component component = Component.create(NAMENODE,
        ImmutableList.<Filter>of(Filter.linkNameFilter(NAMENODE_UI, true)));

    Service service = Service.create(HDFS, ImmutableList.<Filter>of(), ImmutableList.of(component));

    QuickLinksProfile profile = QuickLinksProfile.create(ImmutableList.<Filter>of(), ImmutableList.of(service));
    QuickLinksProfileEvaluator evaluator = new QuickLinksProfileEvaluator(profile);
    namenodeUi.setComponentName(null);
    assertFalse("Link should be hidden as there are no applicable filters", evaluator.isVisible(HDFS, namenodeUi));
  }

  /**
   * Test to prove that component level filters are evaluated first.
   */
  @Test
  public void testComponentLevelFiltersEvaluatedFirst() throws Exception {
    Component component = Component.create(
        NAMENODE,
        ImmutableList.<Filter>of(Filter.linkAttributeFilter(AUTHENTICATED, true)));

    Service service = Service.create(
        HDFS,
        ImmutableList.<Filter>of(Filter.linkAttributeFilter(AUTHENTICATED, false)),
        ImmutableList.of(component));

    QuickLinksProfile profile = QuickLinksProfile.create(
        ImmutableList.<Filter>of(Filter.acceptAllFilter(false)),
        ImmutableList.of(service));

    QuickLinksProfileEvaluator evaluator = new QuickLinksProfileEvaluator(profile);
    assertTrue("Component level filter should have been applied.", evaluator.isVisible(HDFS, namenodeUi));
  }

  /**
   * Test to prove that service level filters are evaluated secondly.
   */
  @Test
  public void testServiceLevelFiltersEvaluatedSecondly() throws Exception {
    Component component = Component.create(NAMENODE,
        ImmutableList.<Filter>of(Filter.linkAttributeFilter(SSO, false)));

    Service service = Service.create(HDFS,
        ImmutableList.<Filter>of(Filter.linkAttributeFilter(AUTHENTICATED, true)),
        ImmutableList.of(component));

    QuickLinksProfile profile = QuickLinksProfile.create(
        ImmutableList.<Filter>of(Filter.acceptAllFilter(false)),
        ImmutableList.of(service));

    QuickLinksProfileEvaluator evaluator = new QuickLinksProfileEvaluator(profile);
    assertTrue("Component level filter should have been applied.", evaluator.isVisible(HDFS, namenodeUi));
  }

  /**
   * Test to prove that global filters are evaluated last.
   */
  @Test
  public void testGlobalFiltersEvaluatedLast() throws Exception {
    Component component = Component.create(NAMENODE,
        ImmutableList.<Filter>of(Filter.linkAttributeFilter(SSO, false)));

    Service service = Service.create(HDFS,
        ImmutableList.<Filter>of(Filter.linkAttributeFilter(SSO, false)),
        ImmutableList.of(component));

    QuickLinksProfile profile = QuickLinksProfile.create(
        ImmutableList.<Filter>of(Filter.acceptAllFilter(true)),
        ImmutableList.of(service));

    QuickLinksProfileEvaluator evaluator = new QuickLinksProfileEvaluator(profile);
    assertTrue("Global filter should have been applied.", evaluator.isVisible(HDFS, namenodeUi));
  }

  /**
   * Test to prove that the link is hidden if no filters apply.
   */
  @Test
  public void testNoMatchingRule() throws Exception {
    Component component1 = Component.create(NAMENODE,
        ImmutableList.<Filter>of(Filter.linkAttributeFilter(SSO, true)));

    Component component2 = Component.create("DATANODE",
        ImmutableList.<Filter>of(Filter.acceptAllFilter(true)));

    Service service1 = Service.create(HDFS,
        ImmutableList.<Filter>of(Filter.linkAttributeFilter(SSO, true)),
        ImmutableList.of(component1, component2));

    Service service2 = Service.create("YARN",
        ImmutableList.<Filter>of(Filter.acceptAllFilter(true)),
        ImmutableList.<Component>of());

    QuickLinksProfile profile = QuickLinksProfile.create(
        ImmutableList.<Filter>of(Filter.linkAttributeFilter(SSO, true)),
        ImmutableList.of(service1, service2));

    QuickLinksProfileEvaluator evaluator = new QuickLinksProfileEvaluator(profile);
    assertFalse("No filters should have been applied, so default false should have been returned.",
        evaluator.isVisible(HDFS, namenodeUi));
  }

}