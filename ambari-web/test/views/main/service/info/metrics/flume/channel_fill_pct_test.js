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
require('views/main/service/info/metrics/flume/channel_fill_pct');

describe('App.ChartServiceMetricsFlume_ChannelFillPercent', function () {

  var view;

  beforeEach(function () {
    view = App.ChartServiceMetricsFlume_ChannelFillPercent.create();
  });

  describe('#displayName', function () {
    it('should return name', function () {
      var name = 'test';
      var hostName = 'host1';
      expect(view.seriesTemplate.displayName(name, hostName)).to.equal(
        Em.I18n.t('services.service.info.metrics.flume.channelName').format(name + ' (' + hostName + ')')
      );
    });
  });

  describe('#getData', function () {

    beforeEach(function () {
      sinon.stub(view, 'getFlumeData', Em.K);
    });

    afterEach(function () {
      view.getFlumeData.restore();
    });

    it('should return flume data from json', function () {
      var jsonDataMock = {"data": "someData"}
      view.getData(jsonDataMock);
      expect(view.getFlumeData.calledWith(jsonDataMock)).to.be.true;
    });
  });

});
