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
var db = require('utils/db');

App.InstallerStep4Controller = Em.ArrayController.extend({
  name: 'installerStep4Controller',
  rawContent: require('data/mock/services'),
  content: [],

  isAll: function () {
    return this.everyProperty('isSelected', true);
  }.property('@each.isSelected'),

  isMinimum: function () {
    return this.filterProperty('isDisabled', false).everyProperty('isSelected', false);
  }.property('@each.isSelected'),

  checkDependencies: function () {
    var hbase = this.findProperty('serviceName', 'HBASE');
    var zookeeper = this.findProperty('serviceName', 'ZOOKEEPER');
    if (hbase && zookeeper) {
      zookeeper.set('isSelected', hbase.get('isSelected'));
    }
    var hive = this.findProperty('serviceName', 'HIVE');
    var hcatalog = this.findProperty('serviceName', 'HCATALOG');
    if (hive && hcatalog) {
      hcatalog.set('isSelected', hive.get('isSelected'));
    }
  }.observes('@each.isSelected'),

  navigateStep: function () {
    if (App.router.get('isFwdNavigation') === true && !App.router.get('backBtnForHigherStep')) {
      this.loadStepFromContent();
    } else {
      this.loadStepFromDb();
    }
    App.router.set('backBtnForHigherStep', false);
  },

  loadStepFromContent: function () {
    console.log("TRACE: Loading from rawContent/API step4: Choose Services");
    this.clear();
    var rawContent = this.rawContent;
    rawContent.setEach('isSelected', true);
    this.renderStep(rawContent);
  },

  loadStepFromDb: function () {
    console.log("TRACE: Loading form localStorage step4: Choose Services");
    this.clear();
    var rawContent = this.rawContent;
    rawContent.setEach('isSelected', false);
    var services = App.db.getSelectedServiceNames();
    if (services !== undefined && services !== null) {
      rawContent.forEach(function (_content) {
        var serviceFound = services.contains(_content.serviceName);
        if (serviceFound) {
          _content.isSelected = true;
        } else {
          _content.isSelected = false;
        }
        console.log('TRACE: value of service is: ' + _content.serviceName);
      });
      this.renderStep(rawContent);
    }
  },

  renderStep: function (rawContent) {
    rawContent.forEach(function (item) {
      this.pushObject(Ember.Object.create(item));
    }, this);
  },

  selectAll: function () {
    this.setEach('isSelected', true);
  },

  selectMinimum: function () {
    this.filterProperty('isDisabled', false).setEach('isSelected', false);
  },

  saveSelectedServiceNamesToDB: function () {
    var serviceNames = [];
    this.filterProperty('isSelected', true).forEach(function (item) {
      serviceNames.push(item.serviceName);
    });
    db.setSelectedServiceNames(serviceNames);
  },

  needToAddMapReduce: function () {
    if (this.findProperty('serviceName', 'MAPREDUCE').get('isSelected') === false) {
      var mapreduceDependentServices = this.filter(function (item) {
        return ['PIG', 'OOZIE', 'HIVE'].contains(item.get('serviceName')) && item.get('isSelected', true);
      });
      return (mapreduceDependentServices.get('length') > 0);
    } else {
      return false;
    }
  },

  gangliaOrNagiosNotSelected: function () {
    return (this.findProperty('serviceName', 'GANGLIA').get('isSelected') === false || this.findProperty('serviceName', 'NAGIOS').get('isSelected') === false);
  },

  submit: function () {
    var self = this;
    if (this.needToAddMapReduce()) {
      App.ModalPopup.show({
        header: Em.I18n.t('installer.step4.mapreduceCheck.popup.header'),
        body: Em.I18n.t('installer.step4.mapreduceCheck.popup.body'),
        onPrimary: function () {
          self.findProperty('serviceName', 'MAPREDUCE').set('isSelected', true);
          this.hide();
          self.validateMonitoring();
        },
        onSecondary: function () {
          this.hide();
        }
      });
    } else {
      self.validateMonitoring();
    }
  },

  validateMonitoring: function () {
    var self = this;
    if (this.gangliaOrNagiosNotSelected()) {
      App.ModalPopup.show({
        header: Em.I18n.t('installer.step4.monitoringCheck.popup.header'),
        body: Em.I18n.t('installer.step4.monitoringCheck.popup.body'),
        onPrimary: function () {
          this.hide();
          self.proceed();
        },
        onSecondary: function () {
          this.hide();
        }
      });
    } else {
      self.proceed();
    }
  },

  proceed: function () {
    this.saveSelectedServiceNamesToDB();
    App.router.send('next');
  }
})