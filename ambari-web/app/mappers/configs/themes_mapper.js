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
  subSectionTabModel: App.SubSectionTab,
  themeConditionModel: App.ThemeCondition,

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
    "section_id": "section_id",
    "depends_on": "depends-on",
    "left_vertical_splitter": "left-vertical-splitter"
  },

  subSectionTabConfig: {
    "id": "name",
    "name": "name",
    "display_name": "display-name",
    "depends_on": "depends-on",
    "sub_section_id": "sub_section_id"
  },

  map: function (json) {
    console.time('App.themesMapper execution time');
    var tabs = [];
    json.items.forEach(function(item) {
      this.mapThemeLayouts(item, tabs);
      this.mapThemeConfigs(item);
      this.mapThemeWidgets(item);
    }, this);

    App.store.commit();
    App.store.loadMany(this.get("tabModel"), tabs);
    App.store.commit();
    console.timeEnd('App.themesMapper execution time');
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
                var subSectionConditions = [];
                section.subsections.forEach(function(subSection) {
                  var parsedSubSection = this.parseIt(subSection, this.get("subSectionConfig"));
                  parsedSubSection.section_id = parsedSection.id;

                  if (subSection['subsection-tabs']) {
                    var subSectionTabs = [];
                    var subSectionTabConditions = [];

                    subSection['subsection-tabs'].forEach(function (subSectionTab) {
                      var parsedSubSectionTab = this.parseIt(subSectionTab, this.get("subSectionTabConfig"));
                      parsedSubSectionTab.sub_section_id = parsedSubSection.id;
                      if (parsedSubSectionTab['depends_on']) {
                        subSectionTabConditions.push(parsedSubSectionTab);
                      }
                      subSectionTabs.push(parsedSubSectionTab);
                    }, this);
                    subSectionTabs[0].is_active = true;
                    if (subSectionTabConditions.length) {
                      var type = 'subsectionTab';
                      this.mapThemeConditions(subSectionTabConditions, type);
                    }
                    App.store.loadMany(this.get("subSectionTabModel"), subSectionTabs);
                    parsedSubSection.sub_section_tabs = subSectionTabs.mapProperty("id");
                  }
                  if (parsedSubSection['depends_on']) {
                    subSectionConditions.push(parsedSubSection);
                  }
                  subSections.push(parsedSubSection);
                }, this);
                if (subSectionConditions.length) {
                  var type = 'subsection';
                  this.mapThemeConditions(subSectionConditions, type);
                }
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
    var serviceName = Em.get(json, "ThemeInfo.service_name");
    Em.getWithDefault(json, "ThemeInfo.theme_data.Theme.configuration.placement.configs", []).forEach(function(configLink) {
      var configId = this.getConfigId(configLink);
      var subSectionId = configLink["subsection-name"];
      var subSectionTabId = configLink["subsection-tab-name"];
      if (subSectionTabId) {
        var subSectionTab = App.SubSectionTab.find(subSectionTabId);
      } else if (subSectionId) {
        var subSection = App.SubSection.find(subSectionId);
      }
      var configProperty = App.StackConfigProperty.find(configId);

      var dependsOnConfigs = configLink["depends-on"] || [];

      if (configProperty.get('id') && subSection) {
        subSection.get('configProperties').pushObject(configProperty);
        configProperty.set('subSection', subSection);
      } else if (configProperty.get('id') && subSectionTab) {
        subSectionTab.get('configProperties').pushObject(configProperty);
        configProperty.set('subSectionTab', subSectionTab);
      } else {
        console.log('there is no such property: ' + configId + '. Or subsection: ' + subSectionId);
        var valueAttributes = configLink["property_value_attributes"];
        if (valueAttributes) {
          var isUiOnlyProperty = valueAttributes["ui_only_property"];
          // UI only configs are mentioned in the themes for supporting widgets that is not intended for setting a value
          // And thus is affiliated with fake config property termed as ui only config property
          if (isUiOnlyProperty && subSection) {
            var split = configLink.config.split("/");
            var fileName =  split[0] + '.xml';
            var configName = split[1];
            var uiOnlyConfig = App.uiOnlyConfigDerivedFromTheme.filterProperty('filename', fileName).findProperty('name', configName);
            if (!uiOnlyConfig) {
              var coreObject = {
                id: configName + '_' + split[0],
                isRequiredByAgent: false,
                showLabel: false,
                isOverridable: false,
                recommendedValue: true,
                name: configName,
                isUserProperty: false,
                filename: fileName,
                serviceName: serviceName,
                subSection: subSection
              };
              var uiOnlyConfigDerivedFromTheme = Em.Object.create(App.config.createDefaultConfig(configName, serviceName, fileName, false, coreObject));
              App.uiOnlyConfigDerivedFromTheme.pushObject(uiOnlyConfigDerivedFromTheme);
            }
          }
        }
      }

      // map all the configs which conditionally affect the value attributes of a config
      if (dependsOnConfigs && dependsOnConfigs.length) {
        this.mapThemeConfigConditions(dependsOnConfigs, uiOnlyConfigDerivedFromTheme || configProperty);
      }

    }, this);
  },

  /**
   *
   * @param configConditions: Array
   * @param configProperty: DS.Model Object (App.StackConfigProperty)
   */
  mapThemeConfigConditions: function(configConditions, configProperty) {
    var configConditionsCopy = [];
    configConditions.forEach(function(_configCondition, index){
      var configCondition = $.extend({},_configCondition);
      configCondition.id = configProperty.get('id') + '_' + index;
      configCondition.config_name =  configProperty.get('name');
      configCondition.file_name =  configProperty.get('filename');
      if (_configCondition.configs && _configCondition.configs.length) {
        configCondition.configs = _configCondition.configs.map(function (item) {
          var result = {};
          result.fileName = item.split('/')[0] + '.xml';
          result.configName = item.split('/')[1];
          return result;
        });
      }

      configCondition.resource = _configCondition.resource || 'config';
      configCondition.type = _configCondition.type || 'config';

      configConditionsCopy.pushObject(configCondition);
    }, this);

    App.store.loadMany(this.get("themeConditionModel"), configConditionsCopy);
    App.store.commit();
  },

  /**
   *
   * @param subSections: Array
   * @param type: {String} possible values: `subsection` or `subsectionTab`
   */
  mapThemeConditions: function(subSections, type) {
    var subSectionConditionsCopy = [];
    subSections.forEach(function(_subSection){
      var subSectionConditions = _subSection['depends_on'];
      subSectionConditions.forEach(function(_subSectionCondition, index){
        var subSectionCondition = $.extend({},_subSectionCondition);
        subSectionCondition.id = _subSection.id + '_' + index;
        subSectionCondition.name = _subSection.name;
        if (_subSectionCondition.configs && _subSectionCondition.configs.length) {
          subSectionCondition.configs = _subSectionCondition.configs.map(function (item) {
            var result = {};
            result.fileName = item.split('/')[0] + '.xml';
            result.configName = item.split('/')[1];
            return result;
          });
        }

        subSectionCondition.resource = _subSectionCondition.resource || 'config';
        subSectionCondition.type = _subSectionCondition.type || type;
        subSectionConditionsCopy.pushObject(subSectionCondition);
      }, this);
    }, this);
    App.store.loadMany(this.get("themeConditionModel"), subSectionConditionsCopy);
    App.store.commit();
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

      if (configProperty.get('id')) {
        configProperty.set('widget', widget.widget);
        configProperty.set('widgetType', Em.get(widget, 'widget.type'));
      } else {
        var split = widget.config.split("/");
        var fileName =  split[0] + '.xml';
        var configName = split[1];
        var uiOnlyProperty = App.uiOnlyConfigDerivedFromTheme.filterProperty('filename',fileName).findProperty('name',configName);
        if (uiOnlyProperty) {
          uiOnlyProperty.set('widget', widget.widget);
          uiOnlyProperty.set('widgetType', Em.get(widget, 'widget.type'));
        } else {
          console.warn('there is no such property: ' + configId);
        }
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
