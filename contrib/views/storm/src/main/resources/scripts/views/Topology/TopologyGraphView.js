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

define(['require', 'utils/Globals'], function(require, Globals) {
  'use strict'

  var topologyGraphView = Marionette.LayoutView.extend({
    template: _.template('<div><canvas id="topoGraph" width="844" height="260"></div>'),

    initialize: function(options) {
      this.topologyId = options.id;
    },
    onRender: function() {
      var topology_data,
        that = this;
      var sys = arbor.ParticleSystem(20, 1000, 0.15, true, 55, 0.02, 0.6);
      sys.renderer = this.renderGraph('#topoGraph');
      sys.stop();

      Backbone.ajax({
        url: Globals.baseURL + "/api/v1/topology/" + this.topologyId + "/visualization",
        success: function(data, status, jqXHR) {
          if(_.isString(data)){
            data = JSON.parse(data);
          }
          topology_data = data;
          that.update_data(topology_data, sys);
          sys.renderer.signal_update();
          sys.renderer.redraw();
          that.rechoose(topology_data, sys, 'default')
        }
      });

    },

    renderGraph: function(elem) {
      var canvas = this.$(elem).get(0);
      var ctx = canvas.getContext("2d");
      var gfx = arbor.Graphics(canvas);
      var psys;

      var totaltrans = 0;
      var weights = {};
      var texts = {};
      var update = false;
      var that = this;
      var myRenderer = {
        init: function(system) {
          psys = system;
          psys.screenSize(canvas.width, canvas.height)
          psys.screenPadding(20);
          myRenderer.initMouseHandling();
        },

        signal_update: function() {
          update = true;
        },

        redraw: function() {
          if (!psys)
            return;

          if (update) {
            totaltrans = that.calculate_total_transmitted(psys);
            weights = that.calculate_weights(psys, totaltrans);
            texts = that.calculate_texts(psys, totaltrans);
            update = false;
          }



          ctx.fillStyle = "white";
          ctx.fillRect(0, 0, canvas.width, canvas.height);
          var x = 0;


          psys.eachEdge(function(edge, pt1, pt2) {

            var len = Math.sqrt(Math.pow(pt2.x - pt1.x, 2) + Math.pow(pt2.y - pt1.y, 2));
            var sublen = len - (Math.max(50, 20 + gfx.textWidth(edge.target.name)) / 2);
            var thirdlen = len / 3;
            var theta = Math.atan2(pt2.y - pt1.y, pt2.x - pt1.x);

            var newpt2 = {
              x: pt1.x + (Math.cos(theta) * sublen),
              y: pt1.y + (Math.sin(theta) * sublen)
            };

            var thirdpt = {
              x: pt1.x + (Math.cos(theta) * thirdlen),
              y: pt1.y + (Math.sin(theta) * thirdlen)
            }

            var weight = weights[edge.source.name + edge.target.name];

            if (!weights[edge.source.name + edge.target.name]) {
              totaltrans = that.calculate_total_transmitted(psys);
              weights = that.calculate_weights(psys, totaltrans);
            }

            ctx.strokeStyle = "rgba(0,0,0, .333)";
            ctx.lineWidth = 25 * weight + 5;
            ctx.beginPath();

            var arrlen = 15;
            ctx.moveTo(pt1.x, pt1.y);
            ctx.lineTo(newpt2.x, newpt2.y);
            ctx.lineTo(newpt2.x - arrlen * Math.cos(theta - Math.PI / 6), newpt2.y - arrlen * Math.sin(theta - Math.PI / 6));
            ctx.moveTo(newpt2.x, newpt2.y);
            ctx.lineTo(newpt2.x - arrlen * Math.cos(theta + Math.PI / 6), newpt2.y - arrlen * Math.sin(theta + Math.PI / 6));


            if (texts[edge.source.name + edge.target.name] == null) {
              totaltrans = calculate_total_transmitted(psys);
              texts = calculate_texts(psys, totaltrans);
            }

            gfx.text(texts[edge.source.name + edge.target.name], thirdpt.x, thirdpt.y + 10, {
              color: "black",
              align: "center",
              font: "Arial",
              size: 10
            })
            ctx.stroke();
          });

          psys.eachNode(function(node, pt) {
            var col;

            var real_trans = that.gather_stream_count(node.data[":stats"], "default", "600");

            if (node.data[":type"] === "bolt") {
              var cap = Math.min(node.data[":capacity"], 1);
              var red = Math.floor(cap * 225) + 30;
              var green = Math.floor(255 - red);
              var blue = Math.floor(green / 5);
              col = arbor.colors.encode({
                r: red,
                g: green,
                b: blue,
                a: 1
              });
            } else {
              col = "#0000FF";
            }

            var w = Math.max(55, 25 + gfx.textWidth(node.name));

            gfx.oval(pt.x - w / 2, pt.y - w / 2, w, w, {
              fill: col
            });
            gfx.text(node.name, pt.x, pt.y + 3, {
              color: "white",
              align: "center",
              font: "Arial",
              size: 12
            });
            gfx.text(node.name, pt.x, pt.y + 3, {
              color: "white",
              align: "center",
              font: "Arial",
              size: 12
            });

            gfx.text(_.isEqual(parseFloat(node.data[":latency"]).toFixed(2), 'NaN') ? "0.00 ms" : parseFloat(node.data[":latency"]).toFixed(2) + " ms", pt.x, pt.y + 17, {
              color: "white",
              align: "center",
              font: "Arial",
              size: 12
            });

          });

          // Draw heatmap
          var rect_x = canvas.width - 250,
            rect_y = canvas.height - 30,
            colorArr = ['#ff0000', '#F7BF43', '#F4EA47', '#A4CC45', '#1EE12D'];
          ctx.fillStyle = "grey";
          ctx.fillText("Heatmap", rect_x, rect_y - 5)
          ctx.rect(rect_x, rect_y, 245, canvas.height - 50);
          var grd = ctx.createLinearGradient(rect_x, rect_y, canvas.width - 5, rect_y);
          grd.addColorStop(0.000, colorArr[0]);
          grd.addColorStop(0.250, colorArr[1]);
          grd.addColorStop(0.500, colorArr[2]);
          grd.addColorStop(0.750, colorArr[3]);
          grd.addColorStop(1.000, colorArr[4]);
          ctx.fillStyle = grd;
          ctx.fillRect(rect_x, rect_y, 245, canvas.height - 50);

          //Draw legends
          var legendX = canvas.width - 140,
            legendY = canvas.height - 170,
            legendWidth = 30,
            legendY1 = canvas.height - 240,
            legendTextX = canvas.width - 105,
            legendTextY = canvas.height - 160,
            legendTextArr = ['0% - 20%', '21% - 40%', '41% - 60%', '61% - 80%', '81% - 100%'];

          for (var i = 0; i < colorArr.length; i++) {
            ctx.rect(legendX, legendY + (i * 25), legendWidth, legendY1);
            ctx.fillStyle = colorArr[4 - i];
            ctx.fillRect(legendX, legendY + (i * 25), legendWidth, legendY1);
            ctx.fillStyle = "black";
            ctx.fillText(legendTextArr[i], legendTextX, legendTextY + (i * 25));
          }

        },

        initMouseHandling: function() {
          var dragged = null;
          var clicked = false;
          var _mouseP;

          var handler = {
            clicked: function(e) {
              var pos = $(canvas).offset();
              _mouseP = arbor.Point(e.pageX - pos.left, e.pageY - pos.top);
              dragged = psys.nearest(_mouseP);

              if (dragged && dragged.node !== null) {
                dragged.node.fixed = true;
              }

              clicked = true;
              setTimeout(function() {
                clicked = false;
              }, 50);

              $(canvas).bind('mousemove', handler.dragged);
              $(window).bind('mouseup', handler.dropped);

              return false;
            },

            dragged: function(e) {

              var pos = $(canvas).offset();
              var s = arbor.Point(e.pageX - pos.left, e.pageY - pos.top);

              if (dragged && dragged.node != null) {
                var p = psys.fromScreen(s);
                dragged.node.p = p;
              }

              return false;

            },

            dropped: function(e) {
              // if (clicked) {
              //   if (dragged.distance < 50) {
              //     if (dragged && dragged.node != null) {
              //       window.location = dragged.node.data[":link"];
              //     }
              //   }
              // }

              if (dragged === null || dragged.node === undefined) return;
              if (dragged.node !== null) dragged.node.fixed = false;
              dragged.node.tempMass = 1000;
              dragged = null;
              $(canvas).unbind('mousemove', handler.dragged);
              $(window).unbind('mouseup', handler.dropped);
              _mouseP = null;
              return false;
            }

          }

          $(canvas).mousedown(handler.clicked);
        }
      };

      this.calculate_texts = function(psys, totaltrans) {
        var texts = {};
        psys.eachEdge(function(edge, pt1, pt2) {
          var text = "";
          for (var i = 0; i < edge.target.data[":inputs"].length; i++) {
            var stream = edge.target.data[":inputs"][i][":stream"];
            var sani_stream = edge.target.data[":inputs"][i][":sani-stream"];
            if (edge.target.data[":inputs"][i][":component"] === edge.source.name) {
              var stream_transfered = that.gather_stream_count(edge.source.data[":stats"], sani_stream, "600");
              text += stream + ": " + stream_transfered + ": " + (totaltrans > 0 ? Math.round((stream_transfered / totaltrans) * 100) : 0) + "%\n";

            }
          }

          texts[edge.source.name + edge.target.name] = text;
        });

        return texts;
      };

      this.calculate_weights = function(psys, totaltrans) {
        var weights = {};

        psys.eachEdge(function(edge, pt1, pt2) {
          var trans = 0;
          for (var i = 0; i < edge.target.data[":inputs"].length; i++) {
            var stream = edge.target.data[":inputs"][i][":sani-stream"];
            if (edge.target.data[":inputs"][i][":component"] === edge.source.name)
              trans += that.gather_stream_count(edge.source.data[":stats"], stream, "600");
          }
          weights[edge.source.name + edge.target.name] = (totaltrans > 0 ? trans / totaltrans : 0);
        });
        return weights;
      };

      this.calculate_total_transmitted = function(psys) {
        var totaltrans = 0;
        var countedmap = {}
        psys.eachEdge(function(node, pt, pt2) {
          if (!countedmap[node.source.name])
            countedmap[node.source.name] = {};

          for (var i = 0; i < node.target.data[":inputs"].length; i++) {
            var stream = node.target.data[":inputs"][i][":stream"];
            if (that.stream_checked(node.target.data[":inputs"][i][":sani-stream"])) {
              if (!countedmap[node.source.name][stream]) {
                if (node.source.data[":stats"]) {
                  var toadd = that.gather_stream_count(node.source.data[":stats"], node.target.data[":inputs"][i][":sani-stream"], "600");
                  totaltrans += toadd;
                }
                countedmap[node.source.name][stream] = true;
              }
            }
          }

        });

        return totaltrans;
      };

      this.stream_checked = function(stream) {
        // var checked = $("#" + stream).is(":checked");
        var checked = _.isEqual(stream.substr(0, 7), 'default');
        return checked;
      };

      this.gather_stream_count = function(stats, stream, time) {
        var transferred = 0;
        if (stats)
          for (var i = 0; i < stats.length; i++) {
            if (stats[i][":transferred"] != null) {
              var stream_trans = stats[i][":transferred"][time][stream];
              if (stream_trans != null)
                transferred += stream_trans;
            }
          }
        return transferred;
      };

      return myRenderer;
    },
    update_data: function(jdat, sys) {
      _.each(jdat, function(k, v) {
        if (sys.getNode(k))
          sys.getNode(k).data = v;
      });
    },

    has_checked_stream_input: function(inputs) {
      for (var i = 0; i < inputs.length; i++) {
        var x = this.stream_checked(inputs[i][":sani-stream"]);
        if (x)
          return true;
      }
      return false;
    },

    has_checked_stream_output: function(jdat, component) {
      var that = this;
      var ret = false;
      $.each(jdat, function(k, v) {
        for (var i = 0; i < v[":inputs"].length; i++) {
          if (that.stream_checked(v[":inputs"][i][":sani-stream"]) && v[":inputs"][i][":component"] == component)
            ret = true;
        }
      });
      return ret;
    },

    rechoose: function(jdat, sys, box) {
      var that = this;
      //Check each node in our json data to see if it has inputs from or outputs to selected streams. If it does, add a node for it.
      $.each(jdat, function(k, v) {
        if (that.has_checked_stream_input(v[":inputs"]) || that.has_checked_stream_output(jdat, k))
          sys.addNode(k, v);
      });

      //Check each node in our json data and add necessary edges based on selected components.
      $.each(jdat, function(k, v) {
        for (var i = 0; i < v[":inputs"].length; i++)
          if (_.isEqual(v[":inputs"][i][":sani-stream"].substr(0, 7), 'default')) {
            sys.addEdge(v[":inputs"][i][":component"], k, v);
          }
      });

      //Tell the particle system's renderer that it needs to update its labels, colors, widths, etc.
      sys.renderer.signal_update();
      sys.renderer.redraw();

    }

  });

  return topologyGraphView;
});