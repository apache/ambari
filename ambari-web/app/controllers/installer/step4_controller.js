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
  rawContent: [
    {
      serviceName: 'HDFS',
      displayName: 'HDFS',
      isDisabled: true,
      description: Em.I18n.t('services.hdfs.description')
    },
    {
      serviceName: 'MAPREDUCE',
      displayName: 'MapReduce',
      isDisabled: true,
      description: Em.I18n.t('services.mapreduce.description')
    },
    {
      serviceName: 'NAGIOS',
      displayName: 'Nagios',
      isDisabled: true,
      description: Em.I18n.t('services.nagios.description')
    },
    {
      serviceName: 'GANGLIA',
      displayName: 'Ganglia',
      isDisabled: true,
      description: Em.I18n.t('services.ganglia.description')
    },
    {
      serviceName: 'HIVE',
      displayName: 'Hive + HCatalog',
      isDisabled: false,
      description: Em.I18n.t('services.hive.description')
    },
    {
      serviceName: 'HBASE',
      displayName: 'HBase + ZooKeeper',
      isDisabled: false,
      description: Em.I18n.t('services.hbase.description')
    },
    {
      serviceName: 'PIG',
      displayName: 'Pig',
      isDisabled: false,
      description: Em.I18n.t('services.pig.description')
    },
    {
      serviceName: 'SQOOP',
      displayName: 'Sqoop',
      isDisabled: false,
      description: Em.I18n.t('services.sqoop.description')
    },
    {
      serviceName: 'OOZIE',
      displayName: 'Oozie',
      isDisabled: false,
      description: Em.I18n.t('services.oozie.description')
    },
    {
      serviceName: 'ZOOKEEPER',
      isDisabled: false,
      isHidden: true
    },
    {
      serviceName: 'HCATALOG',
      isDisabled: false,
      isHidden: true
    }
  ],

  content: [],

  isAll: function() {
    return this.everyProperty('isSelected', true);
  }.property('@each.isSelected'),

  isMinimum: function() {
    return this.filterProperty('isDisabled', false).everyProperty('isSelected', false);
  }.property('@each.isSelected'),

  checkDependencies: function() {
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

  init: function() {
    this._super();
    // wrap each item with Ember.Object
    this.rawContent.forEach(function(item) {
      item.isSelected = true;
      this.pushObject(Ember.Object.create(item));
    }, this);
  },

  selectAll: function() {
    this.setEach('isSelected', true);
  },

  selectMinimum: function() {
    this.filterProperty('isDisabled', false).setEach('isSelected', false);
  },

  saveSelectedServiceNamesToDB: function() {
    var serviceNames = [];
    this.filterProperty('isSelected', true).forEach(function(item){
      serviceNames.push(item.serviceName);
    });
    db.setSelectedServiceNames(serviceNames);
  },

  submit: function() {
    this.saveSelectedServiceNamesToDB();
    App.router.send('next');
  }

})