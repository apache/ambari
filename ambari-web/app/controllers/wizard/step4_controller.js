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

App.WizardStep4Controller = Em.ArrayController.extend({

  name: 'wizardStep4Controller',
  content: [],

  isSubmitDisabled:function(){
    return this.filterProperty('isSelected', true).filterProperty('isInstalled', false).length === 0;
  }.property("@each.isSelected"),

  /**
   * Check whether all properties are selected
   */
  isAll: function () {
    return this.everyProperty('isSelected', true);
  }.property('@each.isSelected'),

  /**
   * Check whether none properties(minimum) are selected
   */
  isMinimum: function () {
    return this.filterProperty('isDisabled', false).everyProperty('isSelected', false);
  }.property('@each.isSelected'),

  /**
   * Update hidden services. Make them to have the same status as master ones.
   */
  checkDependencies: function () {
    var hbase = this.findProperty('serviceName', 'HBASE');
    var zookeeper = this.findProperty('serviceName', 'ZOOKEEPER');
    var hive = this.findProperty('serviceName', 'HIVE');
    var hcatalog = this.findProperty('serviceName', 'HCATALOG');
    var webhcat = this.findProperty('serviceName', 'WEBHCAT');

    // prevent against getting error when not all elements have been loaded yet
    if (hbase && zookeeper && hive && hcatalog && webhcat) {
      zookeeper.set('isSelected', hbase.get('isSelected') || hive.get('isSelected'));
      hcatalog.set('isSelected', hive.get('isSelected'));
      webhcat.set('isSelected', hive.get('isSelected'));
    }
  }.observes('@each.isSelected'),

  /**
   * Onclick handler for <code>select all</code> link
   */
  selectAll: function () {
    this.setEach('isSelected', true);
  },

  /**
   * onclick handler for <code>select minimum</code> link
   */
  selectMinimum: function () {
    this.filterProperty('isDisabled', false).setEach('isSelected', false);
  },

  /**
   * Check whether we should turn on <code>MapReduce</code> service
   * @return {Boolean}
   */
  needToAddMapReduce: function () {
    if (this.findProperty('serviceName', 'MAPREDUCE').get('isSelected') === false) {
      var mapreduceDependentServices = this.filter(function (item) {
        return ['PIG', 'OOZIE', 'HIVE'].contains(item.get('serviceName')) && item.get('isSelected', true);
      });
      return (mapreduceDependentServices.get('length') > 0);
    }

    return false;
  },

  /**
   * Check do we have any monitoring service turned on
   * @return {Boolean}
   */
  gangliaOrNagiosNotSelected: function () {
    return (this.findProperty('serviceName', 'GANGLIA').get('isSelected') === false || this.findProperty('serviceName', 'NAGIOS').get('isSelected') === false);
  },

  /**
   * Check whether user turned on monitoring service and go to next step
   */
  validateMonitoring: function () {
    if (this.gangliaOrNagiosNotSelected()) {
      App.ModalPopup.show({
        header: Em.I18n.t('installer.step4.monitoringCheck.popup.header'),
        body: Em.I18n.t('installer.step4.monitoringCheck.popup.body'),
        onPrimary: function () {
          this.hide();
          App.router.send('next');
        },
        onSecondary: function () {
          this.hide();
        }
      });
    } else {
      App.router.send('next');
    }
  },

  /**
   * Onclick handler for <code>Next</code> button
   */
  submit: function () {
    if(!this.get("isSubmitDisabled")){
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
    }
  }
})