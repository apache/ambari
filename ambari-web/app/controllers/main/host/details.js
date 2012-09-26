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

App.MainHostDetailsController = Em.Controller.extend({
  name: 'mainHostDetailsController',
  content: null,
  startComponents: function(){
    return this.get('content.workStatus');
  }.property('content.workStatus'),
  stopComponents: function(){
    return !this.get('startComponents');
  }.property('startComponents'),
  changeWorkStatus: function(){
    if (this.get('startComponents')) {
      this.set('iconClass', 'play');
    } else {
      this.set('iconClass', 'stop');
    }
  }.observes('startComponents'),
  iconClass: '',

  startConfirmPopup: function (event) {
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('hosts.host.start.popup.header'),
      body: Em.I18n.t('hosts.host.start.popup.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        console.log(self.get('content.components').getEach('workStatus'));
        self.get('content.components').setEach('workStatus', self.get('content.workStatus'));

        self.set('content.workStatus', !self.get('content.workStatus'));

        this.hide();
      },
      onSecondary: function() {
        this.hide();
      }
    });
  },
  stopConfirmPopup: function (event) {
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('hosts.host.stop.popup.header'),
      body: Em.I18n.t('hosts.host.stop.popup.body'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function() {
        console.log(self.get('content.components').getEach('workStatus'));
        self.get('content.components').setEach('workStatus', self.get('content.workStatus'));
        self.set('content.workStatus', !self.get('content.workStatus'));
        this.hide();
      },
      onSecondary: function() {
        this.hide();
      }
    });
  }
})