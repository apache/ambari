/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

App.ServiceRestartView = Em.View.extend({

  templateName: require('templates/common/service_restart'),

  useRolling: true,

  useExpress: false,

  didInsertElement: function() {
    this.set('useRolling', true);
    this.set('useExpress',  false);
  },

  rollingRestartCheckBox: App.RadioButtonView.extend({
    labelTranslate: 'common.rolling',
    checked: Em.computed.alias('parentView.useRolling'),
    click: function () {
      this.set('parentView.useRolling', true);
      this.set('parentView.useExpress', false);
    }
  }),

  expressRestartCheckBox: App.RadioButtonView.extend({
    labelTranslate: 'common.express',
    checked: Em.computed.alias('parentView.useExpress'),
    click: function () {
      this.set('parentView.useRolling', false);
      this.set('parentView.useExpress', true);
    }
  })

});