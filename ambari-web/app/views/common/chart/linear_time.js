/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

/**
 * @class
 * 
 * This is a view which GETs data from a URL and shows it as a time based line
 * graph. Time is shown on the X axis with data series shown on Y axis. It
 * optionally also has the ability to auto refresh itself over a given time
 * interval.
 * 
 * This is an abstract class which is meant to be extended.
 * 
 * Extending classes should override the following:
 * <ul>
 * <li>url - from where the data can be retrieved
 * <li>title - Title to be displayed when showing the chart
 * <li>id - which uniquely identifies this chart in any page
 * <li>#transformToSeries(jsonData) - function to map server data into graph
 * series
 * </ul>
 * 
 * Extending classes could optionally override the following:
 * <ul>
 * <li>#colorForSeries(series) - function to get custom colors per series
 * </ul>
 * 
 * @extends Ember.Object
 * @extends Ember.View
 */
App.ChartLinearTimeView = Ember.View
    .extend({
      templateName: require('templates/main/charts/linear_time'),

      /**
       * The URL from which data can be retrieved.
       * 
       * This property must be provided for the graph to show properly.
       * 
       * @type String
       * @default null
       */
      url: null,

      /**
       * A unique ID for this chart.
       * 
       * @type String
       * @default null
       */
      id: null,

      /**
       * Title to be shown under the chart.
       * 
       * @type String
       * @default null
       */
      title: null,

      /**
       * @private
       * 
       * @type Rickshaw.Graph
       * @default null
       */
      _graph: null,

      /**
       * Color palette used for this chart
       * 
       * @private
       * @type String[]
       */
      _paletteScheme: [ 'rgba(181,182,169,0.4)', 'rgba(133,135,114,0.4)',
          'rgba(120,95,67,0.4)', 'rgba(150,85,126,0.4)',
          'rgba(70,130,180,0.4)', 'rgba(0,255,204,0.4)',
          'rgba(255,105,180,0.4)', 'rgba(101,185,172,0.4)',
          'rgba(115,192,58,0.4)', 'rgba(203,81,58,0.4)' ].reverse(),

      init: function () {
        this._super();
      },

      selector: function () {
        return '#' + this.get('elementId');
      }.property('elementId'),

      didInsertElement: function () {
        this._super();
        if (this.url != null) {
          var hash = {};
          hash.url = this.url;
          hash.type = 'GET';
          hash.dataType = 'json';
          hash.contentType = 'application/json; charset=utf-8';
          hash.context = this;
          hash.success = this._refreshGraph;
          jQuery.ajax(hash);
        }
      },

      /**
       * Transforms the JSON data retrieved from the server into the series
       * format that Rickshaw.Graph understands.
       * 
       * The series object is generally in the following format: [ { name :
       * "Series 1", data : [ { x : 0, y : 0 }, { x : 1, y : 1 } ] } ]
       * 
       * Extending classes should override this method.
       * 
       * @param jsonData
       *          Data retrieved from the server
       * @type: Function
       * 
       */
      transformToSeries: function (jsonData) {
        return [ {
          name: "Series 1",
          data: [ {
            x: 0,
            y: 0
          }, {
            x: 1,
            y: 1
          } ]
        } ]
      },

      /**
       * Provides the formatter to use in displaying Y axis.
       * 
       * The default is Rickshaw.Fixtures.Number.formatKMBT which shows 10K,
       * 300M etc.
       * 
       * @type Function
       */
      yAxisFormatter: Rickshaw.Fixtures.Number.formatKMBT,

      /**
       * Provides the color (in any HTML color format) to use for a particular
       * series.
       * 
       * @param series
       *          Series for which color is being requested
       * @return color String. Returning null allows this chart to pick a color
       *         from palette.
       * @default null
       * @type Function
       */
      colorForSeries: function (series) {
        return null;
      },

      /**
       * @private
       * 
       * Refreshes the graph with the latest JSON data.
       * 
       * @type Function
       */
      _refreshGraph: function (jsonData) {
        var seriesData = this.transformToSeries(jsonData);
        if (seriesData instanceof Array) {
          var palette = new Rickshaw.Color.Palette({
            scheme: this._paletteScheme
          });
          seriesData.forEach(function (series) {
            series.color = this.colorForSeries(series) || palette.color();
            series.stroke = 'rgba(0,0,0,0.3)';
          }.bind(this));
        }
        if (this._graph == null) {
          var chartId = "#" + this.id + "-chart";
          var chartOverlayId = "#" + this.id + "-overlay";
          var xaxisElementId = "#" + this.id + "-xaxis";
          var yaxisElementId = "#" + this.id + "-yaxis";
          var chartElement = document.querySelector(chartId);
          var overlayElement = document.querySelector(chartOverlayId);
          var xaxisElement = document.querySelector(xaxisElementId);
          var yaxisElement = document.querySelector(yaxisElementId);

          this._graph = new Rickshaw.Graph({
            height: 150,
            element: chartElement,
            series: seriesData,
            interpolation: 'step-after',
            stroke: true,
            renderer: 'area',
            strokeWidth: 1
          });
          this._graph.renderer.unstack = true;

          xAxis = new Rickshaw.Graph.Axis.Time({
            graph: this._graph
          });
          yAxis = new Rickshaw.Graph.Axis.Y({
            tickFormat: this.yAxisFormatter,
            element: yaxisElement,
            graph: this._graph
          });

          overlayElement.addEventListener('mousemove', function () {
            $(xaxisElement).removeClass('hide');
            $(yaxisElement).removeClass('hide');
            $(chartElement).children("div").removeClass('hide');
          });
          overlayElement.addEventListener('mouseout', function () {
            $(xaxisElement).addClass('hide');
            $(yaxisElement).addClass('hide');
            $(chartElement).children("div").addClass('hide');
          });
          // Hide axes
          this._graph.onUpdate(function () {
            $(xaxisElement).addClass('hide');
            $(yaxisElement).addClass('hide');
            $(chartElement).children('div').addClass('hide');
          });

          new Rickshaw.Graph.Legend({
            graph: this._graph,
            element: xaxisElement
          });

          // The below code will be needed if we ever use curve
          // smoothing in our graphs. (see rickshaw defect below)
          // this._graph.onUpdate(jQuery.proxy(function () {
          // this._adjustSVGHeight();
          // }, this));
        }
        this._graph.render();

      },

      /**
       * @private
       * 
       * When a graph is given a particular width and height,the lines are drawn
       * in a slightly bigger area thereby chopping off some of the UI. Hence
       * after the rendering, we adjust the SVGs size in the DOM to compensate.
       * 
       * Opened https://github.com/shutterstock/rickshaw/issues/141
       * 
       * @type Function
       */
      _adjustSVGHeight: function () {
        if (this._graph && this._graph.element
            && this._graph.element.firstChild) {
          var svgElement = this._graph.element.firstChild;
          svgElement.setAttribute('height', $(this._graph.element).height()
              + "px");
          svgElement.setAttribute('width', $(this._graph.element).width()
              + "px");
        }
      }
    });

