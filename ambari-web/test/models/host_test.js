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

describe('App.Host', function () {

  var data = [
    {
      id: 'host1',
      host_name: 'host1',
      memory: 200000,
      disk_total: 100.555,
      disk_free: 90.555,
      health_status: 'HEALTHY',
      last_heart_beat_time: (new Date()).getTime() - 18100000
    },
    {
      id: 'host2',
      host_name: 'host2',
      memory: 99999,
      disk_total: 90,
      disk_free: 90,
      health_status: 'HEALTHY',
      last_heart_beat_time: (new Date()).getTime() - 170000
    },
    {
      id: 'host3',
      host_name: 'host3',
      memory: 99999,
      disk_total: 99.999,
      disk_free: 0,
      health_status: 'UNKNOWN',
      last_heart_beat_time: (new Date()).getTime()
    }
  ];
  App.set('testMode', false);
  App.store.loadMany(App.Host, data);

  describe('#diskUsedFormatted', function () {

    it('host1 - 10GB ', function () {
      var host = App.Host.find().findProperty('hostName', 'host1');
      expect(host.get('diskUsedFormatted')).to.equal('10GB');
    });
    it('host2 - 0GB', function () {
      var host = App.Host.find().findProperty('hostName', 'host2');
      expect(host.get('diskUsedFormatted')).to.equal('0GB');
    });
    it('host3 - 100GB', function () {
      var host = App.Host.find().findProperty('hostName', 'host3');
      expect(host.get('diskUsedFormatted')).to.equal('100GB');
    });
  });

  describe('#diskTotalFormatted', function () {

    it('host1 - 100.56GB ', function () {
      var host = App.Host.find().findProperty('hostName', 'host1');
      expect(host.get('diskTotalFormatted')).to.equal('100.56GB');
    });
    it('host2 - 90GB', function () {
      var host = App.Host.find().findProperty('hostName', 'host2');
      expect(host.get('diskTotalFormatted')).to.equal('90GB');
    });
    it('host3 - 100GB', function () {
      var host = App.Host.find().findProperty('hostName', 'host3');
      expect(host.get('diskTotalFormatted')).to.equal('100GB');
    });
  });

  describe('#diskUsageFormatted', function () {

    it('host1 - 9.94% ', function () {
      var host = App.Host.find().findProperty('hostName', 'host1');
      expect(host.get('diskUsageFormatted')).to.equal('9.94%');
    });
    it('host2 - 0%', function () {
      var host = App.Host.find().findProperty('hostName', 'host2');
      expect(host.get('diskUsageFormatted')).to.equal('0%');
    });
    it('host3 - 100%', function () {
      var host = App.Host.find().findProperty('hostName', 'host3');
      expect(host.get('diskUsageFormatted')).to.equal('100%');
    });
  });

  describe('#isNotHeartBeating', function () {

    it('host1 - true ', function () {
      var host = App.Host.find().findProperty('hostName', 'host1');
      expect(host.get('isNotHeartBeating')).to.equal(true);
    });
    it('host2 - false', function () {
      var host = App.Host.find().findProperty('hostName', 'host2');
      expect(host.get('isNotHeartBeating')).to.equal(false);
    });
    it('host3 - false', function () {
      var host = App.Host.find().findProperty('hostName', 'host3');
      expect(host.get('isNotHeartBeating')).to.equal(false);
    });
  });

});
