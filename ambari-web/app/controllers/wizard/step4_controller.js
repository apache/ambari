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
    var hbase = this.findProperty('serviceName', 'HBASE');
    var zookeeper = this.findProperty('serviceName', 'ZOOKEEPER');
    var hive = this.findProperty('serviceName', 'HIVE');
    var hcatalog = this.findProperty('serviceName', 'HCATALOG');
    var webhcat = this.findProperty('serviceName', 'WEBHCAT');
    var yarn = this.findProperty('serviceName', 'YARN');
    var mapreduce2 = this.findProperty('serviceName', 'MAPREDUCE2');

    // prevent against getting error when not all elements have been loaded yet
    if (hbase && zookeeper && hive && hcatalog && webhcat) {
      if (yarn && mapreduce2) {
        mapreduce2.set('isSelected', yarn.get('isSelected'));
      }
      hcatalog.set('isSelected', hive.get('isSelected'));
      webhcat.set('isSelected', hive.get('isSelected'));
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
    return this.needAddService('YARN', ['PIG', 'OOZIE', 'HIVE']);
  },
  /**
   * Check whether we should turn on <code>ZooKeeper</code> service
   * @return {Boolean}
   */
  needToAddZooKeeper: function() {
    return this.needAddService('ZOOKEEPER', ['HBASE','HIVE','WEBHCAT']);
  },
  /**
   * Check whether we should turn on <code>ZooKeeper</code> service (on 2.x stack)
   * @returns {Boolean}
   */
  needToAddZooKeeperOnStack2x: function() {
    return this.findProperty('serviceName', 'ZOOKEEPER') && this.findProperty('serviceName', 'ZOOKEEPER').get('isSelected') === false;
  },
  /** 
   * Check whether we should turn on <code>HDFS or HCFS</code> service
   * @return {Boolean}
   */
  noDFSs: function () {
    return (this.findProperty('serviceName', 'HDFS').get('isSelected') === false &&
    		(!this.findProperty('serviceName', 'HCFS') || this.findProperty('serviceName', 'HCFS').get('isSelected') === false));
  },

  /** 
   * Check if multiple distributed file systems were selected
   * @return {Boolean}
   */
  multipleDFSs: function () {
	  return (this.findProperty('serviceName', 'HDFS').get('isSelected') === true &&
	    	(this.findProperty('serviceName', 'HCFS') && this.findProperty('serviceName', 'HCFS').get('isSelected') === true));
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
   * Onclick handler for <code>Next</code> button
   */
  submit: function () {
    if(!this.get("isSubmitDisabled")) {
      if (this.needToAddMapReduce()) {
        this.mapReduceCheckPopup();
      }
      else {
        if (this.noDFSs()) {
          this.needToAddHDFSPopup();
        }
        else {
          if (this.needToAddYarnMapReduce2()) {
            this.mapReduce2CheckPopup();
          }
          else {
            if ((!App.get('isHadoop2Stack') && this.needToAddZooKeeper()) || (App.get('isHadoop2Stack') && this.needToAddZooKeeperOnStack2x())) {
              this.zooKeeperCheckPopup();
            }
            else {
              if (this.multipleDFSs()) {
                this.multipleDFSPopup();
              }
               else {
                this.validateMonitoring();
              }
            }
          }
        }
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
      },
      onSecondary: function () {
        this.hide();
      }
    });
  },

  multipleDFSPopup: function() {
    var services = [
      {serviceName: 'HDFS', selected: true},
      {serviceName: 'HCFS', selected: false}
    ];
    this.needToAddServicePopup(services, 'multipleDFS');
  },
  needToAddHDFSPopup: function() {
    this.needToAddServicePopup({serviceName:'HDFS', selected: true}, 'hdfsCheck');
  },

  mapReduceCheckPopup: function () {
    this.needToAddServicePopup({serviceName:'MAPREDUCE', selected: true}, 'mapreduceCheck');
  },

  mapReduce2CheckPopup: function () {
    this.needToAddServicePopup({serviceName:'YARN', selected:true}, 'yarnCheck');
  },

  zooKeeperCheckPopup: function () {
    this.needToAddServicePopup({serviceName:'ZOOKEEPER', selected: true}, 'zooKeeperCheck');
  },

  monitoringCheckPopup: function () {
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
  }
});