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
