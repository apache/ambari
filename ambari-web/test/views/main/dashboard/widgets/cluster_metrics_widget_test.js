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

  describe('#exportTargetView', function () {

    var childViews = [
        {
          p0: 'v0'
        },
        {
          p1: 'v1'
        }
      ],
      title = 'should take last child view';

    beforeEach(function () {
      view.get('childViews').pushObjects(childViews);
    });

    it(title, function () {
      expect(view.get('exportTargetView')).to.eql(childViews[1]);
    });
  });

  describe("#didInsertElement()", function() {

    beforeEach(function() {
      sinon.stub(App, 'tooltip');
    });
    afterEach(function() {
      App.tooltip.restore();
    });

    it("App.tooltip should be called", function() {
      view.didInsertElement();
      expect(App.tooltip.calledOnce).to.be.true;
    });
  });

  describe("#willDestroyElement()", function() {
    var mock = {
      tooltip: Em.K
    };

    beforeEach(function() {
      sinon.stub(mock, 'tooltip');
      sinon.stub(view, '$').returns(mock);
    });
    afterEach(function() {
      mock.tooltip.restore();
      view.$.restore();
    });

    it("tooltip destroy should be called", function() {
      view.willDestroyElement();
      expect(mock.tooltip.calledWith('destroy')).to.be.true;
    });
  });

});
