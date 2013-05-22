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
require('utils/config');

describe('App.config', function () {

  App.supports.capacitySchedulerUi = true;

  describe('#identifyCategory', function () {
    var data = {};
    it('should return null if config doesn\'t have category', function () {
      expect(App.config.identifyCategory(data)).to.equal(null);
    });
    it('should return "AdvancedCoreSite" if filename "core-site.xml" and serviceName "HDFS"', function () {
      data = {
        serviceName: 'HDFS',
        filename: 'core-site.xml'
      }
      expect(App.config.identifyCategory(data).name).to.equal('AdvancedCoreSite');
    });
    it('should return "CapacityScheduler" if filename "capacity-scheduler.xml" and serviceName "MAPREDUCE"', function () {
      data = {
        serviceName: 'MAPREDUCE',
        filename: 'capacity-scheduler.xml'
      }
      expect(App.config.identifyCategory(data).name).to.equal('CapacityScheduler');
    });
  });

  describe('#handleSpecialProperties', function () {
    var config = {};
    it('value should be transformed to "1024" from "1024m"', function () {
      config = {
        displayType: 'int',
        value: '1024m',
        defaultValue: '1024m'
      };
      App.config.handleSpecialProperties(config);
      expect(config.value).to.equal('1024');
      expect(config.defaultValue).to.equal('1024');
    });
    it('value should be transformed to true from "true"', function () {
      config = {
        displayType: 'checkbox',
        value: 'true',
        defaultValue: 'true'
      };
      App.config.handleSpecialProperties(config);
      expect(config.value).to.equal(true);
      expect(config.defaultValue).to.equal(true);
    });
    it('value should be transformed to false from "false"', function () {
      config = {
        displayType: 'checkbox',
        value: 'false',
        defaultValue: 'false'
      };
      App.config.handleSpecialProperties(config);
      expect(config.value).to.equal(false);
      expect(config.defaultValue).to.equal(false);
    });
  });

  describe('#calculateConfigProperties', function () {
    var config = {};
    var isAdvanced = false;
    var advancedConfigs = [];
    it('isUserProperty should be true if config is custom(site.xml) and not advanced', function () {
      config = {
        serviceName: 'HDFS',
        filename: 'core-site.xml'
      };
      App.config.calculateConfigProperties(config, isAdvanced, advancedConfigs);
      expect(config.isUserProperty).to.equal(true);
    });

    it('isUserProperty should be false if config from "capacity-scheduler.xml" or "mapred-queue-acls.xml" ', function () {
      config = {
        name: 'test',
        serviceName: 'MAPREDUCE',
        filename: 'capacity-scheduler.xml',
        isUserProperty: false
      };
      isAdvanced = true;
      App.config.calculateConfigProperties(config, isAdvanced, advancedConfigs);
      expect(config.isUserProperty).to.equal(false);
    });

    it('isRequired should be false if config is advanced"', function () {
      config = {
        name: 'test',
        serviceName: 'HDFS',
        filename: 'core-site.xml'
      };
      isAdvanced = true;
      advancedConfigs = [{name:'test', filename: 'test.xml'}];
      App.config.calculateConfigProperties(config, isAdvanced, advancedConfigs);
      expect(config.category).to.equal('Advanced');
      expect(config.isRequired).to.equal(true);
      expect(config.filename).to.equal('test.xml');
    });
  });

});