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
  durationFormatter:function(d) {
      if (d==0) { return "0" }
      var seconds = Math.floor(parseInt(d) / 1000);
      if ( seconds < 60 )
        return seconds + "s";
      var minutes = Math.floor(seconds / 60);
      if ( minutes < 60 ) {
        var x = seconds - 60*minutes;
        return minutes + "m" + (x==0 ? "" : " " + x + "s");
      }
      var hours = Math.floor(minutes / 60);
      if ( hours < 24 ) {
        var x = minutes - 60*hours;
        return hours + "h" + (x==0 ? "" : " " + x + "m");
      }
      var days = Math.floor(hours / 24);
      if ( days < 7 ) {
        var x = hours - 24*days;
        return days + "d " + (x==0 ? "" : " " + x + "h");
      }
      var weeks = Math.floor(days / 7);
      var x = days - 7*weeks;
      return weeks + "w " + (x==0 ? "" : " " + x + "d");
  },
  bytesFormatter:function(y) {
    if (y >= 1125899906842624)  { return Math.floor(10 * y / 1125899906842624)/10 + " PB" }
    else if (y >= 1099511627776){ return Math.floor(10 * y / 1099511627776)/10 + " TB" }
    else if (y >= 1073741824)   { return Math.floor(10 * y / 1073741824)/10 + " GB" }
    else if (y >= 1048576)      { return Math.floor(10 * y / 1048576)/10 + " MB" }
    else if (y >= 1024)         { return Math.floor(10 * y / 1024)/10 + " KB" }
    else                        { return y + " B"}
  },
  addSeries:function(svgg,series,color,xscale,yscale,margin,startTime,dotInfo) {
    if (series.length==0) return;
    var self = this;
    var g = svgg.append("svg:g").selectAll("g")
      .data(series)
      .enter().append("svg:g")
      .attr("transform", "translate(0,"+margin+")");
    g.append("svg:circle")
      .attr("r",function(d) {return d.r;})
      .attr("cx",function(d) {return xscale(d.x);})
      .attr("cy",function(d) {return yscale(d.y);})
      .style("fill",color)
      .style("fill-opacity",0.8)
      .style("stroke",d3.interpolateRgb(color, 'black')(0.125))
      .append("title")
      .text(function(d) { return dotInfo[Math.round(xscale(d.x))][Math.round(yscale(d.y))]; });
    g.append("svg:line")
      .attr("x1", function(d) { return xscale(d.x)+d.r; } )
      .attr("x2", function(d) { return xscale(d.x+d.y); } )
      .attr("y1", function(d) { return yscale(d.y); } )
      .attr("y2", function(d) { return yscale(d.y); } )
      .style("stroke",d3.interpolateRgb(color, 'black')(0.125))
      .style("stroke-width",2)
      .append("title")
      .text(function(d) { return dotInfo[Math.round(xscale(d.x))][Math.round(yscale(d.y))]; });
  },
  /**
   *
   * @param mapNodeLocal
   * @param mapRackLocal
   * @param mapOffSwitch
   * @param reduceOffSwitch
   * @param startTime
   * @param endTime
   * @param svgw
   * @param svgh
   * @param element
   */
  drawJobTasks:function (mapNodeLocal, mapRackLocal, mapOffSwitch, reduceOffSwitch, startTime, endTime, svgw, svgh, element) {
    var rmax = 24; // default value
    var axisHeight = 24;
    var margin = {"vertical":10, "horizontal":50};
    var w = svgw - 2*margin.horizontal;
    var h = svgh - 2*margin.vertical;
    var x = d3.time.scale.utc()
      .domain([startTime, endTime])
      .range([0, w]);
    var xrel = d3.time.scale()
      .domain([0, endTime-startTime])
      .range([0, w]);
    // create axes
    var topAxis = d3.svg.axis()
      .scale(x)
      .orient("bottom");
    var self = this;
    var bottomAxis = d3.svg.axis()
      .scale(xrel)
      .orient("bottom")
      .tickFormat(function(d) {return self.durationFormatter(d.getTime())});

    var svg = d3.select("div#" + element).append("svg:svg")
      .attr("width", svgw+"px")
      .attr("height", svgh+"px");
    var svgg = svg.append("g")
      .attr("transform", "translate("+margin.horizontal+","+margin.vertical+")");

    svgg.append("g")
      .attr("class", "x axis top")
      .call(topAxis);
    svgg.append("g")
      .attr("class", "x axis bottom")
      .call(bottomAxis)
      .attr("transform", "translate(0,"+(h-axisHeight)+")");

    var ymax = 0;
    if (mapNodeLocal.length > 0)
      ymax = Math.max(ymax, d3.max(mapNodeLocal, function(d) { return d.y; } ));
    if (mapRackLocal.length > 0)
      ymax = Math.max(ymax, d3.max(mapRackLocal, function(d) { return d.y; } ));
    if (mapOffSwitch.length > 0)
      ymax = Math.max(ymax, d3.max(mapOffSwitch, function(d) { return d.y; } ));
    if (reduceOffSwitch.length > 0)
      ymax = Math.max(ymax, d3.max(reduceOffSwitch, function(d) { return d.y; } ));

    var y = d3.scale.linear()
      .domain([0, ymax])
      .range([h-2*axisHeight-rmax, 0]);

    var yAxis = d3.svg.axis()
      .scale(y)
      .orient("left")
      .tickFormat(self.durationFormatter);
 
    svgg.append("svg:g")
      .attr("class", "y axis")
      .call(yAxis)
      .attr("transform", "translate(0,"+(axisHeight+rmax)+")")
      .append("text")
      .attr("transform", "rotate(-90)")
      .attr("x", -(h-2*axisHeight-rmax)/2)
      .attr("y", -margin.horizontal + 11)
      .attr("class", "axislabel")
      .text("Task Attempt Duration");


    var dotInfo = [];
    var mapDotInfo = function(d) {
      var thisx = Math.round(x(d.x));
      var thisy = Math.round(y(d.y));
      if (!(thisx in dotInfo))
        dotInfo[thisx] = [];
      var existing = dotInfo[thisx][thisy];
      var newInfo = d.label + "  \n" +
          'Run-time: ' + self.durationFormatter(d.y) + '  \nWait-time: ' + self.durationFormatter(d.x-startTime) +
          '  \nI/O: ' + self.bytesFormatter(d.io) + '  \nStatus: ' + d.status;
      if (existing)
        dotInfo[thisx][thisy] = existing + "  \n" + newInfo;
      else
        dotInfo[thisx][thisy] = newInfo;
    };

    mapNodeLocal.forEach(mapDotInfo);
    mapRackLocal.forEach(mapDotInfo);
    mapOffSwitch.forEach(mapDotInfo);
    reduceOffSwitch.forEach(mapDotInfo);

    this.addSeries(svgg, mapNodeLocal, "green", x, y, axisHeight+rmax, startTime, dotInfo);
    this.addSeries(svgg, mapRackLocal,'#66B366', x, y, axisHeight+rmax, startTime, dotInfo);
    this.addSeries(svgg, mapOffSwitch, 'brown', x, y, axisHeight+rmax, startTime, dotInfo);
    this.addSeries(svgg, reduceOffSwitch, 'steelblue', x, y, axisHeight+rmax, startTime, dotInfo);
  }
};
