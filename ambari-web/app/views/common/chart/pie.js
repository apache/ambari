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

App.ChartPieView = Em.View.extend({
  w:90,
  h:90,
  data:[300, 500],
  palette: new Rickshaw.Color.Palette({ scheme: 'munin'}),
  donut:d3.layout.pie().sort(null),

  r:function () {
    return Math.min(this.get('w'), this.get('h')) / 2;
  }.property('w', 'h'),

  outerR:function () {
    return this.get('r'); // - 10;
  }.property('r'),

  innerR:function () {
    return 0; // this.get('r') - 20;
  }.property('r'),

  arc:function () {
    return d3.svg.arc().innerRadius(this.get('innerR')).outerRadius(this.get('outerR'));
  }.property(),

  didInsertElement:function () {
    this._super();
    this.appendSvg();
  },

  selector:function () {
    return '#' + this.get('elementId');
  }.property('elementId'),

  appendSvg:function () {
    var thisChart = this;

    this.set('svg', d3.select(this.get('selector')).append("svg:svg")
      .attr("width", thisChart.get('w'))
      .attr("height", thisChart.get('h'))
      .append("svg:g")
      .attr("transform", "translate(" + thisChart.get('w') / 2 + "," + thisChart.get('h') / 2 + ")"));

    this.set('arcs', this.get('svg').selectAll("path")
      .data(thisChart.donut(thisChart.get('data')))
      .enter().append("svg:path")
      .attr("fill", function (d, i) {
        return thisChart.palette.color(i);
      })
      .attr("d", this.get('arc')));
  }
});