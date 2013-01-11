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

App.MainDashboardView = Em.View.extend({
  templateName:require('templates/main/dashboard'),
  didInsertElement:function () {
    this.services();
  },
  content:[],
  updateServices: function(){
    var services = App.Service.find();
    services.forEach(function (item) {
      var view;
      switch (item.get('serviceName')) {
        case "HDFS":
          view = this.get('content').filterProperty('viewName', App.MainDashboardServiceHdfsView);
          view.objectAt(0).set('model', App.HDFSService.find(item.get('id')));
          break;
        case "MAPREDUCE":
          view = this.get('content').filterProperty('viewName', App.MainDashboardServiceMapreduceView);
          view.objectAt(0).set('model', App.MapReduceService.find(item.get('id')));
          break;
        case "HBASE":
          view = this.get('content').filterProperty('viewName', App.MainDashboardServiceHbaseView);
          view.objectAt(0).set('model', App.HBaseService.find(item.get('id')));
      }
    }, this);
  }.observes('App.router.updateController.isUpdate'),
  services: function () {
    var services = App.Service.find();
    if (this.get('content').length > 0) {
      return false
    }
    services.forEach(function (item) {
      var vName;
      var item2;
      switch (item.get('serviceName')) {
        case "HDFS":
          vName = App.MainDashboardServiceHdfsView;
          item2 = App.HDFSService.find(item.get('id'));
          break;
        case "MAPREDUCE":
          vName = App.MainDashboardServiceMapreduceView;
          item2 = App.MapReduceService.find(item.get('id'));
          break;
        case "HBASE":
          vName = App.MainDashboardServiceHbaseView;
          item2 = App.HBaseService.find(item.get('id'));
          break;
        case "HIVE":
          vName = App.MainDashboardServiceHiveView;
          break;
        case "ZOOKEEPER":
          vName = App.MainDashboardServiceZookeperView;
          break;
        case "OOZIE":
          vName = App.MainDashboardServiceOozieView;
          break;
        default:
          vName = Em.View;
      }
      this.get('content').pushObject(Em.Object.create({
        viewName: vName,
        model: item2 || item
      }))
    }, this);
  },

  gangliaUrl: function () {
    return App.router.get('clusterController.gangliaUrl') + "/?r=hour&cs=&ce=&m=&s=by+name&c=HDPSlaves&tab=m&vn=";
  }.property('App.router.clusterController.gangliaUrl'),

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
        templateName: require('templates/main/dashboard/alert_notification_popup')
      }),
      primary: 'Close',
      onPrimary: function() {
        this.hide();
      },
      secondary : null,
      didInsertElement: function () {
        this.$().find('.modal-footer').addClass('align-center');
        this.$().children('.modal').css({'margin-top': '-350px'});
      }
    });
    event.stopPropagation();
  }
});