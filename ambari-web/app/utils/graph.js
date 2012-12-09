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
  drawJobTimeLine:function (json_str, w, h, element, legend_id, timeline_id) {
    var json = $.parseJSON(json_str);
    if (!json) {
      return new Error("unable to load data");
    }

    var graph = new Rickshaw.Graph({
      width:w,
      height:h,
      element:document.querySelector(element),
      renderer:'area',
      stroke:true,
      series:[
        {
          data:json[0],
          color:'green',
          name:'maps'
        },
        {
          data:json[1],
          color:'lightblue',
          name:'shuffles'
        },
        {
          data:json[2],
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

      graph:graph
    });
    xAxis.render();

    var yAxis = new Rickshaw.Graph.Axis.Y({

      graph:graph
    });
    yAxis.render();

    var hoverDetail = new Rickshaw.Graph.HoverDetail({
      graph:graph,
      yFormatter:function (y) {
        return Math.floor(y) + " tasks"
      }
    });

    var annotator = new Rickshaw.Graph.Annotate({
      graph:graph,
      element:document.getElementById(timeline_id)
    });
  },
  drawJobTasks:function (json_str, w, h, element, legend_id, timeline_id) {
    var json = $.parseJSON(json_str);
    if (!json) {
      return new Error("unable to load data");
    }

    var graph = new Rickshaw.Graph({
      width:w,
      height:h,
      element:document.querySelector(element),
      renderer:'scatterplot',
      stroke:true,
      series:[
        {
          data:json[0],
          color:'green',
          name:'node_local_map'
        },
        {
          data:json[1],
          color:'lightblue',
          name:'rack_local_map'
        },
        {
          data:json[2],
          color:'brown',
          name:'off_switch_map'
        },
        {
          data:json[3],
          color:'red',
          name:'reduce'
        },
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
      ticksTreatment:ticksTreatment,
    });
    xAxis.render();

    var yAxis = new Rickshaw.Graph.Axis.Y({
      graph:graph,
      ticksTreatment:ticksTreatment,
    });
    yAxis.render();

    var hoverDetail = new Rickshaw.Graph.HoverDetail({
      graph:graph,
      xFormatter:function (x) {
        return (x - json[4].submitTime) / 1000 + 's'
      },
      yFormatter:function (y) {
        return y / 1000 + 's'
      },
      formatter:function (series, x, y, formattedX, formattedY, d) {
        var swatch = '<span class="detail_swatch" style="background-color: ' + series.color + '"></span>';
        return swatch + d.label +
          '<br>Run-time: ' + formattedY + '<br>Wait-time: ' + formattedX;
      }

    });
    var annotator = new Rickshaw.Graph.Annotate({
      graph:graph,
      element:document.getElementById(timeline_id)
    });
    annotator.add(1337970759432, 'Lost tasktracker');
    graph.update();
  }
}
