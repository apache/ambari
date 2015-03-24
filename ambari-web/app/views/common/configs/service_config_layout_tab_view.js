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

App.ServiceConfigLayoutTabView = Em.View.extend({

  templateName: require('templates/common/configs/service_config_layout_tab'),

  classNames: ['enhanced-config-tab-content'],
  /**
   * ConfigType-Widget map
   * key - widget type
   * value - widget view
   * @type {object}
   */
  widgetTypeMap: {
    slider: App.SliderConfigWidgetView
  },

  /**
   * Prepare configs for render
   * <code>subsection.configs</code> is an array of App.StackConfigProperty, but not App.ConfigProperty,
   * so proper config-properties should be linked to the subsections.
   * Also correct widget should be used for each config (it's selected according to <code>widget.type</code> and
   * <code>widgetTypeMap</code>). It may throw an error if needed widget can't be found in the <code>widgetTypeMap</code>
   * @method prepareConfigProperties
   */
  prepareConfigProperties: function () {
    var widgetTypeMap = this.get('widgetTypeMap');
    var self = this;
    this.get('content.sectionRows').forEach(function (row) {
      row.forEach(function (section) {
        section.get('subsectionRows').forEach(function (subRow) {
          subRow.forEach(function (subsection) {
            subsection.set('configs', []);
            subsection.get('configProperties').forEach(function (config) {
              var c = App.ConfigProperty.find(config.get('id') + '_' + self.get('controller.selectedVersion'));
              subsection.get('configs').pushObject(c);
              var configWidgetType = config.get('widget.type');
              var widget = widgetTypeMap[configWidgetType];
              Em.assert('Unknown config widget view for config ' + c.get('id') + ' with type ' + configWidgetType, widget);
              c.set('widget', widget);
            });
          });
        });
      });
    });
  },

  willInsertElement: function () {
    this._super();
    this.prepareConfigProperties();
  }

});
