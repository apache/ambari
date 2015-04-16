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


App.widgetLayoutMapper = App.QuickDataMapper.create({
  widgetLayoutModel: App.WidgetLayout,
  widgetModel:  App.Widget,
  widgetLayoutConfig: {
    id: 'id',
    layout_name: 'layout_name',
    section_name: 'section_name',
    scope: 'scope',
    user: 'user_name',
    display_name: 'display_name',
    widgets_key: 'widgets',
    widgets_type: 'array',
    widgets: {
      item: 'id'
    }
  },
  widgetConfig: {
    id: 'id',
    widget_name: 'widget_name',
    default_order: 'default_order',
    widget_type: 'widget_type',
    display_name: 'display_name',
    time_created: 'time_created',
    author: 'author',
    properties: 'properties',
    metrics: 'metrics',
    values: 'values',
    description: 'description',
    scope: 'scope'
  },


  map: function(json) {
    var modelWidget = this.widgetModel;
    var modelWidgetLayout = this.widgetLayoutModel;

    var resultWidget = [];
    json.WidgetLayoutInfo.widgets.forEach(function(item, index) {
      item = item.WidgetInfo;
      item.metrics = JSON.parse(item.metrics);
      item.values = JSON.parse(item.values);
      resultWidget.push(this.parseIt(item, this.widgetConfig));
      json.WidgetLayoutInfo.widgets[index] = item;
    },this);

    var resultWidgetLayout = this.parseIt(json.WidgetLayoutInfo, this.widgetLayoutConfig);
    App.store.loadMany(modelWidget, resultWidget);
    App.store.load(modelWidgetLayout, resultWidgetLayout);
  }
});
