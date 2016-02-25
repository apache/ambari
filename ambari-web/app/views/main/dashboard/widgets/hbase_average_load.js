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

App.HBaseAverageLoadView = App.TextDashboardWidgetView.extend(App.EditableWidgetMixin, {

  title: Em.I18n.t('dashboard.widgets.HBaseAverageLoad'),
  id: '14',

  model_type: 'hbase',

  hiddenInfo: function () {
    var avgLoad = this.get('model.averageLoad');
    if (isNaN(avgLoad)) {
      avgLoad = Em.I18n.t('services.service.summary.notAvailable');
    }
    return [Em.I18n.t('dashboard.services.hbase.averageLoadPerServer').format(avgLoad)];
  }.property("model.averageLoad"),

  isGreen: Em.computed.lteProperties('data', 'thresh1'),
  isRed: Em.computed.gtProperties('data', 'thresh2'),

  isNA: function (){
    return this.get('data') === null || isNaN(this.get('data'));
  }.property('data'),

  thresh1: 0.5,
  thresh2: 2,
  maxValue: 'infinity',

  data: Em.computed.alias('model.averageLoad'),

  content: function (){
    if(this.get('data') || this.get('data') == 0){
      return this.get('data') + "";
    }
    return Em.I18n.t('services.service.summary.notAvailable');
  }.property('model.averageLoad'),

  hintInfo: Em.I18n.t('dashboard.widgets.hintInfo.hint2')

});
