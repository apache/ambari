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
var stringUtils = require('utils/string_utils');

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
    return this.filterProperty('canBeSelected', true).everyProperty('isSelected', true);
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
    var services = {};
    this.forEach(function (service) {
      services[service.get('serviceName')] = service;
    });

    // prevent against getting error when not all elements have been loaded yet
    if (services['HBASE'] && services['ZOOKEEPER'] && services['HIVE'] && services['HCATALOG'] && services['WEBHCAT']) {
      if (services['YARN'] && services['MAPREDUCE2']) {
        services['MAPREDUCE2'].set('isSelected', services['YARN'].get('isSelected'));
      }
      services['HCATALOG'].set('isSelected', services['HIVE'].get('isSelected'));
      services['WEBHCAT'].set('isSelected', services['HIVE'].get('isSelected'));
    }
  }.observes('@each.isSelected'),

  /**
   * Onclick handler for <code>select all</code> link
   */
  selectAll: function () {
    this.filterProperty('canBeSelected', true).setEach('isSelected', true);
  },

  /**
   * onclick handler for <code>select minimum</code> link
   */
  selectMinimum: function () {
    this.filterProperty('isDisabled', false).setEach('isSelected', false);
  },

  /**
   * Check whether we should turn on <code>serviceName</code> service according to selected <code>dependentServices</code>
   * @param serviceName checked service
   * @param dependentServices list of dependent services
   * @returns {boolean}
   */
  needAddService: function(serviceName, dependentServices) {
    if (!(dependentServices instanceof Array)) {
      dependentServices = [dependentServices];
    }
    if (this.findProperty('serviceName', serviceName) && this.findProperty('serviceName', serviceName).get('isSelected') === false) {
      var ds = this.filter(function (item) {
        return dependentServices.contains(item.get('serviceName')) && item.get('isSelected');
      });
      return (ds.get('length') > 0);
    }
    return false;
  },

  /**
   * Check whether we should turn on <code>Oozie</code> service
   * @return {Boolean}
   */
  needToAddOozie: function () {
    return this.needAddService('OOZIE', ['FALCON']);
  },

  /**
   * Check whether we should turn on <code>MapReduce</code> service
   * @return {Boolean}
   */
  needToAddMapReduce: function () {
    return this.needAddService('MAPREDUCE', ['PIG', 'OOZIE', 'HIVE']);
  },
  /**
   * Check whether we should turn on <code>MapReduce2</code> service
   * @return {Boolean}
   */
  needToAddYarnMapReduce2: function() {
    return this.needAddService('YARN', ['PIG', 'OOZIE', 'HIVE','TEZ']);
  },
  /**
   * Check whether we should turn on <code>Tez</code> service
   * @return {Boolean}
   */
  needToAddTez: function() {
    return this.needAddService('TEZ', ['YARN']);
  },
  /**
   * Check whether we should turn on <code>ZooKeeper</code> service
   * @return {Boolean}
   */
  needToAddZooKeeper: function () {
    if (App.get('isHadoop2Stack')) {
      return this.findProperty('serviceName', 'ZOOKEEPER') && this.findProperty('serviceName', 'ZOOKEEPER').get('isSelected') === false;
    } else {
      return this.needAddService('ZOOKEEPER', ['HBASE', 'HIVE', 'WEBHCAT', 'STORM']);
    }
  },
  /** 
   * Check whether we should turn on <code>HDFS or GLUSTERFS</code> service
   * @return {Boolean}
   */
  noDFSs: function () {
    return (this.findProperty('serviceName', 'HDFS').get('isSelected') === false &&
    		(!this.findProperty('serviceName', 'GLUSTERFS') || this.findProperty('serviceName', 'GLUSTERFS').get('isSelected') === false));
  },

  /** 
   * Check if multiple distributed file systems were selected
   * @return {Boolean}
   */
  multipleDFSs: function () {
	  return (this.findProperty('serviceName', 'HDFS').get('isSelected') === true &&
	    	(this.findProperty('serviceName', 'GLUSTERFS') && this.findProperty('serviceName', 'GLUSTERFS').get('isSelected') === true));
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
      this.monitoringCheckPopup();
    } else {
      App.router.send('next');
    }
  },

  /**
   * submit checks describe dependency rules between services
   * @checkCallback - callback, which check for dependency
   * @popupParams - parameters for popup
   */
  submitChecks: [
    {
      checkCallback: 'needToAddMapReduce',
      popupParams: [{serviceName: 'MAPREDUCE', selected: true}, 'mapreduceCheck']
    },
    {
      checkCallback: 'noDFSs',
      popupParams: [{serviceName:'HDFS', selected: true}, 'hdfsCheck']
    },
    {
      checkCallback: 'needToAddYarnMapReduce2',
      popupParams: [{serviceName:'YARN', selected:true}, 'yarnCheck']
    },
    {
      checkCallback: 'needToAddZooKeeper',
      popupParams: [{serviceName:'ZOOKEEPER', selected: true}, 'zooKeeperCheck']
    },
    {
      checkCallback: 'multipleDFSs',
      popupParams: [[
        {serviceName: 'HDFS', selected: true},
        {serviceName: 'GLUSTERFS', selected: false}
      ], 'multipleDFS']
    },
    {
      checkCallback: 'needToAddOozie',
      popupParams: [{serviceName:'OOZIE', selected: true}, 'oozieCheck']
    },
    {
      checkCallback: 'needToAddTez',
      popupParams: [{serviceName:'TEZ', selected: true}, 'tezCheck']
    }
  ],

  /**
   * Onclick handler for <code>Next</code> button
   */
  submit: function () {
    var submitChecks = this.get('submitChecks');
    var doValidateMonitoring = true;
    if (!this.get("isSubmitDisabled")) {
      for (var i = 0; i < submitChecks.length; i++) {
        if (this[submitChecks[i].checkCallback].call(this)) {
          doValidateMonitoring = false;
          this.needToAddServicePopup.apply(this, submitChecks[i].popupParams);
          break;
        }
      }
      if (doValidateMonitoring) {
        this.validateMonitoring();
      }
    }
  },
  
  /**
   * Select/deselect services
   * @param services array of objects
   *  <code>
   *    [
   *      {
   *        service: 'HDFS',
   *        selected: true
   *      },
   *      ....
   *    ]
   *  </code>
   * @param i18nSuffix
   */
  needToAddServicePopup: function(services, i18nSuffix) {
    if (!(services instanceof Array)) {
      services = [services];
    }
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('installer.step4.' + i18nSuffix + '.popup.header'),
      body: Em.I18n.t('installer.step4.' + i18nSuffix + '.popup.body'),
      onPrimary: function () {
        services.forEach(function(service) {
          self.findProperty('serviceName', service.serviceName).set('isSelected', service.selected);
        });
        this.hide();
        self.submit();
      }
    });
  },

  monitoringCheckPopup: function () {
    App.ModalPopup.show({
      header: Em.I18n.t('installer.step4.monitoringCheck.popup.header'),
      body: Em.I18n.t('installer.step4.monitoringCheck.popup.body'),
      onPrimary: function () {
        this.hide();
        App.router.send('next');
      }
    });
  }
});
