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
require('views/main/service/info/metrics/flume/flume_outgoing_mma');

describe('App.ChartServiceMetricsFlume_OutgoingMMA', function () {

  var view;

  beforeEach(function () {
    view = App.ChartServiceMetricsFlume_OutgoingMMA.create();
  });

  describe('#displayName', function () {
    it('should return name', function () {
      var name = 'test';
      var hostName = 'host1';
      expect(view.seriesTemplate.displayName(name, hostName)).to.equal(
        Em.I18n.t('services.service.info.metrics.flume.outgoing_mma').format(name)
      );
    });
  });

  describe('#colorForSeries', function () {

    it('should return null', function () {
      var seriesMock = {};
      view.colorForSeries(seriesMock);
      expect(view.colorForSeries(seriesMock)).to.be.equal(null);
    });

    it('should return #0066b3', function () {
      var seriesMock = {name: Em.I18n.t('services.service.info.metrics.flume.outgoing_mma').format("avg")};
      view.colorForSeries(seriesMock);
      expect(view.colorForSeries(seriesMock)).to.be.equal('#0066b3');
    });

    it('should return #00CC00', function () {
      var seriesMock = {name: Em.I18n.t('services.service.info.metrics.flume.outgoing_mma').format("min")};
      view.colorForSeries(seriesMock);
      expect(view.colorForSeries(seriesMock)).to.be.equal('#00CC00');
    });

    it('should return #FF8000', function () {
      var seriesMock = {name: Em.I18n.t('services.service.info.metrics.flume.outgoing_mma').format("max")};
      view.colorForSeries(seriesMock);
      expect(view.colorForSeries(seriesMock)).to.be.equal('#FF8000');
    });
  });

});
