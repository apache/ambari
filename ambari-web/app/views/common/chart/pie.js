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
  id:null,
  palette: new Rickshaw.Color.Palette({ scheme: 'munin'}),
  stroke: 'black',
  strokeWidth: 2,
  donut:d3.layout.pie().sort(null),
  existCenterText: false,
  centerTextColor: 'black',

  r:function () {
    return Math.min(this.get('w'), this.get('h')) / 2 - this.get('strokeWidth');
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
    var svg = d3.select(thisChart.get('selector')).append("svg:svg")
      .attr("id", thisChart.get('id'))
      .attr("width", thisChart.get('w'))
      .attr("height", thisChart.get('h'))
      .attr("stroke", thisChart.get('stroke'))
      .attr("stroke-width", thisChart.get('strokeWidth'));

    // set percentage data in center if there exist a center text
    if(thisChart.get('existCenterText')){
      this.set('svg', svg
        .append("svg:g")
        .attr("render-order", 1)
        .append("svg:text")
        .style('fill', thisChart.get('centerTextColor'))
        .attr("stroke", thisChart.get('centerTextColor'))
        .attr("font-size", 17)
        .attr("transform", "translate(" + thisChart.get('w') / 2 + "," + ((thisChart.get('h') / 2) + 3) + ")")
        .attr("text-anchor", "middle")
        .text(function(d) {
                 return thisChart.get('data')[0] + '%';
              })
         );
    }

    this.set('svg', svg
      .append("svg:g")
      .attr("transform", "translate(" + thisChart.get('w') / 2 + "," + thisChart.get('h') / 2 + ")"));

    this.set('arcs', thisChart.get('svg').selectAll("path")
      .data(thisChart.donut(thisChart.get('data')))
      .enter().append("svg:path")
      .attr("fill", function (d, i) {
        return thisChart.palette.color(i);
      })
      .attr("d", thisChart.get('arc'))

    );

  }

});