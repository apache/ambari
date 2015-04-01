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

App.Widget = DS.Model.extend({
  widgetName: DS.attr('string'),

  /**
   * types:
   *  - GAUGE (shown as a percentage dial)
   *  - HEATMAP
   *  - GRAPH (Line graph and stack graph)
   *  - NUMBER (e.g., “1 ms” for RPC latency)
   *  - LINKS
   *  - TEMPLATE
   */
  widgetType: DS.attr('string'),
  displayName: DS.attr('string'),
  serviceName: DS.attr('string'),
  componentName: DS.attr('string'),
  timeCreated: DS.attr('number'),
  sectionName: DS.attr('string'),
  author: DS.attr('string'),
  properties: DS.attr('object'),
  expression: DS.attr('array'),
  metrics: DS.attr('array'),
  values: DS.attr('array'),
  isVisible: DS.attr('boolean'),

  /**
   * @type {number}
   * @default 0
   */
  defaultOrder: 0, // This field is not derived from API but needs to be filled in the mapper on the client side

  /**
   * @type Em.View
   * @class
   */
  viewClass: function () {
    switch (this.get('widgetType')) {
      case 'GRAPH':
        return App.GraphWidgetView;
      case 'TEMPLATE':
        return App.TemplateWidgetView;
      case 'NUMBER':
        return App.NumberWidgetView;
      case 'GAUGE':
        return App.GaugeWidgetView;
      default:
        return Em.View;
    }
  }.property('widgetType')
});

App.WidgetType = DS.Model.extend({
  name: DS.attr('string'),
  displayName: DS.attr('string'),
  description: DS.attr('string'),
  properties: DS.attr('array')
});


App.Widget.FIXTURES = [];

App.WidgetType.FIXTURES = [
  {
    id: 1,
    name: 'GAUGE',
    display_name: 'Gauge',
    description: Em.I18n.t('widget.type.gauge.description'),
    properties: [
      {
        property_name : 'warning_threshold',
        isRequired: true   // This field is used to distinguish required properties from optional. This can be used for imposing client side validation
      },
      {
        property_name : 'error_threshold',
        isRequired: true
      }
    ]
  },
  {
    id: 2,
    name: 'NUMBER',
    display_name: 'Number',
    description: Em.I18n.t('widget.type.number.description'),
    properties: [
      {
        property_name : 'warning_threshold',
        display_name: 'warning',
        isRequired: false
      },
      {
        property_name : 'error_threshold',
        display_name: 'critical',
        isRequired: false
      },
      {
        property_name : 'display_unit',
        display_name: 'unit',
        isRequired: false
      }
    ]
  },
  {
    id: 3,
    name: 'GRAPH',
    display_name: 'Graph',
    description: Em.I18n.t('widget.type.graph.description'),
    properties: [
      {
        property_name : 'graph_type',
        isRequired: true
      },
      {
        property_name : 'time_range',
        isRequired: true
      }
    ]
  },
  {
    id: 4,
    name: 'TEMPLATE',
    display_name: 'Template',
    description: Em.I18n.t('widget.type.template.description'),
    properties: [
    ]
  }
];
