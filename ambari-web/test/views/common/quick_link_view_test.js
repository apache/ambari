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
require('views/common/quick_view_link_view');

describe('App.QuickViewLinks', function () {

  var quickViewLinks = App.QuickViewLinks.create({});

  describe('#setProtocol', function() {
    var tests = [
      { serviceName: "GANGLIA", ambariProperties: { 'ganglia.https': true }, m: "https for ganglia", result: "https" },
      { serviceName: "GANGLIA", ambariProperties: { 'ganglia.https': false }, m: "http for ganglia 1", result: "http" },
      { serviceName: "GANGLIA", m: "http for ganglia 2", result: "http" },
      { serviceName: "NAGIOS", ambariProperties: { 'nagios.https': true }, m: "https for nagios", result: "https" },
      { serviceName: "NAGIOS", ambariProperties: { 'nagios.https': false }, m: "http for nagios", result: "http" },
      { serviceName: "YARN", configProperties: [
        { type: 'yarn-site', properties: { 'yarn.http.policy': 'HTTPS_ONLY' }},
        { type: 'core-site', properties: { 'hadoop.ssl.enabled': null }}
      ], m: "https for yarn", result: "https" },
      { serviceName: "YARN", configProperties: [
        { type: 'yarn-site', properties: { 'yarn.http.policy': 'HTTP_ONLY' }},
        { type: 'core-site', properties: { 'hadoop.ssl.enabled': null }}
      ], m: "http for yarn", result: "http" },
      { serviceName: "YARN", configProperties: [
        { type: 'yarn-site', properties: { 'yarn.http.policy': 'HTTP_ONLY' }},
        { type: 'core-site', properties: { 'hadoop.ssl.enabled': true }}
      ], m: "http for yarn (overrides hadoop.ssl.enabled)", result: "http" },
      { serviceName: "YARN", configProperties: [
        { type: 'yarn-site', properties: { 'yarn.http.policy': 'HTTPS_ONLY' }},
        { type: 'core-site', properties: { 'hadoop.ssl.enabled': false }}
      ], m: "https for yarn (overrides hadoop.ssl.enabled)", result: "https" },
      { serviceName: "YARN", configProperties: [
        { type: 'core-site', properties: { 'hadoop.ssl.enabled': true }}
      ], m: "https for yarn by hadoop.ssl.enabled", result: "https" },
      { serviceName: "MAPREDUCE2", configProperties: [
        { type: 'mapred-site', properties: { 'mapreduce.jobhistory.http.policy': 'HTTPS_ONLY' }},
        { type: 'core-site', properties: { 'hadoop.ssl.enabled': null }}
      ], m: "https for mapreduce2", result: "https" },
      { serviceName: "MAPREDUCE2", configProperties: [
        { type: 'mapred-site', properties: { 'mapreduce.jobhistory.http.policy': 'HTTP_ONLY' }},
        { type: 'core-site', properties: { 'hadoop.ssl.enabled': null }}
      ], m: "http for mapreduce2", result: "http" },
      { serviceName: "MAPREDUCE2", configProperties: [
        { type: 'core-site', properties: { 'hadoop.ssl.enabled': true }}
      ], m: "https for mapreduce2 by hadoop.ssl.enabled", result: "https" },
      { serviceName: "ANYSERVICE", configProperties: [
        { type: 'core-site', properties: { 'hadoop.ssl.enabled': true }}
      ], m: "http for anyservice hadoop.ssl.enabled is true but doesn't support security", servicesSupportsHttps: [], result: "http" },
      { serviceName: "ANYSERVICE", configProperties: [
        { type: 'core-site', properties: { 'hadoop.ssl.enabled': false }}
      ], m: "http for anyservice hadoop.ssl.enabled is false", servicesSupportsHttps: ["ANYSERVICE"], result: "http" },
      { serviceName: "ANYSERVICE", configProperties: [
        { type: 'core-site', properties: { 'hadoop.ssl.enabled': true }}
      ], m: "https for anyservice", servicesSupportsHttps: ["ANYSERVICE"], result: "https" }
    ];

    tests.forEach(function(t) {
      it(t.m, function() {
        quickViewLinks.set('servicesSupportsHttps', t.servicesSupportsHttps);
        expect(quickViewLinks.setProtocol(t.serviceName, t.configProperties, t.ambariProperties)).to.equal(t.result);
      });
    });
  });

  describe('#setPort', function () {
    var testData = [
      Em.Object.create({
        'service_id': 'YARN',
        'protocol': 'http',
        'result': '8088',
        'default_http_port': '8088',
        'default_https_port': '8090',
        'regex': '\\w*:(\\d+)'
      }),
      Em.Object.create({
        'service_id': 'YARN',
        'protocol': 'https',
        'https_config': 'https_config',
        'result': '8090',
        'default_http_port': '8088',
        'default_https_port': '8090',
        'regex': '\\w*:(\\d+)'
      }),
      Em.Object.create({
        'service_id': 'YARN',
        'protocol': 'https',
        'https_config': 'https_config',
        'result': '8090',
        'default_http_port': '8088',
        'default_https_port': '8090',
        'regex': '\\w*:(\\d+)'
      })
    ];

    testData.forEach(function(item) {
      it(item.service_id + ' ' + item.protocol, function () {
        expect(quickViewLinks.setPort(item, item.protocol, item.version)).to.equal(item.result);
      })
    },this);
  });
});
