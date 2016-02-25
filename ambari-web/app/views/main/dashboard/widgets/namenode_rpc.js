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

App.NameNodeRpcView = App.TextDashboardWidgetView.extend(App.EditableWidgetMixin, {

  title: Em.I18n.t('dashboard.widgets.NameNodeRpc'),
  id: '5',

  model_type: 'hdfs',
  hiddenInfo: function () {
    return [
      this.get('content') + ' average RPC',
      'queue wait time'
    ];
  }.property('content'),

  thresh1: 0.5,
  thresh2: 2,
  maxValue: 'infinity',

  isGreen: Em.computed.lteProperties('data', 'thresh1'),

  isRed: Em.computed.gtProperties('data', 'thresh2'),

  data: function () {
    if (this.get('model.nameNodeRpc')) {
      return (this.get('model.nameNodeRpc')).toFixed(2);
    }
    if (this.get('model.nameNodeRpc') == 0) {
      return 0;
    }
    return null;
  }.property('model.nameNodeRpc'),

  content: function () {
    if (this.get('data') || this.get('data') == 0) {
      return this.get('data') + " ms";
    }
    return Em.I18n.t('services.service.summary.notAvailable');
  }.property('model.nameNodeRpc'),

  hintInfo: Em.I18n.t('dashboard.widgets.hintInfo.hint3')

});
