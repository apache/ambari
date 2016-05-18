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

App.HBaseRegionsInTransitionView = App.TextDashboardWidgetView.extend(App.EditableWidgetMixin, {

  title: Em.I18n.t('dashboard.widgets.HBaseRegionsInTransition'),
  id: '15',

  model_type: 'hbase',
  hiddenInfo: function () {
    return [
      this.get("model.regionsInTransition") + " regions",
      "in transition"
    ];
  }.property("model.regionsInTransition"),

  classNameBindings: ['isRed', 'isOrange', 'isGreen', 'isNA'],
  isGreen: Em.computed.lteProperties('data', 'thresh1'),
  isRed: Em.computed.gtProperties('data', 'thresh2'),
  isOrange: Em.computed.and('!isGreen', '!isRed'),
  isNA: function () {
    return this.get('data') === null;
  }.property('data'),

  thresh1: 0.5,
  thresh2: 2,
  maxValue: 'infinity',

  data: Em.computed.alias('model.regionsInTransition'),

  content: Em.computed.format('{0}', 'data'),

  hintInfo: Em.I18n.t('dashboard.widgets.hintInfo.hint2')

});
