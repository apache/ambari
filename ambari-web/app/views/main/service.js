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

App.MainServiceView = Em.View.extend({
  templateName:require('templates/main/service'),

  showAlertsPopup: function (event) {
    App.ModalPopup.show({
      header: this.t('services.alerts.headingOfList'),
      bodyClass: Ember.View.extend({
        service: event.context,
        warnAlerts: function () {
          var allAlerts = App.router.get('clusterController.alerts');
          var serviceId = this.get('service.serviceName');
          if (serviceId) {
            return allAlerts.filterProperty('serviceType', serviceId).filterProperty('isOk', false);
          }
          return 0;
        }.property('App.router.clusterController.alerts'),

        warnAlertsCount: function () {
          return this.get('warnAlerts').length;
        }.property('warnAlerts'),

        nagiosUrl: function () {
          return App.router.get('clusterController.nagiosUrl');
        }.property('App.router.clusterController.nagiosUrl'),

        closePopup: function () {
          this.get('parentView').hide();
        },

        selectService: function () {
          App.router.transitionTo('services.service.summary', event.context)
          this.closePopup();
        },
        templateName: require('templates/main/service/alert_notification_popup')
      }),
      primary: 'Close',
      onPrimary: function() {
        this.hide();
      },
      secondary : null,
      didInsertElement: function () {
        this.$().find('.modal-footer').addClass('align-center');
      }
    });
  }
});