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
var string_utils = require('utils/string_utils');

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
App.ChartLinearTimeView = Ember.View.extend({
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

      _popupGraph: null,

      _seriesProperties: null,

      renderer: 'area',

      popupSuffix: '-popup',

      isPopup: false,

      isReady: false,

      isPopupReady: false,

      hasData: true,

      /**
       * Color palette used for this chart
       *
       * @private
       * @type String[]
       */
       /*
      _paletteScheme: [ 'rgba(181,182,169,0.4)', 'rgba(133,135,114,0.4)',
          'rgba(120,95,67,0.4)', 'rgba(150,85,126,0.4)',
          'rgba(70,130,180,0.4)', 'rgba(0,255,204,0.4)',
          'rgba(255,105,180,0.4)', 'rgba(101,185,172,0.4)',
          'rgba(115,192,58,0.4)', 'rgba(203,81,58,0.4)' ].reverse(),
      */

      selector: function () {
        return '#' + this.get('elementId');
      }.property('elementId'),

      didInsertElement: function () {
        this.loadData();
        this.registerGraph();
      },
      registerGraph: function(){
        var graph = {
          name: this.get('title'),
          id: this.get('elementId'),
          popupId: this.get('id')
        };
        App.router.get('updateController.graphs').push(graph);
      },

      loadData: function() {
        var validUrl = this.get('url');
        if (validUrl) {
          var hash = {};
          hash.url = validUrl;
          hash.type = 'GET';
          hash.dataType = 'json';
          hash.contentType = 'application/json; charset=utf-8';
          hash.context = this;
          hash.success = this._refreshGraph,
           hash.error = function(xhr, textStatus, errorThrown){
            this.set('isReady', true);
            if (xhr.readyState == 4 && xhr.status) {
              textStatus = xhr.status + " " + textStatus;
            }
            this._showMessage('warn', this.t('graphs.error.title'), this.t('graphs.error.message').format(textStatus, errorThrown));
            this.set('isPopup', false);
            this.set('hasData', false);
          }
          jQuery.ajax(hash);
        }
      },
      
      /**
       * Shows a yellow warning message in place of the chart.
       * 
       * @param type  Can be any of 'warn', 'error', 'info', 'success'
       * @param title Bolded title for the message
       * @param message String representing the message
       * @type: Function
       */
      _showMessage: function(type, title, message){
        var chartOverlayId = '#' + this.id + '-chart';
        if (this.get('isPopup')) {
          chartOverlayId += this.get('popupSuffix');
        }
        var typeClass;
        switch (type) {
          case 'error':
            typeClass = 'alert-error';
            break;
          case 'success':
            typeClass = 'alert-success';
            break;
          case 'info':
            typeClass = 'alert-info';
            break;
          default:
            typeClass = '';
            break;
        }
        $(chartOverlayId).html('');
        $(chartOverlayId).append('<div class=\"alert '+typeClass+'\"><strong>'+title+'</strong> '+message+'</div>');
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
      yAxisFormatter: function(y) {
        var value = Rickshaw.Fixtures.Number.formatKMBT(y);
        if (value == '') return '0';
        value = String(value);
        var c = value[value.length - 1];
        if (!isNaN(parseInt(c))) {
          // c is digit
          value = parseFloat(value).toFixed(3).replace(/0+$/, '').replace(/\.$/, '');
        }
        else {
          // c in not digit
          value = parseFloat(value.substr(0, value.length - 1)).toFixed(3).replace(/0+$/, '').replace(/\.$/, '') + c;
        }
        return value;
      },

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
   * Check whether seriesData is correct data for chart drawing
   * @param seriesData
   * @return {Boolean}
   */
      checkSeries : function(seriesData){
        if(!seriesData || !seriesData.length){
          return false;
        }
        var result = true;
        seriesData.forEach(function(item){
          if(!item.data.length || !item.data[0] || typeof item.data[0].x === 'undefined'){
            result = false;
          }
        });
        return result;
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

        if (this.checkSeries(seriesData)) {
          //if graph opened as modal popup
          var popup_path = $("#" + this.id + "-container" + this.get('popupSuffix'));
          if(popup_path.length) {
            popup_path.children().each(function () {
              $(this).children().remove();
            });
            this.set('isPopup', true);
          }
          else {
            $("#" + this.id + "-container").children().each(function (index, value) {
              $(value).children().remove();
            });
          }
          this.draw(seriesData);
          this.set('hasData', true);
        }
        else {
          this.set('isReady', true);
          this._showMessage('info', this.t('graphs.noData.title'), this.t('graphs.noData.message'));
          this.set('isPopup', false);
          this.set('hasData', false);
        }
      },
      
      /**
       * Returns a custom time unit for the graph's X axis. This is needed
       * as Rickshaw's default time X axis uses UTC time, which can be confusing
       * for users expecting locale specific time. This value defaults to
       * App.ChartLinearTimeView.FifteenMinuteTimeUnit.
       * 
       * If <code>null</code> is returned, Rickshaw's default time unit is used.
       * 
       * @type Function
       * @return Rickshaw.Fixtures.Time
       * @default App.ChartLinearTimeView.FifteenMinuteTimeUnit
       */
      localeTimeUnit: function(){
        return App.ChartLinearTimeView.FifteenMinuteTimeUnit;
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
      },

      draw: function(seriesData) {
        var isPopup = this.get('isPopup');
        var p = '';
        if (isPopup) {
          p = this.get('popupSuffix');
        }
        var palette = new Rickshaw.Color.Palette({ scheme: 'munin'});

        // var palette = new Rickshaw.Color.Palette({
        //   scheme: this._paletteScheme
        // });

        var self = this;
        var series_min_length = 100000000;
        seriesData.forEach(function (series, index) {
          var seriesColor = self.colorForSeries(series);
          if (seriesColor == null) {
            seriesColor = palette.color();
          }
          series.color = seriesColor;
          series.stroke = 'rgba(0,0,0,0.3)';
          if (isPopup) {
            // calculate statistic data for popup legend
            var avg = 0;
            var min = Number.MAX_VALUE;
            var max = Number.MIN_VALUE;
            for (var i = 0; i < series.data.length; i++) {
              avg += series.data[i]['y'];
              if (series.data[i]['y'] < min) {
                min = series.data[i]['y'];
              }
              else {
                if (series.data[i]['y'] > max) {
                  max = series.data[i]['y'];
                }
              }
            }
            series.name = string_utils.pad(series.name, 30, '&nbsp;', 2) + string_utils.pad('min', 5, '&nbsp;', 3) + string_utils.pad(this.get('yAxisFormatter')(min), 12, '&nbsp;', 3) + string_utils.pad('avg', 5, '&nbsp;', 3) + string_utils.pad(this.get('yAxisFormatter')(avg/series.data.length), 12, '&nbsp;', 3) + string_utils.pad('max', 12, '&nbsp;', 3) + string_utils.pad(this.get('yAxisFormatter')(max), 5, '&nbsp;', 3);
          }
          if (series.data.length < series_min_length) {
            series_min_length = series.data.length;
          }
        }.bind(this));
        seriesData.forEach(function(series, index) {
          if (series.data.length > series_min_length) {
            series.data.length = series_min_length;
          }
        });
        var chartId = "#" + this.id + "-chart" + p;
        var chartOverlayId = "#" + this.id + "-container" + p;
        var xaxisElementId = "#" + this.id + "-xaxis" + p;
        var yaxisElementId = "#" + this.id + "-yaxis" + p;
        var legendElementId = "#" + this.id + "-legend" + p;

        var chartElement = document.querySelector(chartId);
        var overlayElement = document.querySelector(chartOverlayId);
        var xaxisElement = document.querySelector(xaxisElementId);
        var yaxisElement = document.querySelector(yaxisElementId);
        var legendElement = document.querySelector(legendElementId);

        var strokeWidth = 1;
        if (this.get('renderer') != 'area') {
          strokeWidth = 2;
        }

        var height = 150;
        var width = 400;
        if (isPopup) {
          height = 180;
          width = 670;
        } else {
          // If not in popup, the width could vary.
          // We determine width based on div's size.
          var thisElement = this.get('element');
          if (thisElement!=null) {
            var calculatedWidth = $(thisElement).width();
            if (calculatedWidth > 10) {
              width = calculatedWidth;
            }
          }
        }
        var _graph = new Rickshaw.Graph({
          height: height,
          width: width,
          element: chartElement,
          series: seriesData,
          interpolation: 'step-after',
          stroke: true,
          renderer: this.get('renderer'),
          strokeWidth: strokeWidth
        });
        if (this.get('renderer') === 'area') {
          _graph.renderer.unstack = false;
        }

        xAxis = new Rickshaw.Graph.Axis.Time({
          graph: _graph,
          timeUnit: this.localeTimeUnit()
        });

        var orientation = 'right';
        if (isPopup) {
          orientation = 'left';
        }
        yAxis = new Rickshaw.Graph.Axis.Y({
          tickFormat: this.yAxisFormatter,
          element: yaxisElement,
          orientation: orientation,
          graph: _graph
        });

        var legend = new Rickshaw.Graph.Legend({
          graph: _graph,
          element: legendElement
        });

        if (!isPopup) {
          overlayElement.addEventListener('mousemove', function () {
            $(xaxisElement).removeClass('hide');
            $(legendElement).removeClass('hide');
            $(chartElement).children("div").removeClass('hide');
          });
          overlayElement.addEventListener('mouseout', function () {
            $(legendElement).addClass('hide');
          });
          _graph.onUpdate(function () {
            $(legendElement).addClass('hide');
          });
        }

       var shelving = new Rickshaw.Graph.Behavior.Series.Toggle({
          graph: _graph,
          legend: legend
        });

        var order = new Rickshaw.Graph.Behavior.Series.Order({
          graph: _graph,
          legend: legend
        });
        //show the graph when it's loaded
        _graph.onUpdate(function(){
          self.set('isReady', true);
        });
        _graph.render();

        if (isPopup) {
          var self = this;
          var hoverDetail = new Rickshaw.Graph.HoverDetail({
            graph: _graph,
            yFormatter:function (y) {
              return self.yAxisFormatter(y);
            },
            xFormatter:function (x) {
              return (new Date(x * 1000)).toLocaleTimeString();
            },
            formatter:function (series, x, y, formattedX, formattedY, d) {
              return formattedY + '<br />' + formattedX;
            }
          });
        }

        if (isPopup) {
          var self = this;
          // In popup save selected metrics and show only them after data update
          _graph.series.forEach(function(series, index) {
            if (self.get('_seriesProperties') !== null && self.get('_seriesProperties')[index] !== null) {
              if(self.get('_seriesProperties')[self.get('_seriesProperties').length - index - 1].length > 1) {
                $('#'+self.get('id')+'-container'+self.get('popupSuffix')+' a.action:eq('+(self.get('_seriesProperties').length - index - 1)+')').parent('li').addClass('disabled');
                series.disable();
              }
            }
          });
          //show the graph when it's loaded
          _graph.onUpdate(function(){
            self.set('isPopupReady', true);
          });
          _graph.update();

          $('li.line').click(function() {
            var series = [];
            $('#'+self.get('id')+'-container'+self.get('popupSuffix')+' a.action').each(function(index, v) {
              series[index] = v.parentNode.classList;
            });
            self.set('_seriesProperties', series);
          });

          this.set('_popupGraph', _graph);
        }
        else {
          this.set('_graph', _graph);
        }
        this.set('isPopup', false);
      },


      showGraphInPopup: function() {
        if(!this.get('hasData')){
          return;
        }

        this.set('isPopup', true);
        var self = this;
        App.ModalPopup.show({
          template: Ember.Handlebars.compile([
            '<div class="modal-backdrop"></div><div class="modal modal-graph-line" id="modal" tabindex="-1" role="dialog" aria-labelledby="modal-label" aria-hidden="true">',
            '<div class="modal-header">',
            '<a class="close" {{action onClose target="view"}}>x</a>',
            '<h3 id="modal-label">',
            '{{#if headerClass}}{{view headerClass}}',
            '{{else}}{{header}}{{/if}}',
            '</h3>',
            '</div>',
            '<div class="modal-body">',
            '{{#if bodyClass}}{{view bodyClass}}',
            '{{else}}',
              '<div class="screensaver no-borders chart-container" {{bindAttr class="view.isReady:hide"}} ></div>',
              '<div id="'+this.get('id')+'-container'+this.get('popupSuffix')+'" class="chart-container chart-container'+this.get('popupSuffix')+' hide" {{bindAttr class="view.isReady:show"}} >',
                '<div id="'+this.get('id')+'-yaxis'+this.get('popupSuffix')+'" class="'+this.get('id')+'-yaxis chart-y-axis"></div>',
                '<div id="'+this.get('id')+'-xaxis'+this.get('popupSuffix')+'" class="'+this.get('id')+'-xaxis chart-x-axis"></div>',
                '<div id="'+this.get('id')+'-legend'+this.get('popupSuffix')+'" class="'+this.get('id')+'-legend chart-legend"></div>',
                '<div id="'+this.get('id')+'-chart'+this.get('popupSuffix')+'" class="'+this.get('id')+'-chart chart"></div>',
                '<div id="'+this.get('id')+'-title'+this.get('popupSuffix')+'" class="'+this.get('id')+'-title chart-title">{{view.title}}</div>'+
              '</div>',
            '{{/if}}',
            '</div>',
            '<div class="modal-footer">',
            '{{#if view.primary}}<a class="btn btn-success" {{action onPrimary target="view"}}>{{view.primary}}</a>{{/if}}',
            '</div>',
            '</div>'
          ].join('\n')),

          header: this.get('title'),
          self: self,
          isReady: function(){
            return this.get('self.isPopupReady');
          }.property('self.isPopupReady'),
          primary: 'OK',
          onPrimary: function() {
            this.hide();
            self.set('isPopup', false);
          }
        });
        Ember.run.next(function() {
          self.loadData();
          self.set('isPopupReady', false);
        });
      }
    });

/**
 * A formatter which will turn a number into computer storage sizes of the
 * format '23 GB' etc.
 * 
 * @type Function
 */
App.ChartLinearTimeView.BytesFormatter = function (y) {
  if (y == 0) return '0 B';
  var value = Rickshaw.Fixtures.Number.formatBase1024KMGTP(y);
  if (!y || y.length < 1) {
    value = '0 B';
  }
  else {
    if ("number" == typeof value) {
      value = String(value);
    }
    if ("string" == typeof value) {
      value = value.replace(/\.\d(\d+)/, function($0, $1){ // Remove only 1-digit after decimal part
        return $0.replace($1, '');
      }); 
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
    value = '0 %';
  } else {
    value = value.toFixed(3).replace(/0+$/, '').replace(/\.$/, '') + '%';
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
    value = '0 ms';
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
      value = millis.toFixed(3).replace(/0+$/, '').replace(/\.$/, '') + ' ms';
    } else {
      value = millis.toFixed(3).replace(/0+$/, '').replace(/\.$/, '') + ' ms';
    }
  }
  return value;
};

/**
 * A time unit which can be used for showing 15 minute intervals on X axis.
 * 
 * @type Rickshaw.Fixtures.Time
 */
App.ChartLinearTimeView.FifteenMinuteTimeUnit = {
  name: '15 minute',
  seconds: 60 * 15,
  formatter: function (d) {
    return d.toLocaleString().match(/(\d+:\d+):/)[1];
  }
};