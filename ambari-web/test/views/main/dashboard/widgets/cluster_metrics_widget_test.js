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

require('views/main/dashboard/widgets/cluster_metrics_widget');

describe('App.ClusterMetricsDashboardWidgetView', function () {

  var view;

  beforeEach(function () {
    view = App.ClusterMetricsDashboardWidgetView.create();
  });

  describe('#exportGraphData', function () {

    beforeEach(function () {
      sinon.stub(App.ajax, 'send', Em.K);
      view.get('childViews').pushObjects([
        {
          ajaxIndex: 'ai0',
          getDataForAjaxRequest: function () {
            return {
              p0: 'v0'
            }
          }
        },
        {
          ajaxIndex: 'ai1',
          getDataForAjaxRequest: function () {
            return {
              p1: 'v1'
            }
          }
        }
      ]);
    });

    afterEach(function () {
      App.ajax.send.restore();
    });

    var cases = [
      {
        event: {},
        isCSV: false,
        title: 'JSON export'
      },
      {
        event: {
          context: true
        },
        isCSV: true,
        title: 'CSV export'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        view.exportGraphData(item.event);
        expect(App.ajax.send.calledOnce).to.be.true;
        expect(App.ajax.send.firstCall.args[0].name).to.equal('ai1');
        expect(App.ajax.send.firstCall.args[0].data).to.eql({
          p1: 'v1',
          isCSV: item.isCSV
        });
      });
    });

  });

});
