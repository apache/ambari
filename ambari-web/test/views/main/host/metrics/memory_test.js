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
require('views/main/host/metrics/memory');

describe('App.ChartHostMetricsMemory', function () {

  var view;

  beforeEach(function () {
    view = App.ChartHostMetricsMemory.create();
  });

  describe('#seriesTemplate.displayName()', function() {

    it('should return color', function() {
      var name = 'mem_shared';
      expect(view.seriesTemplate.displayName(name)).to.equal(Em.I18n.t('hosts.host.metrics.memory.displayNames.mem_shared'));
    });

    it('should return undefined', function() {
      var name = 'CPU';
      expect(view.seriesTemplate.displayName(name)).to.equal();
    });
  });

});
