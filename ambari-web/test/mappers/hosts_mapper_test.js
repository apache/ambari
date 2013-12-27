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

var Ember = require('ember');
var App = require('app');

require('models/host');
require('models/host_component');
require('mappers/server_data_mapper');
require('mappers/hosts_mapper');

describe('App.hostsMapper', function () {

  describe('#sortByPublicHostName()', function () {
    var tests = [
      {
        i: [
          {public_host_name: 'host0'},
          {public_host_name: 'host1'},
          {public_host_name: 'host2'},
          {public_host_name: 'host3'}
        ],
        m: 'Sorted array',
        e: ['host0','host1','host2','host3']
      },
      {
        i: [
          {public_host_name: 'host3'},
          {public_host_name: 'host2'},
          {public_host_name: 'host1'},
          {public_host_name: 'host0'}
        ],
        m: 'Reverse sorted array',
        e: ['host0','host1','host2','host3']
      },
      {
        i: [
          {public_host_name: 'host2'},
          {public_host_name: 'host3'},
          {public_host_name: 'host0'},
          {public_host_name: 'host1'}
        ],
        m: 'Shuffled array',
        e: ['host0','host1','host2','host3']
      }
    ];
    tests.forEach(function(test) {
      it(test.m, function() {
        expect(App.hostsMapper.sortByPublicHostName(test.i).mapProperty('public_host_name')).to.eql(test.e);
      });
    });
  });

});
