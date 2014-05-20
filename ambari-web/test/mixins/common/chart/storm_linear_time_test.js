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

require('mixins/common/chart/storm_linear_time');

var slt,
  template,
  series,
  jsonDataFalse = {
    metrics: {
      id: 'metrics'
    }
  },
  jsonDataTrue = {
    metrics: {
      storm: {
        nimbus: {
          name: 'nimbus'
        }
      }
    }
  };

describe('App.StormLinearTimeChartMixin', function () {

  beforeEach(function () {
    slt = Em.Object.create(App.StormLinearTimeChartMixin, {
      stormChartDefinition: [
        {
          field: 'name',
          name: 'nimbus'
        }
      ]
    });
  });

  describe('#getDataForAjaxRequest', function () {
    it('should take data from stormChartDefinition', function () {
      template = slt.getDataForAjaxRequest().metricsTemplate;
      expect(template).to.contain('metrics');
      expect(template).to.contain('storm');
      expect(template).to.contain('nimbus');
    });
  });

  describe('#transformToSeries', function () {
    it('should be empty', function () {
      expect(slt.transformToSeries(jsonDataFalse)).to.be.empty;
    });
    it('should take one element from data', function () {
      slt.set('transformData', function (data, name) {
        return name + ': ' + JSON.stringify(data);
      });
      series = slt.transformToSeries(jsonDataTrue);
      expect(series).to.have.length(1);
      expect(series[0]).to.equal('nimbus: "nimbus"');
    });
  });

});
