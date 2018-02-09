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
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var App = require('app');

App.ServiceConfigLayoutTabView = Em.View.extend(App.ConfigOverridable, App.LoadingOverlaySupport, {

  /**
   * Determines if view is editable
   * It true - show all control-elements (undo, override, finalize etc) for each widget
   * If false - no widgets control-elements will be shown
   * Bound from template
   * @type {boolean}
   */
  canEdit: true,

  /**
   * view need some time to prepare data to display it correct
   * before that it's better not to show anything
   * @type {boolean}
   */
  dataIsReady: false,

  /**
   * @type {App.Service}
   */
  service: Em.computed.alias('controller.selectedService'),

  templateName: function() {
    var customTemplate = this.get('customTemplate');
    return customTemplate ? customTemplate : require('templates/common/configs/service_config_layout_tab');
  }.property('customTemplate'),

  customTemplate: null,

  fieldToObserve: 'controller.recommendationsInProgress',

  classNames: ['enhanced-config-tab-content'],
  /**
   * ConfigType-Widget map
   * key - widget type
   * value - widget view
   * @type {object}
   */
  widgetTypeMap: {
    checkbox: App.CheckboxConfigWidgetView,
    combo: App.ComboConfigWidgetView,
    directory: App.TextFieldConfigWidgetView,
    directories: App.DirectoryConfigWidgetView,
    list: App.ListConfigWidgetView,
    password: App.PasswordConfigWidgetView,
    'radio-buttons': App.RadioButtonConfigWidgetView,
    slider: App.SliderConfigWidgetView,
    'text-field': App.TextFieldConfigWidgetView,
    'time-interval-spinner': App.TimeIntervalSpinnerView,
    toggle: App.ToggleConfigWidgetView,
    'text-area': App.StringConfigWidgetView,
    'label': App.LabelView,
    'test-db-connection': App.TestDbConnectionWidgetView
  },

  configNameWidgetMixinMap: {
    num_llap_nodes: App.NumLlapNodesWidgetMixin
  },

  checkOverlay: function () {
    this.handleFieldChanges();
  }.observes('controller.activeTab.id', 'controller.activeTab.isRendered'),

  /**
   * Prepare configs for render
   * <code>subsection.configs</code> is an array of App.StackConfigProperty, but not App.ConfigProperty,
   * so proper config-properties should be linked to the subsections.
   * @method prepareConfigProperties
   */
  prepareConfigProperties: function () {
    var self = this;
    this.get('content.sectionRows').forEach(function (row) {
      row.forEach(function (section) {
        section.get('subsectionRows').forEach(function (subRow) {
          subRow.forEach(function (subsection) {
            self.setConfigsToContainer(subsection);
            subsection.get('subSectionTabs').forEach(function (subSectionTab) {
              self.setConfigsToContainer(subSectionTab);
            });
          });
        });
      });
    });
  },

  /**
   * set {code} configs {code} array of subsection or subsection tab.
   * Also correct widget should be used for each config (it's selected according to <code>widget.type</code> and
   * <code>widgetTypeMap</code>). It may throw an error if needed widget can't be found in the <code>widgetTypeMap</code>
   * @param containerObject
   */
  setConfigsToContainer: function(containerObject) {
    containerObject.set('configs', []);

    containerObject.get('configProperties').forEach(function (configId) {

      var config = App.configsCollection.getConfig(configId);
      var stepConfig = this.get('controller.stepConfigs').findProperty('serviceName', Em.get(config, 'serviceName'));
      if (!stepConfig) return;

      var configProperty = stepConfig.get('configs').findProperty('id', Em.get(config, 'id'));
      if (!configProperty) return;

      containerObject.get('configs').pushObject(configProperty);

      var widget = this.getWidgetView(config);
      Em.assert('Unknown config widget view for config ' + configProperty.get('id') + ' with type ' +  Em.get(config, 'widgetType'), widget);

      var additionalProperties = {
        widget: widget,
        stackConfigProperty: config
      };

      var configConditions = App.ThemeCondition.find().filter(function (_configCondition) {
        // Filter config condition depending on the value of another config
        var conditionalConfigs = _configCondition.getWithDefault('configs', []).filterProperty('fileName', Em.get(config,'filename')).filterProperty('configName', Em.get(config,'name'));
        // Filter config condition depending on the service existence or service state
        var serviceConfigConditionFlag = ((_configCondition.get('configName') === Em.get(config,'name')) &&  (_configCondition.get('fileName') === Em.get(config,'filename')) &&  (_configCondition.get('resource') === 'service'));
        var conditions;

        if (serviceConfigConditionFlag) {
          var configCondition = {
            configName: _configCondition.get('configName'),
            fileName: _configCondition.get('fileName')
          };
          conditions = conditionalConfigs.concat(configCondition)
        } else {
          conditions = conditionalConfigs;
        }
        return (conditions && conditions.length);
      }, this);

      if (configConditions && configConditions.length) {
        additionalProperties.configConditions = configConditions;
      }

      var configAction = App.ConfigAction.find().filterProperty('fileName', Em.get(config,'filename')).findProperty('configName', Em.get(config,'name'));

      if (configAction) {
        additionalProperties.configAction = configAction;
      }

      configProperty.setProperties(additionalProperties);

      if (configProperty.get('overrides')) {
        configProperty.get('overrides').setEach('stackConfigProperty', config);
      }
      if (configProperty.get('compareConfigs')) {
        configProperty.get('compareConfigs').invoke('setProperties', {
          isComparison: false,
          stackConfigProperty: config
        });
      }
    }, this);
  },

  /**
   *
   * @param {object} config
   * @returns {Em.View}
   */
  getWidgetView: function (config) {
    var configWidgetType = Em.get(config, 'widgetType');
    var name = Em.get(config, 'name');
    var mixin = this.get('configNameWidgetMixinMap')[name];
    var viewClass = this.get('widgetTypeMap')[configWidgetType];
    return Em.isNone(mixin) ? viewClass : viewClass.extend(mixin);
  },

  /**
   * changes active subsection tab
   * @param event
   */
  setActiveSubTab: function(event) {
    if (!event.context || !event.context.get('isVisible')) {
      return false;
    }
    try {
      event.context.get('subSection.subSectionTabs').setEach('isActive', false);
      event.context.set('isActive', true);
    } catch (e) {
      console.error('Can\'t update active subsection tab');
    }
  },

  didInsertElement: function () {
    this.set('dataIsReady', false);
    this.set('content.isConfigsPrepared', false);
    this._super();
    this.prepareConfigProperties();
    if (this.get('controller.isCompareMode')) {
      this.get('parentView').filterEnhancedConfigs();
    }
    this.set('content.isConfigsPrepared', true);
    this.set('dataIsReady', true);
    this._super(...arguments);
  }

});
