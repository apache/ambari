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

  describe('#setPort', function () {
    var testData = [
      {
        'service_id': 'HDFS',
        'protocol': 'https',
        'version': '2.0.6',
        'result': ''
      },
      {
        'service_id': 'YARN',
        'protocol': 'http',
        'version': '2.0.6',
        'result': '8088'
      },
      {
        'service_id': 'YARN',
        'protocol': 'https',
        'version': '2.0.5',
        'result': '8088'
      },
      {
        'service_id': 'YARN',
        'protocol': 'https',
        'version': '2.0.6',
        'result': '8090'
      },
    ];

    testData.forEach(function(item) {
      it('should return empty string if service_id is not YARN, 8090 if protocol is https and stack version higher than 2.0.5, http otherwise', function () {
        expect(quickViewLinks.setPort(item.service_id, item.protocol, item.version)).to.equal(item.result);
      })
    },this);
  });
});
