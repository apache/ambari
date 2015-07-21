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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var App = require("app");

App.themesMapper = App.QuickDataMapper.create({
  tabModel: App.Tab,
  sectionModel: App.Section,
  subSectionModel: App.SubSection,

  tabConfig: {
    "id": "name",
    "name": "name",
    "display_name": "display-name",
    "columns": "layout.tab-columns",
    "rows": "layout.tab-rows",
    "service_name": "service_name",
    "sections_key": "sections"
  },

  sectionConfig: {
    "id": "name",
    "name": "name",
    "display_name": "display-name",
    "row_index": "row-index",
    "column_index": "column-index",
    "row_span": "row-span",
    "column_span": "column-span",
    "section_columns": "section-columns",
    "section_rows": "section-rows",
    "tab_id": "tab_id"
  },

  subSectionConfig: {
    "id": "name",
    "name": "name",
    "display_name": "display-name",
    "border": "border",
    "row_index": "row-index",
    "column_index": "column-index",
    "column_span": "column-span",
    "row_span": "row-span",
    "configProperties": "config_properties",
    "section_id": "section_id"
  },

  map: function (json) {
    var tabs = [];
    json.items.forEach(function(item) {
      this.mapThemeLayouts(item, tabs);
      this.mapThemeConfigs(item);
      this.mapThemeWidgets(item);
    }, this);

    App.store.loadMany(this.get("tabModel"), tabs);
    App.store.commit();
  },

  /**
   * Bootstrap tab objects and link sections with subsections.
   *
   * @param {Object} json - json to parse
   * @param {Object[]} tabs - tabs list
   */
  mapThemeLayouts: function(json, tabs) {
     var serviceName = Em.get(json, "ThemeInfo.service_name");
     Em.getWithDefault(json, "ThemeInfo.theme_data.Theme.configuration.layouts", []).forEach(function(layout) {
      if (layout.tabs) {
        layout.tabs.forEach(function(tab) {
          var parsedTab = this.parseIt(tab, this.get("tabConfig"));
          parsedTab.id = serviceName + "_" + tab.name;
          parsedTab.service_name = serviceName;

          if (Em.get(tab, "layout.sections")) {
            var sections = [];
            Em.get(tab, "layout.sections").forEach(function(section) {
              var parsedSection = this.parseIt(section, this.get("sectionConfig"));
              parsedSection.tab_id = parsedTab.id;

              if (section.subsections) {
                var subSections = [];
                section.subsections.forEach(function(subSection) {
                  var parsedSubSection = this.parseIt(subSection, this.get("subSectionConfig"));
                  parsedSubSection.section_id = parsedSection.id;

                  subSections.push(parsedSubSection);
                }, this);
                App.store.loadMany(this.get("subSectionModel"), subSections);
                parsedSection.sub_sections = subSections.mapProperty("id");
              }

              sections.push(parsedSection);
            }, this);

            App.store.loadMany(this.get("sectionModel"), sections);
            parsedTab.sections = sections.mapProperty("id");
          }

          tabs.push(parsedTab);
        }, this);
      }

    }, this);
  },

  /**
   * create tie between <code>stackConfigProperty<code> and <code>subSection<code>
   *
   * @param {Object} json - json to parse
   */
  mapThemeConfigs: function(json) {
    Em.getWithDefault(json, "ThemeInfo.theme_data.Theme.configuration.placement.configs", []).forEach(function(configLink) {
      var configId = this.getConfigId(configLink);
      var subSectionId = configLink["subsection-name"];
      var subSection = App.SubSection.find(subSectionId);
      var configProperty = App.StackConfigProperty.find(configId);

      if (configProperty && subSection) {
        subSection.get('configProperties').pushObject(configProperty);
        configProperty.set('subSection', subSection);
      } else {
        console.warn('there is no such property: ' + configId + '. Or subsection: ' + subSectionId);
      }
    }, this);
  },

  /**
   * add widget object to <code>stackConfigProperty<code>
   *
   * @param {Object} json - json to parse
   */
  mapThemeWidgets: function(json) {
    Em.getWithDefault(json, "ThemeInfo.theme_data.Theme.configuration.widgets", []).forEach(function(widget) {
      var configId = this.getConfigId(widget);
      var configProperty = App.StackConfigProperty.find(configId);

      if (configProperty) {
        configProperty.set('widget', widget.widget);
      } else {
        console.warn('there is no such property: ' + configId);
      }
    }, this);
  },

  /**
   * transform info from json to config id
   * @param {Object} json
   * @returns {string|null}
   */
  getConfigId: function(json) {
    if (json && json.config && typeof json.config === "string") {
      var split = json.config.split("/");
      return App.config.configId(split[1], split[0]);
    } else {
      console.warn('getConfigId: invalid input data');
      return null;
    }
  },

  /**
   * generates Advanced tabs for selected or all services
   * @param {[string]} [serviceNames=null]
   */
  generateAdvancedTabs: function(serviceNames) {
    serviceNames = Em.isArray(serviceNames) ? serviceNames : App.StackService.find().mapProperty('serviceName');
    var advancedTabs = [];
    serviceNames.forEach(function(serviceName) {
      advancedTabs.pushObject({
        id: serviceName + '_advanced',
        name: 'advanced',
        display_name: 'Advanced',
        is_advanced: true,
        service_name: serviceName
      });
    });
    App.store.commit();
    App.store.loadMany(this.get("tabModel"), advancedTabs);
    App.store.commit();
  }
});
