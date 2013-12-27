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
require('controllers/wizard');
require('controllers/main/service/add_controller');

describe('App.AddServiceController', function() {

  describe('#isServiceConfigurable', function() {
    var tests = [
      {
        services: [
          {serviceName: 'HDFS'},
          {serviceName: 'MAPREDUCE'},
          {serviceName: 'NAGIOS'}
        ],
        service: 'HDFS',
        m: 'Service is configurable',
        e: true
      },
      {
        services: [
          {serviceName: 'HDFS'},
          {serviceName: 'MAPREDUCE'},
          {serviceName: 'NAGIOS'}
        ],
        service: 'PIG',
        m: 'Service is not configurable',
        e: false
      },
      {
        services: [],
        service: 'HDFS',
        m: 'No services',
        e: false
      }
    ];
    tests.forEach(function(test) {
      var controller = App.AddServiceController.create({serviceConfigs: test.services});
      it('', function() {
        expect(controller.isServiceConfigurable(test.service)).to.equal(test.e);
      });
    });
  });

  describe('#skipConfigStep', function() {
    var tests = [
      {
        content: {
          services:[
            {serviceName: 'HDFS', isInstalled: true, isSelected: true},
            {serviceName: 'PIG', isInstalled: false, isSelected: true},
            {serviceName: 'MAPREDUCE', isInstalled: true, isSelected: true}
          ]
        },
        serviceConfigs: [
          {serviceName: 'HDFS'},
          {serviceName: 'MAPREDUCE'},
          {serviceName: 'NAGIOS'}
        ],
        m: '2 installed services and 1 new that can\'t be configured',
        e: true
      },
      {
        content: {
          services:[
            {serviceName: 'HDFS', isInstalled: true, isSelected: true},
            {serviceName: 'NAGIOS', isInstalled: false, isSelected: true},
            {serviceName: 'MAPREDUCE', isInstalled: true, isSelected: true}
          ]
        },
        serviceConfigs: [
          {serviceName: 'HDFS'},
          {serviceName: 'MAPREDUCE'},
          {serviceName: 'NAGIOS'}
        ],
        m: '2 installed services and 1 new that can be configured',
        e: false
      },
      {
        content: {
          services:[
            {serviceName: 'HDFS', isInstalled: true, isSelected: true},
            {serviceName: 'PIG', isInstalled: false, isSelected: true},
            {serviceName: 'SQOOP', isInstalled: false, isSelected: true},
            {serviceName: 'MAPREDUCE', isInstalled: true, isSelected: true}
          ]
        },
        serviceConfigs: [
          {serviceName: 'HDFS'},
          {serviceName: 'MAPREDUCE'},
          {serviceName: 'NAGIOS'}
        ],
        m: '2 installed services and 2 new that can\'t be configured',
        e: true
      }
    ];
    tests.forEach(function(test) {
      var controller = App.AddServiceController.create({content:{services: test.content.services}, serviceConfigs: test.serviceConfigs});
      it(test.m, function() {
        expect(controller.skipConfigStep()).to.equal(test.e);
      })
    });
  });

});
