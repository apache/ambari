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

package org.apache.ambari.server.state.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ThemeMergeTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String PARENT_THEME = "{"
    + "\"name\":\"parent\","
    + "\"configuration\":{"
    + "\"layouts\":["
    + "{\"name\":\"base\",\"tabs\":["
    + "{\"name\":\"primary\",\"display-name\":\"Primary\",\"layout\":{\"tab-rows\":\"2\",\"tab-columns\":\"2\",\"sections\":["
    + "{\"name\":\"primary-section\",\"display-name\":\"Primary Section\",\"row-index\":\"0\",\"subsections\":["
    + "{\"name\":\"primary-sub\",\"display-name\":\"Primary Subsection\",\"row-index\":\"0\",\"column-index\":\"0\"},"
    + "{\"name\":\"trailing-sub\",\"row-index\":\"1\",\"column-index\":\"0\"}]},"
    + "{\"name\":\"trailing-section\",\"row-index\":\"1\",\"subsections\":[{\"name\":\"other-sub\",\"row-index\":\"0\"}]}]}},"
    + "{\"name\":\"trailing-tab\",\"display-name\":\"Trailing\",\"layout\":{\"tab-rows\":\"1\"}}]},"
    + "{\"name\":\"trailing-layout\",\"tabs\":[{\"name\":\"only-tab\",\"display-name\":\"Only\",\"layout\":{\"tab-rows\":\"1\"}}]}],"
    + "\"placement\":{\"configuration-layout\":\"base\",\"configs\":["
    + "{\"config\":\"site/replace\",\"subsection-name\":\"primary-sub\",\"subsection-tab-name\":\"old-tab\",\"property_value_attributes\":{\"read_only\":false},\"depends-on\":[{\"resource\":\"config\",\"configs\":[\"site/dependency\"],\"if\":\"isEmpty()\"}]},"
    + "{\"config\":\"site/remove\",\"subsection-name\":\"trailing-sub\"}]},"
    + "\"widgets\":["
    + "{\"config\":\"site/replace\",\"widget\":{\"type\":\"slider\",\"units\":[{\"unit-name\":\"MB\"}]}},"
    + "{\"config\":\"site/remove\",\"widget\":{\"type\":\"checkbox\"}}]}}";

  private static final String ADDITIONS_THEME = "{"
    + "\"name\":\"child\",\"configuration\":{"
    + "\"layouts\":["
    + "{\"name\":\"base\",\"tabs\":["
    + "{\"name\":\"primary\",\"layout\":{\"sections\":["
    + "{\"name\":\"primary-section\",\"subsections\":["
    + "{\"name\":\"primary-sub\",\"display-name\":\"Updated Subsection\",\"depends-on\":[{\"configs\":[\"site/replace\"],\"if\":\"isEmpty()\"}],\"subsection-tabs\":[{\"name\":\"details\",\"display-name\":\"Details\"}]},"
    + "{\"name\":\"child-sub\",\"row-index\":\"2\",\"column-index\":\"0\"}]},"
    + "{\"name\":\"child-section\",\"row-index\":\"2\",\"subsections\":[{\"name\":\"child-section-sub\",\"row-index\":\"0\"}]}]}},"
    + "{\"name\":\"child-tab\",\"display-name\":\"Child Tab\",\"layout\":{\"tab-rows\":\"1\"}}]},"
    + "{\"name\":\"child-layout\",\"tabs\":[{\"name\":\"new-tab\",\"display-name\":\"New\",\"layout\":{\"tab-rows\":\"1\"}}]}],"
    + "\"placement\":{\"configs\":[{\"config\":\"site/new\",\"subsection-name\":\"child-sub\"}]},"
    + "\"widgets\":[{\"config\":\"site/new\",\"widget\":{\"type\":\"text-field\"}}]}}";

  private static final String REMOVALS_THEME = "{"
    + "\"configuration\":{"
    + "\"layouts\":["
    + "{\"name\":\"base\",\"tabs\":["
    + "{\"name\":\"primary\",\"layout\":{\"sections\":["
    + "{\"name\":\"primary-section\",\"subsections\":[{\"name\":\"trailing-sub\"}]},"
    + "{\"name\":\"trailing-section\"}]}},"
    + "{\"name\":\"trailing-tab\"}]},"
    + "{\"name\":\"trailing-layout\"}],"
    + "\"placement\":{\"configs\":["
    + "{\"config\":\"site/replace\",\"subsection-name\":\"updated-sub\",\"property_value_attributes\":{\"read_only\":true},\"depends-on\":[{\"resource\":\"service\",\"configs\":[\"HDFS\"],\"if\":\"isInstalled()\"}]},"
    + "{\"config\":\"site/remove\"},"
    + "{\"config\":\"site/add\",\"subsection-name\":\"primary-sub\"}]},"
    + "\"widgets\":["
    + "{\"config\":\"site/replace\",\"widget\":{\"type\":\"checkbox\",\"display-name\":\"Replacement\"}},"
    + "{\"config\":\"site/remove\"},"
    + "{\"config\":\"site/add\",\"widget\":{\"type\":\"text-field\"}}]}}";

  @Test
  public void testNewEntriesPreserveDeclarationOrderAtEveryLevel() throws Exception {
    Theme parent = theme(PARENT_THEME);
    Theme child = theme(ADDITIONS_THEME);

    child.mergeWithParent(parent);

    ThemeConfiguration configuration = child.getThemeConfiguration();
    assertEquals(Arrays.asList("base", "trailing-layout", "child-layout"),
      configuration.getLayouts().stream().map(Layout::getName).collect(Collectors.toList()));

    Layout base = configuration.getLayouts().get(0);
    assertEquals(Arrays.asList("primary", "trailing-tab", "child-tab"),
      base.getTabs().stream().map(Tab::getName).collect(Collectors.toList()));

    TabLayout primary = base.getTabs().get(0).getTabLayout();
    assertEquals("2", primary.getTabRows());
    assertEquals("2", primary.getTabColumns());
    assertEquals(Arrays.asList("primary-section", "trailing-section", "child-section"),
      primary.getSections().stream().map(Section::getName).collect(Collectors.toList()));

    Section primarySection = primary.getSections().get(0);
    assertEquals(Arrays.asList("primary-sub", "trailing-sub", "child-sub"),
      primarySection.getSubsections().stream().map(Subsection::getName).collect(Collectors.toList()));

    Subsection overridden = primarySection.getSubsections().get(0);
    assertEquals("Updated Subsection", overridden.getDisplayName());
    assertEquals("0", overridden.getRowIndex());
    assertEquals("0", overridden.getColumnIndex());
    assertEquals(1, overridden.getDependsOn().size());
    assertEquals(1, overridden.getSubsectionTabs().size());

    assertEquals(Arrays.asList("site/replace", "site/remove", "site/new"),
      configuration.getPlacement().getConfigs().stream().map(ConfigPlacement::getConfig).collect(Collectors.toList()));
    assertEquals(Arrays.asList("site/replace", "site/remove", "site/new"),
      configuration.getWidgets().stream().map(WidgetEntry::getConfig).collect(Collectors.toList()));
  }

  @Test
  public void testTypeSpecificRemovalsAndCompleteReplacements() throws Exception {
    Theme parent = theme(PARENT_THEME);
    Theme child = theme(REMOVALS_THEME);

    child.mergeWithParent(parent);

    ThemeConfiguration configuration = child.getThemeConfiguration();
    assertEquals(Arrays.asList("base"),
      configuration.getLayouts().stream().map(Layout::getName).collect(Collectors.toList()));

    List<Tab> tabs = configuration.getLayouts().get(0).getTabs();
    assertEquals(Arrays.asList("primary"), tabs.stream().map(Tab::getName).collect(Collectors.toList()));
    List<Section> sections = tabs.get(0).getTabLayout().getSections();
    assertEquals(Arrays.asList("primary-section"),
      sections.stream().map(Section::getName).collect(Collectors.toList()));
    assertEquals(Arrays.asList("primary-sub"),
      sections.get(0).getSubsections().stream().map(Subsection::getName).collect(Collectors.toList()));

    List<ConfigPlacement> placements = configuration.getPlacement().getConfigs();
    assertEquals(Arrays.asList("site/replace", "site/add"),
      placements.stream().map(ConfigPlacement::getConfig).collect(Collectors.toList()));
    ConfigPlacement replacement = placements.get(0);
    assertEquals("updated-sub", replacement.getSubsectionName());
    assertNull(replacement.getSubsectionTabName());
    assertTrue(replacement.getPropertyValueAttributes().getReadOnly());
    assertEquals("service", replacement.getDependsOn().get(0).getResource());

    List<WidgetEntry> widgets = configuration.getWidgets();
    assertEquals(Arrays.asList("site/replace", "site/add"),
      widgets.stream().map(WidgetEntry::getConfig).collect(Collectors.toList()));
    assertEquals("checkbox", widgets.get(0).getWidget().getType());
    assertEquals("Replacement", widgets.get(0).getWidget().getDisplayName());
    assertNull(widgets.get(0).getWidget().getUnits());
  }

  private Theme theme(String json) throws Exception {
    return MAPPER.readValue(json, Theme.class);
  }
}