/**
 * A formatter which will turn a number into computer storage sizes of the
 * format '23 GB' etc.
 * 
 * @type Function
 */
App.ChartLinearTimeView.BytesFormatter = function (y) {
  var value = Rickshaw.Fixtures.Number.formatBase1024KMGTP(y);
  if (!y || y.length < 1) {
    value = '';
  } else {
    if ("number" == typeof value) {
      value = String(value);
    }
    if ("string" == typeof value) {
      value = value.replace(/\.\d+/, ''); // Remove decimal part
      // Either it ends with digit or ends with character
      value = value.replace(/(\d$)/, '$1 '); // Ends with digit like '120'
      value = value.replace(/([a-zA-Z]$)/, ' $1'); // Ends with character like
      // '120M'
      value = value + 'B'; // Append B to make B, MB, GB etc.
    }
  }
  return value;
};

/**
 * A formatter which will turn a number into percentage display like '42%'
 * 
 * @type Function
 */
App.ChartLinearTimeView.PercentageFormatter = function (percentage) {
  var value = percentage;
  if (!value || value.length < 1) {
    value = '';
  } else {
    value = value + '%';
  }
  return value;
};

/**
 * A formatter which will turn elapsed time into display time like '50 ms',
 * '5s', '10 m', '3 hr' etc. Time is expected to be provided in milliseconds.
 * 
 * @type Function
 */
App.ChartLinearTimeView.TimeElapsedFormatter = function (millis) {
  var value = millis;
  if (!value || value.length < 1) {
    value = '';
  } else if ("number" == typeof millis) {
    var seconds = millis > 1000 ? Math.round(millis / 1000) : 0;
    var minutes = seconds > 60 ? Math.round(seconds / 60) : 0;
    var hours = minutes > 60 ? Math.round(minutes / 60) : 0;
    var days = hours > 24 ? Math.round(hours / 24) : 0;
    if (days > 0) {
      value = days + ' d';
    } else if (hours > 0) {
      value = hours + ' hr';
    } else if (minutes > 0) {
      value = minutes + ' m';
    } else if (seconds > 0) {
      value = seconds + ' s';
    } else if (millis > 0) {
      value = millis + ' ms';
    } else {
      value = millis + ' ms';
    }
  }
  return value;
};
