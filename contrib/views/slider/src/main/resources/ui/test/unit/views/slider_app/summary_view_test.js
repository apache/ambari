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

moduleFor('view:SliderAppSummary', 'App.SliderAppSummaryView', {

  needs: [
    'view:AppMetric',
    'view:Chart'
  ],

  setup: function () {
    Em.run(App, App.advanceReadiness);
  },

  teardown: function () {
    App.reset();
  }

});

test('graphsNotEmpty', function () {

  var sliderAppSummaryView = this.subject({
    controller: {
      model: Em.Object.create({
        supportedMetricNames: ''
      })
    }
  });
  Em.run(function () {
    sliderAppSummaryView.set('controller.model.supportedMetricNames', 'firstMetric,secondMetric');
    var v = sliderAppSummaryView.createChildView(sliderAppSummaryView.get('graphs')[0].view);
    v._refreshGraph({
      "metrics": {
        "firstMetric": [
          [
            5.0,
            1401351555
          ]
        ]}
    });
    sliderAppSummaryView.createChildView(sliderAppSummaryView.get('graphs')[1].view);
  });
  ok(sliderAppSummaryView.get('graphsNotEmpty'), 'One graph has metrics');

  Em.run(function () {
    var v = sliderAppSummaryView.createChildView(sliderAppSummaryView.get('graphs')[0].view);
    v._refreshGraph({
      "metrics": {}
    });
  });
  equal(sliderAppSummaryView.get('graphsNotEmpty'), false, 'No one graph has metrics');
});