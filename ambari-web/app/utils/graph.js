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


module.exports = {
  uniformSeries: function () {
    var series_min_length = 100000000;
    for (i=0; i<arguments.length; i++) {
      if (arguments[i].length < series_min_length) {
        series_min_length = arguments[i].length;
      }
    }
    for (i=0; i<arguments.length; i++) {
      if (arguments[i].length > series_min_length) {
        arguments[i].length = series_min_length;
      }
    }
  },
  /**
   * Get min, max for X and Y for provided series
   * @param series
   * @return {Object}
   */
  getExtInSeries: function(series) {
    var maxY = 0;
    var maxX = 0;
    var minX = 2147465647; // max timestamp value
    var minY = 2147465647;
    if (series.length > 0) {
      series.forEach(function(item){
        if (item.y > maxY) {
          maxY = item.y;
        }
        if (item.y < minY) {
          minY = item.y;
        }
        if (item.x > maxX) {
          maxX = item.x;
        }
        if (item.x < minX) {
          minX = item.x;
        }
      });
    }
    return {maxX: maxX, minX: minX, maxY: maxY, minY: minY};
  },
  /**
   * Get min, max for x and Y for all provided series
   * @param args array of series
   * @return {Object}
   */
  getExtInAllSeries: function(args) {
    var maxx = [];
    var minx = [];
    var maxy = [];
    var miny = [];
    for (var i = 0; i < args.length; i++) {
      var localExt = this.getExtInSeries(args[i]);
      maxx.push(localExt.maxX);
      minx.push(localExt.minX);
      maxy.push(localExt.maxY);
      miny.push(localExt.minY);
    }
    return {
      maxX: Math.max.apply(null, maxx),
      minX: Math.min.apply(null, minx),
      maxY: Math.max.apply(null, maxy),
      minY: Math.min.apply(null, miny)
    };
  },
  /**
   * Get coordinates for new circle in the graph
   * New circle needed to prevent cut on the borders of the graph
   * List of arguments - series arrays
   * @return {Object}
   */
  getNewCircle: function() {
    var ext = this.getExtInAllSeries(arguments);
    var newX;
    if (ext.minX != 2147465647) {
      newX = ext.maxX + Math.round((ext.maxX - ext.minX) * 0.2);
    }
    else {
      newX = (new Date()).getTime();
    }
    var newY = ext.maxY * 1.2;
    return {
      x: newX,
      y: newY,
      r: 0,
      io: 0
    };
  },
  /**
   *
   * @param map
   * @param shuffle
   * @param reduce
   * @param w
   * @param h
   * @param element
   * @param legend_id
   * @param timeline_id
   */
  drawJobTimeLine:function (map, shuffle, reduce, w, h, element, legend_id, timeline_id) {
    map = $.parseJSON(map);
    shuffle = $.parseJSON(shuffle);
    reduce = $.parseJSON(reduce);
    if (!map || !shuffle || !reduce) {
      console.warn('drawJobTimeLine');
      return;
    }
    this.uniformSeries(map, reduce, shuffle);
    var ext = this.getExtInAllSeries([map, reduce, shuffle]);
    var submitTime = ext.minX;
    var maxX = ext.maxX; // Used on X-axis time stamps

    var graph = new Rickshaw.Graph({
      width:w,
      height:h,
      element:document.querySelector(element),
      renderer:'area',
      interpolation: 'step-after',
      strokeWidth: 2,
      stroke:true,
      series:[
        {
          data:map,
          color:'green',
          name:'maps'
        },
        {
          data:shuffle,
          color:'lightblue',
          name:'shuffles'
        },
        {
          data:reduce,
          color:'steelblue',
          name:'reduces'
        }
      ]
      }
    );
    graph.render();

    var legend = new Rickshaw.Graph.Legend({
      graph:graph,
      element:document.getElementById(legend_id)
    });

    var shelving = new Rickshaw.Graph.Behavior.Series.Toggle({
      graph:graph,
      legend:legend
    });

    var order = new Rickshaw.Graph.Behavior.Series.Order({
      graph:graph,
      legend:legend
    });

    var highlight = new Rickshaw.Graph.Behavior.Series.Highlight({
      graph:graph,
      legend:legend
    });

    var xAxis = new Rickshaw.Graph.Axis.Time({
      graph:graph,
      timeUnit: {
        name: 'Custom',
        seconds: Math.round((maxX - submitTime) / 2),
        formatter: function(d) { return (new Date(d)).getTime() / 1000 - submitTime + 's'; }
      }
    });
    xAxis.render();

    var yAxis = new Rickshaw.Graph.Axis.Y({
      orientation: 'left',
      element: document.querySelector('#y-axis'),
      graph:graph
    });
    yAxis.render();

    var hoverDetail = new Rickshaw.Graph.HoverDetail({
      graph:graph,
      yFormatter:function (y) {
        return Math.floor(y) + " tasks"
      }
    });

    /*var annotator = new Rickshaw.Graph.Annotate({
      graph:graph,
      //element:document.getElementById(timeline_id)
    });*/
  },
  /**
   *
   * @param mapNodeLocal
   * @param mapRackLocal
   * @param mapOffSwitch
   * @param reduceOffSwitch
   * @param submitTime
   * @param w
   * @param h
   * @param element
   * @param legend_id
   * @param timeline_id
   */
  drawJobTasks:function (mapNodeLocal, mapRackLocal, mapOffSwitch, reduceOffSwitch, submitTime, w, h, element, legend_id, timeline_id) {
    mapNodeLocal = $.parseJSON(mapNodeLocal);
    mapRackLocal = $.parseJSON(mapRackLocal);
    mapOffSwitch = $.parseJSON(mapOffSwitch);
    reduceOffSwitch = $.parseJSON(reduceOffSwitch);
    if (!mapNodeLocal || !mapRackLocal || !mapOffSwitch || !reduceOffSwitch) {
      console.warn('drawJobTasks');
      return;
    }
    this.uniformSeries(mapNodeLocal, mapRackLocal, mapOffSwitch, reduceOffSwitch);
    var newC = this.getNewCircle(mapNodeLocal, mapRackLocal, mapOffSwitch, reduceOffSwitch);
    var ext = this.getExtInAllSeries([mapNodeLocal, mapRackLocal, mapOffSwitch, reduceOffSwitch]);
    var maxX = ext.maxX; // Used on X-axis time stamps
    mapNodeLocal.push(newC);
    mapRackLocal.push(newC);
    mapOffSwitch.push(newC);
    reduceOffSwitch.push(newC);
    var graph = new Rickshaw.Graph({
      width:w,
      height:h,
      element:document.querySelector(element),
      renderer:'scatterplot',
      stroke:true,
      series:[
        {
          data:mapNodeLocal,
          color:'green',
          name:'node_local_map'
        },
        {
          data:mapRackLocal,
          color:'#66B366',
          name:'rack_local_map'
        },
        {
          data:mapOffSwitch,
          color:'brown',
          name:'off_switch_map'
        },
        {
          data:reduceOffSwitch,
          color:'steelblue',
          name:'reduce'
        }
      ]
    });
    graph.render();
    var legend = new Rickshaw.Graph.Legend({
      graph:graph,
      element:document.getElementById(legend_id)
    });

    var shelving = new Rickshaw.Graph.Behavior.Series.Toggle({
      graph:graph,
      legend:legend
    });

    var order = new Rickshaw.Graph.Behavior.Series.Order({
      graph:graph,
      legend:legend
    });

    var highlight = new Rickshaw.Graph.Behavior.Series.Highlight({
      graph:graph,
      legend:legend
    });

    var ticksTreatment = 'glow';

    var xAxis = new Rickshaw.Graph.Axis.Time({
      graph:graph,
      timeUnit: {
        name: 'Custom',
        seconds: Math.round((maxX - submitTime) / 2),
        formatter: function(d) { return (new Date(d)).getTime() / 1000 - submitTime + 's'; }
      },
      ticksTreatment:ticksTreatment
    });
    xAxis.render();

    var yAxis = new Rickshaw.Graph.Axis.Y({
      graph:graph,
      ticksTreatment:ticksTreatment,
      orientation: 'left',
      element: document.querySelector('#y-axis2'),
      tickFormat: function(y) { return y / 1000 + 's' }
    });
    yAxis.render();

    var hoverDetail = new Rickshaw.Graph.HoverDetail({
      graph:graph,
      xFormatter:function (x) {
        return (x - submitTime) + 's'
      },
      yFormatter:function (y) {
        return y / 1000 + 's'
      },
      formatter:function (series, x, y, formattedX, formattedY, d) {
        var bytesFormatter = function(y) {
          if (y >= 1125899906842624)  { return Math.floor(10 * y / 1125899906842624)/10 + " PB" }
          else if (y >= 1099511627776){ return Math.floor(10 * y / 1099511627776)/10 + " TB" }
          else if (y >= 1073741824)   { return Math.floor(10 * y / 1073741824)/10 + " GB" }
          else if (y >= 1048576)      { return Math.floor(10 * y / 1048576)/10 + " MB" }
          else if (y >= 1024)         { return Math.floor(10 * y / 1024)/10 + " KB" }
          else                        { return y + " B"}
        };
        var swatch = '<span class="detail_swatch" style="background-color: ' + series.color + '"></span>';
        return swatch + d.value.label +
          '<br>Run-time: ' + formattedY + '<br>Wait-time: ' + formattedX +
          '<br>I/O: ' + bytesFormatter(d.value.io) + '<br>Status: ' + d.value.status;
      }

    });
  }
}
