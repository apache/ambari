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

require('models/host');
require('models/rack');

describe('App.Rack', function () {

  var data = {
    id: 'rack1',
    name: 'rack1'
  };

  App.store.safeLoad(App.Rack, data);

  describe('#liveHostsCount', function () {

    it('rack1 has two live hosts', function () {
      var rack = App.Rack.find().findProperty('name', 'rack1');
      expect(rack.get('liveHostsCount')).to.equal(2);
    });

    it('rack1 has three live hosts', function () {
      App.store.safeLoad(App.Host, {
        id: 'host3',
        host_name: 'host3',
        health_status: 'HEALTHY'
      });
      var rack = App.Rack.find().findProperty('name', 'rack1');
      rack.set('name', 'rack1');
      expect(rack.get('liveHostsCount')).to.equal(3);
    });
  });


});
