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

App.MainServiceItemController = Em.Controller.extend({
  name: 'mainServiceItemController',
  content: App.Service.find(1),
  showRebalancer: function() {
    if(this.content.get('serviceName') == 'hdfs') {
      return true;
    } else {
      return false;
    }
  }.property('content'),
  startConfirmPopup: function (event) {
    App.ModalPopup.show({
      header: Em.I18n.t('services.service.start.popup.header'),
      body: Em.I18n.t('services.service.start.popup.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        alert('do');
        this.hide();
      },
      onSecondary: function() {
        alert('not do');
        this.hide();
      }
    });
  },
  stopConfirmPopup: function (event) {
    App.ModalPopup.show({
      header: Em.I18n.t('services.service.stop.popup.header'),
      body: Em.I18n.t('services.service.stop.popup.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        alert('do');
        this.hide();
      },
      onSecondary: function() {
        alert('not do');
        this.hide();
      }
    });
  }
})