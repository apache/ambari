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

App.WidgetSectionMixin = Ember.Mixin.create({
  /**
   * UI default layout name
   */
  defaultLayoutName: function () {
    var heatmapType;
    if (this.get('content.serviceName')) {
      heatmapType = this.get('content.serviceName').toLowerCase();
    } else {
      heatmapType = "system";
    }
    return "default_" + heatmapType + this.layoutNameSuffix;
  }.property('content.serviceName'),

  /**
   * UI section name
   */
  sectionName: function () {
    if (this.get('content.serviceName')) {
      return this.get('content.serviceName') + this.sectionNameSuffix;
    } else {
      return "SYSTEM"  + this.sectionNameSuffix
    }
  }.property('content.serviceName'),



  /**
   * Does Service has widget descriptor defined in the stack
   * @type {boolean}
   */
  isServiceWithEnhancedWidgets: function () {
    var isServiceWithWidgetdescriptor;
    var serviceName = this.get('content.serviceName');
    if (serviceName) {
      isServiceWithWidgetdescriptor = App.StackService.find().findProperty('serviceName', serviceName).get('isServiceWithWidgets');
    } else if (this.get('sectionName') === 'SYSTEM_HEATMAPS') {
      isServiceWithWidgetdescriptor = true;
    }
    return isServiceWithWidgetdescriptor && App.supports.customizedWidgets;
  }.property('content.serviceName'),

  /**
   *  @Type {App.WidgetLayout}
   */
  activeWidgetLayout: {},

  /**
   * @type {Em.A}
   */
  widgets: function () {
    if (this.get('isWidgetsLoaded')) {
      if (this.get('activeWidgetLayout.widgets')) {
        return this.get('activeWidgetLayout.widgets').toArray();
      } else {
        return  [];
      }
    }
  }.property('isWidgetsLoaded'),

  /**
   * load widgets defined by user
   * @returns {$.ajax}
   */
  loadActiveWidgetLayout: function () {
    this.set('activeWidgetLayout', {});
    this.set('isWidgetsLoaded', false);
    if (this.get('isServiceWithEnhancedWidgets')) {
      return App.ajax.send({
        name: 'widget.layout.get',
        sender: this,
        data: {
          layoutName: this.get('defaultLayoutName'),
          serviceName: this.get('content.serviceName')
        },
        success: 'loadActiveWidgetLayoutSuccessCallback'
      });
    } else {
      this.set('isWidgetsLoaded', true);
    }
  },


  /**
   * success callback of <code>loadActiveWidgetLayout()</code>
   * @param {object|null} data
   */
  loadActiveWidgetLayoutSuccessCallback: function (data) {
    if (data.items[0]) {
      App.widgetMapper.map(data.items[0].WidgetLayoutInfo);
      App.widgetLayoutMapper.map(data);
      this.set('activeWidgetLayout', App.WidgetLayout.find().findProperty('layoutName', this.get('defaultLayoutName')));
      this.set('isWidgetsLoaded', true);
    }
  },

  /**
   * save layout after re-order widgets
   * return {$.ajax}
   */
  saveWidgetLayout: function (widgets) {
    var activeLayout = this.get('activeWidgetLayout');
    var data = {
      "WidgetLayoutInfo": {
        "display_name": activeLayout.get("displayName"),
        "id": activeLayout.get("id"),
        "layout_name": activeLayout.get("layoutName"),
        "scope": activeLayout.get("scope"),
        "section_name": activeLayout.get("sectionName"),
        "widgets": widgets.map(function (widget) {
          return {
            "id": widget.get('id')
          }
        })
      }
    };
    return App.ajax.send({
      name: 'widget.layout.edit',
      sender: this,
      data: {
        layoutId: activeLayout.get("id"),
        data: data
      }
    });
  }
});