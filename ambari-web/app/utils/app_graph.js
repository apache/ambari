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
  create:function(domId, wfData, jobData, svgw, svgh, nodeHeight, labelFontSize, maxLabelWidth, axisPadding, stageFontSize) {
    // initialize variables
    var nodes = new Array();
    var links = new Array();
    var numNodes = 0;
    var id = domId;
    var formatDuration = this.formatDuration;

    // create map from entity names to nodes
    var existingNodes = new Array();
    var jobData = (jobData) ? jobData : new Array();
    var minStartTime = 0;
    if (jobData.length > 0)
      minStartTime = jobData[0].submitTime;
    var maxFinishTime = 0;
    // iterate through job data
    for (var i = 0; i < jobData.length; i++) {
      jobData[i].info = "jobId:"+jobData[i].name+"  \n"+
        "nodeName:"+jobData[i].entityName+"  \n"+
        "status:"+jobData[i].status+"  \n"+
        "startTime:"+(new Date(jobData[i].submitTime).toUTCString())+"  \n"+
        "duration:"+formatDuration(jobData[i].finishTime - jobData[i].submitTime);
      jobData[i].state = jobData[i].status==="FINISHED";
      minStartTime = Math.min(minStartTime, jobData[i].submitTime);
      maxFinishTime = Math.max(maxFinishTime, jobData[i].finishTime);
      // add a node to the nodes array and to a provided map of entity names to nodes
      existingNodes[jobData[i].entityName] = jobData[i];
      nodes.push(jobData[i]);
      numNodes++;
    }
    var dag = eval('(' + wfData + ')').dag;
    var sourceMarker = new Array();
    var targetMarker = new Array();
    var sourceMap = new Array();
    // for each source node in the context, create links between it and its target nodes
    for (var source in dag) {
      var sourceNode = null;
      if (source in existingNodes)
        sourceNode = existingNodes[source];
      for (var i = 0; i < dag[source].length; i++) {
        var targetNode = null;
        if (dag[source][i] in existingNodes)
          targetNode = existingNodes[dag[source][i]];
        // add a link between sourceNode and targetNode
        // if source or target is null, add marker indicating unsubmitted job and return
        if (sourceNode==null) {
          if (targetNode==null)
            continue;
          sourceMarker.push(targetNode);
          continue;
        }
        if (targetNode==null) {
          targetMarker.push(sourceNode);
          continue;
        }
        // add link between nodes
        var state = false;
        if (sourceNode.state && targetNode.state)
          state = true;
        links.push({"source":sourceNode, "target":targetNode, "state":state, "value":sourceNode.output});
        // add source to map of targets to sources
        if (!(targetNode.name in sourceMap))
          sourceMap[targetNode.name] = new Array();
        sourceMap[targetNode.name].push(sourceNode);
      }
    }
  
    // display the graph
    // rules of thumb: nodeHeight = 20, labelFontSize = 14, maxLabelWidth = 180
    //                 nodeHeight = 15, labelFontSize = 10, maxLabelWidth = 120
    //                 nodeHeight = 40, labelFontSize = 20, maxLabelWidth = 260
    //                 nodeHeight = 30, labelFontSize = 16
    var nodeHeight = nodeHeight || 26;
    var labelFontSize = labelFontSize || 12;
    var maxLabelWidth = maxLabelWidth || 180;
    var axisPadding = axisPadding || 30;
    var stageFontSize = stageFontSize || 16;

    // draw timeline graph
    var margin = {"vertical":10, "horizontal":50};
    var w = svgw - 2*margin.horizontal;

    var startTime = minStartTime;
    var elapsedTime = maxFinishTime - minStartTime;
    var x = d3.time.scale.utc()
      .domain([startTime, startTime+elapsedTime])
      .range([0, w]);
    var xrel = d3.time.scale()
      .domain([0, elapsedTime])
      .range([0, w]);

    // process nodes and determine their x and y positions, width and height
    var minNodeSpacing = nodeHeight/2;
    var ends = new Array();
    var maxIndex = 0;
    nodes.sort(function(a,b){return a.name.localeCompare(b.name);});
    for (var i = 0; i < numNodes; i++) {
      var d = nodes[i];
      d.x = x(d.submitTime);
      d.w = x(d.finishTime) - x(d.submitTime);
      if (d.w < d.stages.length*(nodeHeight-4)) {
        d.w = d.stages.length*(nodeHeight-4);
        if (d.x + d.w > w)
          d.x = w - d.w;
      }
      var effectiveX = d.x
      var effectiveWidth = d.w;
      if (d.w < maxLabelWidth) {
        effectiveWidth = maxLabelWidth;
        if (d.x + effectiveWidth > w)
          effectiveX = w - effectiveWidth;
        else if (d.x > 0)
          effectiveX = d.x+(d.w-maxLabelWidth)/2;
      }
      // select "lane" (slot for y-position) for this node
      // starting at the slot above the node's closest source node (or 0, if none exists)
      // and moving down until a slot is found that has no nodes within minNodeSpacing of this node
      // excluding slots that contain more than one source of this node
      var index = 0;
      var rejectIndices = new Array();
      if (d.name in sourceMap) {
        var sources = sourceMap[d.name];
        var closestSource = sources[0];
        var indices = new Array();
        for (var j = 0; j < sources.length; j++) {
          if (sources[j].index in indices)
            rejectIndices[sources[j].index] = true;
          indices[sources[j].index] = true;
          if (sources[j].submitTime + sources[j].elapsedTime > closestSource.submitTime + closestSource.elapsedTime)
            closestSource = sources[j];
        }
        index = Math.max(0, closestSource.index-1);
      }
      while ((index in ends) && ((index in rejectIndices) || (ends[index]+minNodeSpacing >= effectiveX))) {
        index++
      }
      ends[index] = Math.max(effectiveX + effectiveWidth);
      maxIndex = Math.max(maxIndex, index);
      d.y = index*2*nodeHeight + axisPadding;
      d.h = nodeHeight;
      d.index = index;
    }

    var h = 2*axisPadding + 2*nodeHeight*(maxIndex+1);
    var realh = svgh - 2*margin.vertical;
    var scale = 1;
    if (h > realh)
      scale = realh / h;
    svgh = Math.min(svgh, h + 2*margin.vertical);
    var svg = d3.select("div#" + id).append("svg:svg")
      .attr("width", svgw+"px")
      .attr("height", svgh+"px");
    
    var svgg = svg.append("g")
      .attr("transform", "translate("+margin.horizontal+","+margin.vertical+") scale("+scale+")");

    // add an untranslated white rectangle below everything
    // so mouse doesn't have to be over nodes for panning/zooming
    svgg.append("svg:rect")
      .attr("x", 0)
      .attr("y", 0)
      .attr("width", svgw)
      .attr("height", svgh/scale)
      .attr("style", "fill:white;stroke:none");
 
    // create axes
    var topAxis = d3.svg.axis()
      .scale(x)
      .orient("bottom");
    var bottomAxis = d3.svg.axis()
      .scale(xrel)
      .orient("top")
      .tickFormat(function(x) { return formatDuration(x.getTime()); });
    var topg = svgg.append("g")
      .attr("class", "x axis top")
      .call(topAxis);
    topg.append("svg:text")
      .attr("class", "axislabel")
      .attr("x", -9)
      .attr("y", 13)
      .text("Time");
    var botg = svgg.append("g")
      .attr("class", "x axis bottom")
      .call(bottomAxis)
      .attr("transform", "translate(0,"+h+")")
    botg.append("svg:text")
      .attr("class", "axislabel")
      .attr("x", -9)
      .attr("y", -19)
      .text("Elapsed");
    botg.append("svg:text")
      .attr("class", "axislabel")
      .attr("x", -9)
      .attr("y", -4)
      .text("Time");

  
    // create a rectangle for each node
    var boxes = svgg.append("svg:g").selectAll("rect")
      .data(nodes)
      .enter().append("svg:rect")
      .attr("x", function(d) { return d.x; } )
      .attr("y", function(d) { return d.y; } )
      .attr("width", function(d) { return d.w; } )
      .attr("height", function(d) { return d.h; } )
      .attr("class", function (d) {
        return "node " + (d.state ? " finished" : "");
      })
      .attr("id", function (d) {
        return d.name;
      })
      .append("title")
      .text(function(d) { return d.info; });
  
    // defs for arrowheads marked as to whether they link finished jobs or not
    svgg.append("svg:defs").selectAll("arrowmarker")
      .data(["finished", "unfinished", "stage"])
      .enter().append("svg:marker")
      .attr("id", String)
      .attr("viewBox", "0 -5 10 10")
      .attr("markerWidth", 6)
      .attr("markerHeight", 6)
      .attr("refX", function(d) { return (d==="stage") ? 5 : 3 })
      .attr("orient", "auto")
      .append("svg:path")
      .attr("d", function(d) { return (d==="stage") ? "M0,-2L6,0L0,2" : "M0,-3L8,0L0,3" });
    // defs for unsubmitted node marker
    svgg.append("svg:defs").selectAll("circlemarker")
      .data(["circle"])
      .enter().append("svg:marker")
      .attr("id", String)
      .attr("viewBox", "-2 -2 18 18")
      .attr("markerWidth", 10)
      .attr("markerHeight", 10)
      .attr("refX", 10)
      .attr("refY", 5)
      .attr("orient", "auto")
      .append("svg:circle")
      .attr("cx", 5)
      .attr("cy", 5)
      .attr("r", 5);

    // create dangling links representing unsubmitted jobs
    var markerWidth = nodeHeight/2;
    var sourceMarker = svgg.append("svg:g").selectAll("line")
      .data(sourceMarker)
      .enter().append("svg:line")
      .attr("x1", function(d) { return d.x - markerWidth; } )
      .attr("x2", function(d) { return d.x; } )
      .attr("y1", function(d) { return d.y; } )
      .attr("y2", function(d) { return d.y + 3; } )
      .attr("class", "source mark")
      .attr("marker-start", "url(#circle)");
    var targetMarker = svgg.append("svg:g").selectAll("line")
      .data(targetMarker)
      .enter().append("svg:line")
      .attr("x1", function(d) { return d.x + d.w + markerWidth; } )
      .attr("x2", function(d) { return d.x + d.w; } )
      .attr("y1", function(d) { return d.y + d.h; } )
      .attr("y2", function(d) { return d.y + d.h - 3; } )
      .attr("class", "target mark")
      .attr("marker-start", "url(#circle)");

    // create links between the nodes
    var lines = svgg.append("svg:g").selectAll("path")
      .data(links)
      .enter().append("svg:path")
      .attr("d", function(d) {
        var s = d.source;
        var t = d.target;
        var x1 = s.x + s.w;
        var x2 = t.x;
        var y1 = s.y;
        var y2 = t.y;
        if (y1==y2) {
          y1 += s.h/2;
          y2 += t.h/2;
        } else if (y1 < y2) {
          y1 += s.h;
        } else {
          y2 += t.h;
        }
        return "M "+x1+" "+y1+" L "+((x2+x1)/2)+" "+((y2+y1)/2)+" L "+x2+" "+y2;
      } )
      .attr("class", function (d) {
        return "link" + (d.state ? " finished" : "");
      })
      .attr("marker-mid", function (d) {
        return "url(#" + (d.state ? "finished" : "unfinished") + ")";
      });
  
    // create text group for each node label
    var text = svgg.append("svg:g").selectAll("g")
      .data(nodes)
      .enter().append("svg:g");
  
    // add a shadow copy of the node label (will have a lighter color and thicker
    // stroke for legibility)
    text.append("svg:text")
      .attr("x", function(d) {
        var goal = d.x + d.w/2;
        var halfLabel = maxLabelWidth/2;
        if (goal < halfLabel) return halfLabel;
        else if (goal > w-halfLabel) return w-halfLabel;
        return goal;
      } )
      .attr("y", function(d) { return d.y + d.h + labelFontSize; } )
      .attr("class", "joblabel shadow")
      .attr("style", "font: "+labelFontSize+"px sans-serif")
      .text(function (d) {
        return d.name;
      });
  
    // add the main node label
    text.append("svg:text")
      .attr("x", function(d) {
        var goal = d.x + d.w/2;
        var halfLabel = maxLabelWidth/2;
        if (goal < halfLabel) return halfLabel;
        else if (goal > w-halfLabel) return w-halfLabel;
        return goal;
      } )
      .attr("y", function(d) { return d.y + d.h + labelFontSize; } )
      .attr("class", "joblabel")
      .attr("style", "font: "+labelFontSize+"px sans-serif")
      .text(function (d) {
        return d.name;
      });

    // add node stage information
    var topstageg = svgg.append("svg:g");
    for (var i = 0; i < numNodes; i++) {
      var parentg = topstageg.append("svg:g")
        .attr("class", "parent");
      var d = nodes[i];
      var cr = d.h / 2 - 2;
      var cy = d.y + cr + 2;
      var cxSpacing = d.w / d.stages.length;
      if (cxSpacing < 2*cr)
        cxSpacing = 2*cr;
      var cxBase = d.x + cxSpacing / 2;

      for (var j = 0; j < d.stages.length; j++) {
        var data = d.stages[j];
        var stageg = parentg.append("svg:g")
          .attr("id", "child"+j);
        var cx = cxBase + j*cxSpacing;
        var x1 = cx + cr;
        var x2 = cx + cxSpacing - cr;
        if (cxSpacing!=2*cr && j!=d.stages.length-1) {
          var path = stageg.append("svg:path")
            .attr("class", "link stage")
            .attr("d", "M "+x1+" "+cy+" L "+((x1+x2)/2)+" "+cy+" L "+x2+" "+cy)
            .attr("marker-end", "url(#stage)");
        }
        if (j==0) {
          stageg.append("svg:rect")
            .attr("class", "stage")
            .attr("x", cx - cr)
            .attr("y", cy - cr)
            .attr("width", 2*cr)
            .attr("height", 2*cr)
            .append("title")
            .text("Map stage with "+data+" task"+(data != 1 ? "s" : ""));
        } else {
          stageg.append("svg:circle")
            .attr("class", "stage")
            .attr("r", cr)
            .attr("cx", cx)
            .attr("cy", cy)
            .append("title")
            .text("Reduce stage with "+data+" task"+(data != 1 ? "s" : ""));
        }
        var fontSize = stageFontSize;
        if (data > 9) fontSize = fontSize - 2;
        if (data > 99) fontSize = fontSize - 4;
        if (data > 999) fontSize = fontSize - 2;
        if (data > 9999) fontSize = fontSize - 1;
        stageg.append("svg:text")
          .attr("class", "stagelabel")
          .attr("x", cx)
          .attr("y", cy)
          .text(data)
          .attr("style", "font: "+fontSize+"px sans-serif");
      }
    }

    svg.call(d3.behavior.zoom().on("zoom", function() {
      var left = Math.min(Math.max(d3.event.translate[0]+margin.horizontal, margin.horizontal-w*d3.event.scale*scale), margin.horizontal+w);
      var top = Math.min(Math.max(d3.event.translate[1]+margin.vertical, margin.vertical-h*d3.event.scale*scale), margin.vertical+h);
      svgg.attr("transform", "translate("+left+","+top+") scale("+(d3.event.scale*scale)+")");
    }));
  },
  formatDuration:function(d) {
    if (d==0) { return "0" }
    var subseconds = parseInt(d) / 1000;
    if (subseconds < 1)
      return subseconds + "s";
    var seconds = Math.floor(subseconds);
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
  }
}
