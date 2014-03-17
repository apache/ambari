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
var date = require('utils/date');
var numberUtils = require('utils/number_utils');
var stringUtils = require('utils/string_utils');

App.MainHiveJobDetailsTezDagView = Em.View.extend({
  templateName : require('templates/main/jobs/hive_job_details_tez_dag'),
  selectedVertex : null,
  summaryMetricType: null,
  svgVerticesLayer : null, // The contents of the <svg> element.
  svgTezRoot: null,
  svgWidth : -1,
  svgHeight : -1,

  // zoomScaleFom: -1, // Bound from parent view
  // zoomScaleTo: -1, // Bound from parent view
  // zoomScale: -1, // Bound from parent view
  zoomTranslate: [0, 0],
  zoomBehavior: null,
  svgCreated: false,

  content : null,

  /**
   * Populated by #drawTezDag() 
   * 
   * {
   *   "nodes": [
   *     {
   *       "id": "Map2",
   *       "name": "Map 2",
   *       "type": App.TezDagVertexType.MAP,
   *       "operations": [
   *         "TableScan",
   *         "File Output"
   *       ],
   *       "depth": 1,
   *       "parents": [],
   *       "children": [],
   *       "x": 0,
   *       "y": 0,
   *       "metricDisplay": "100MB",
   *       "metricPercent": 64,
   *       "metricType": "Input",
   *       "selected": true,
   *       "fixed": true,
   *       "metrics": {
   *         "input": 40022,
   *         "output": 224344,
   *         "recordsRead": 200,
   *         "recordsWrite": 122,
   *         "tezTasks": 2
   *       }
   *     }
   *   ],
   *   "links": [
   *     {
   *       "source": {},
   *       "target": {},
   *       "edgeType": "BROADCAST"
   *     }
   *   ]
   * }
   */
  dagVisualModel : {
    nodes : [],
    links : [],
    maxMetrics : {},
    minMetrics: {}
  },

  didInsertElement : function() {
    this._super();
    this.createSvg();
  },

  willDestroyElement : function() {
    $('.svg-tooltip').tooltip('destroy');
  },

  createSvg : function() {
    var self = this;
    var dagVisualModel = this.get('dagVisualModel');
    dagVisualModel.nodes.clear();
    dagVisualModel.links.clear();
    dagVisualModel.maxMetrics = {};
    dagVisualModel.minMetrics = {};

    this.set('content', this.get('controller.content'));
    var svg = d3.select("#tez-dag-svg");
    d3.selectAll(".tez-dag-canvas").remove();
    var tezRoot = svg.append("svg:g").attr("class", "tez-root");
    this.set('svgTezRoot', tezRoot);
    var tezRootRect = tezRoot.append("rect").attr("class", "tez-root-rect");
    this.set('svgVerticesLayer', tezRoot.append("svg:g").attr("class", "tez-dag-canvas"));
    this.adjustGraphHeight();
    var canvasSize = this.drawTezDag();
    var minScale = Math.min(this.get('svgHeight') / canvasSize.height, this.get('svgWidth') / canvasSize.width);
    if (minScale > 1) {
      minScale = 1;
    }
    tezRootRect.attr("width", canvasSize.width).attr("height", canvasSize.height);
    var zoom = d3.behavior.zoom().scaleExtent([ minScale, 2 ]).on("zoom", function() {
      tezRoot.attr("transform", "translate(" + (d3.event.translate) + ")scale(" + d3.event.scale + ")");
      self.set('zoomScale', d3.event.scale);
      self.set('zoomTranslate', d3.event.translate);
    });
    svg.call(zoom);
    this.set('zoomBehavior', zoom);
    this.set('zoomTranslate', [0, 0]);
    this.set('zoomScaleFrom', minScale);
    this.set('zoomScaleTo', 2);
    this.set('zoomScale', minScale);
    this.set('svgCreated', true);
  },

  zoomScaleObserver : function() {
    var tezRoot = this.get("svgTezRoot");
    var newScale = this.get('zoomScale');
    var newScaleFrom = this.get('zoomScaleFrom');
    var newScaleTo = this.get('zoomScaleTo');
    var zoomTranslate = this.get('zoomTranslate');
    var zoomBehavior = this.get('zoomBehavior');
    if (d3.event == null && this.get('svgCreated')) {
      // Values were set from actions instead of UI events
      // We need to center in on selected vertex if available.
      var selectedNode = null;
      var dagVisualModel = this.get('dagVisualModel');
      if (dagVisualModel && dagVisualModel.nodes && dagVisualModel.nodes.length > 0) {
        dagVisualModel.nodes.every(function(node) {
          if (node.selected) {
            selectedNode = node;
            return false;
          }
          return true;
        })
      }
      if (selectedNode != null) {
        var cX = selectedNode.x + selectedNode.width / 2;
        var cY = selectedNode.y + selectedNode.height / 2;
        var mX = (cX * zoomBehavior.scale()) + zoomTranslate[0];
        var mY = (cY * zoomBehavior.scale()) + zoomTranslate[1];
        var pX = (cX * newScale) + zoomTranslate[0];
        var pY = (cY * newScale) + zoomTranslate[1];
        var nX = (mX - pX);
        var nY = (mY - pY);
        zoomTranslate[0] += nX;
        zoomTranslate[1] += nY;
        this.set('zoomTranslate', zoomTranslate);
      }
    }
    console.debug("zoomScaleObserver(): New scale = " + newScale + ", Range = [" + newScaleFrom + ", " + newScaleTo + "]. Translate = ", zoomTranslate.join(','));
    zoomBehavior.scale(newScale);
    zoomBehavior.translate(zoomTranslate);
    tezRoot.attr("transform", "translate(" + zoomTranslate + ")scale(" + newScale + ")");
  }.observes('zoomScale', 'zoomScaleFrom', 'zoomScaleTo', 'zoomTranslate'),

  /**
   * We have to make the height of the DAG section match the height of the
   * Summary section.
   */
  adjustGraphHeight : function() {
    var rhsDiv = document.getElementById('tez-vertices-rhs');
    var lhsDiv = document.getElementById('tez-dag-section');
    if (lhsDiv && rhsDiv) {
      var rhsHeight = rhsDiv.clientHeight - 26; // box boundary
      var currentWidth = lhsDiv.clientWidth;
      var currentHeight = lhsDiv.clientHeight;
      $(lhsDiv).attr('style', "height:" + rhsHeight + "px;");
      var svgHeight = rhsHeight - 20;
      d3.select("#tez-dag-svg").attr('height', svgHeight).attr('width', '100%');
      this.set('svgWidth', currentWidth);
      this.set('svgHeight', svgHeight);
      console.log("SVG Width=", currentWidth, ", Height=", svgHeight);
    }
  },

  vertexSelectionUpdated : function() {
    var vertexId = this.get('selectedVertex.id');
    console.log("vertexSelectionUpdated(): Selected ",vertexId);
    var dagVisualModel = this.get('dagVisualModel');
    if (dagVisualModel && dagVisualModel.nodes && dagVisualModel.nodes.length > 0) {
      dagVisualModel.nodes.forEach(function(node) {
        node.selected = node.id == vertexId;
        console.log("vertexSelectionUpdated(): Updated  ",node.id," to ",node.selected);
      })
    }
    this.refreshGraphUI();
  }.observes('selectedVertex'),

  summaryMetricTypeUpdated : function() {
    var summaryMetricType = this.get('summaryMetricType');
    var dagVisualModel = this.get('dagVisualModel');
    var min = dagVisualModel.minMetrics[summaryMetricType];
    var max = dagVisualModel.maxMetrics[summaryMetricType];
    dagVisualModel.nodes.forEach(function(node) {
      var value = node.metrics[summaryMetricType];
      var percent = -1;
      if (numberUtils.validateInteger(value)==null && value >= 0) {
        if (numberUtils.validateInteger(min) == null && numberUtils.validateInteger(max) == null) {
          if (max > min && value >= 0) {
            percent = Math.round((value - min) * 100 / (max - min));
          }
        }
      } else {
        value = '';
      }
      switch (summaryMetricType) {
      case "input":
      case "output":
        value = numberUtils.bytesToSize(value);
        break;
      default:
        break;
      }
      node.metricType = Em.I18n.t('jobs.hive.tez.metric.' + summaryMetricType);
      node.metricDisplay = value;
      node.metricPercent = percent;
    });
    this.refreshGraphUI();
  }.observes('summaryMetricType'),

  /**
   * Observes metrics of all vertices.
   */
  vertexMetricsUpdated : function() {
    var dagVisualModel = this.get('dagVisualModel');
    dagVisualModel.minMetrics = {
      input : Number.MAX_VALUE,
      output : Number.MAX_VALUE,
      recordsRead : Number.MAX_VALUE,
      recordsWrite : Number.MAX_VALUE,
      tezTasks : Number.MAX_VALUE,
      spilledRecords : Number.MAX_VALUE
    };
    dagVisualModel.maxMetrics = {
      input : 0,
      output : 0,
      recordsRead : 0,
      recordsWrite : 0,
      tezTasks : 0,
      spilledRecords : 0
    };
    if (dagVisualModel.nodes) {
      dagVisualModel.nodes.forEach(function(node) {
        var vertex = App.TezDagVertex.find(node.id);
        if (vertex) {
          node.metrics['input'] = vertex.get('fileReadBytes') + vertex.get('hdfsReadBytes');
          node.metrics['output'] = vertex.get('fileWriteBytes') + vertex.get('hdfsWriteBytes');
          node.metrics['recordsRead'] = vertex.get('recordReadCount');
          node.metrics['recordsWrite'] = vertex.get('recordWriteCount');
          node.metrics['tezTasks'] = vertex.get('tasksCount');
          node.metrics['spilledRecords'] = vertex.get('spilledRecords');
          node.state = vertex.get('state');
          // Min metrics
          dagVisualModel.minMetrics.input = Math.min(dagVisualModel.minMetrics.input, node.metrics.input);
          dagVisualModel.minMetrics.output = Math.min(dagVisualModel.minMetrics.output, node.metrics.output);
          dagVisualModel.minMetrics.recordsRead = Math.min(dagVisualModel.minMetrics.recordsRead, node.metrics.recordsRead);
          dagVisualModel.minMetrics.recordsWrite = Math.min(dagVisualModel.minMetrics.recordsWrite, node.metrics.recordsWrite);
          dagVisualModel.minMetrics.tezTasks = Math.min(dagVisualModel.minMetrics.tezTasks, node.metrics.tezTasks);
          dagVisualModel.minMetrics.spilledRecords = Math.min(dagVisualModel.minMetrics.spilledRecords, node.metrics.spilledRecords);
          // Max metrics
          dagVisualModel.maxMetrics.input = Math.max(dagVisualModel.maxMetrics.input, node.metrics.input);
          dagVisualModel.maxMetrics.output = Math.max(dagVisualModel.maxMetrics.output, node.metrics.output);
          dagVisualModel.maxMetrics.recordsRead = Math.max(dagVisualModel.maxMetrics.recordsRead, node.metrics.recordsRead);
          dagVisualModel.maxMetrics.recordsWrite = Math.max(dagVisualModel.maxMetrics.recordsWrite, node.metrics.recordsWrite);
          dagVisualModel.maxMetrics.tezTasks = Math.max(dagVisualModel.maxMetrics.tezTasks, node.metrics.tezTasks);
          dagVisualModel.maxMetrics.spilledRecords = Math.max(dagVisualModel.maxMetrics.spilledRecords, node.metrics.spilledRecords);
        }
      });
    }
    Ember.run.once(this, 'summaryMetricTypeUpdated');
  }.observes('content.tezDag.vertices.@each.fileReadBytes', 'content.tezDag.vertices.@each.fileWriteBytes', 
      'content.tezDag.vertices.@each.hdfsReadBytes', 'content.tezDag.vertices.@each.hdfsWriteBytes', 
      'content.tezDag.vertices.@each.recordReadCount', 'content.tezDag.vertices.@each.recordWriteCount',
      'content.tezDag.vertices.@each.state', 'content.tezDag.vertices.@each.spilledRecords'),

  /**
   * Determines layout and creates Tez graph. In the process it populates the
   * visual model into 'dagVisualModel' field.
   * 
   * Terminology: 'vertices' and 'edges' are Tez terms. 'nodes' and 'links' are
   * visual (d3) terms.
   */
  drawTezDag : function() {
    var width = this.get('svgWidth');
    var svgLayer = this.get('svgVerticesLayer');
    var vertices = this.get('content.tezDag.vertices');
    var edges = this.get('content.tezDag.edges');
    var constants = this.get('constants');
    var vertexIdToNode = {};
    var depthToNodes = []; // Array of id arrays
    var dagVisualModel = this.get('dagVisualModel');
    var selectedVertex = this.get('selectedVertex');

    //
    // CALCULATE DEPTH - BFS to get correct graph depth
    //
    var visitEdges = [];
    var maxRowLength = 0;
    var maxRowDepth = 0;
    vertices.forEach(function(v) {
      if (v.get('incomingEdges.length') < 1) {
        visitEdges.push({
          depth : 0,
          parent : null,
          toVertex : v
        });
      }
    });
    function getNodeFromEdge(edgeObj) {
      var vertex = edgeObj.toVertex;
      var pName = edgeObj.parent ? edgeObj.parent.name : null;
      var cName = edgeObj.toVertex ? edgeObj.toVertex.get('name') : null;
      console.debug("Processing vertex ", edgeObj, " (",pName, " > ", cName,")");
      if(edgeObj.parent && edgeObj.depth < edgeObj.parent.depth + 1){
        console.debug("Updating child edge to " + (edgeObj.parent.depth + 1));
        edgeObj.depth = edgeObj.parent.depth + 1;
      }
      var node = vertexIdToNode[vertex.get('id')];
      for ( var k = depthToNodes.length; k <= edgeObj.depth; k++) {
        depthToNodes.push([]);
      }
      if (!node) {
        // New node
        node = {
          id : vertex.get('id'),
          name : vertex.get('name'),
          state : vertex.get('state'),
          type : vertex.get('type'),
          operations : vertex.get('operations'),
          depth : edgeObj.depth,
          parents : [],
          children : [],
          x : 0,
          y : 0,
          metricType : null,
          metricDisplay : null,
          metricPercent : -1,
          selected : selectedVertex != null ? selectedVertex.get('id') == vertex.get('id') : false,
          fixed : true,
          metrics : {
            input : -1,
            output : -1,
            recordsRead : -1,
            recordsWrite : -1,
            tezTasks : -1
          }
        }
        vertexIdToNode[vertex.get('id')] = node;
        depthToNodes[node.depth].push(node);
      } else {
        // Existing node
        if (edgeObj.depth > node.depth) {
          function moveNodeToDepth(node, newDepth) {
            console.debug("Moving " + node.name + " from depth " + node.depth + " to " + newDepth);
            var oldIndex = depthToNodes[node.depth].indexOf(node);
            depthToNodes[node.depth].splice(oldIndex, 1);
            node.depth = newDepth;
            if (!depthToNodes[node.depth]) {
              depthToNodes[node.depth] = [];
            }
            depthToNodes[node.depth].push(node);
            if (node.children) {
              // Move children down depth
              node.children.forEach(function(c) {
                moveNodeToDepth(c, node.depth + 1);
              })
            }
          }
          moveNodeToDepth(node, edgeObj.depth);
        }
      }
      if (depthToNodes[node.depth].length > maxRowLength) {
        maxRowLength = depthToNodes[node.depth].length;
        maxRowDepth = node.depth;
      }
      if (edgeObj.parent != null) {
        node.parents.push(edgeObj.parent);
        edgeObj.parent.children.push(node);
      }
      return node;
    }
    var edgeObj;
    var visitedVertexMap = {};
    while (edgeObj = visitEdges.shift()) {
      var node = getNodeFromEdge(edgeObj);
      if (!visitedVertexMap[edgeObj.toVertex.get('id')]) {
        visitedVertexMap[edgeObj.toVertex.get('id')] = true;
        var outEdges = edgeObj.toVertex.get('outgoingEdges');
        outEdges.forEach(function(oe) {
          var childVertex = oe.get('toVertex');
          visitEdges.push({
            depth : node.depth + 1,
            parent : node,
            toVertex : childVertex
          });
        });
      }
    }
    edges.forEach(function(e) {
      dagVisualModel.links.push({
        source : vertexIdToNode[e.get('fromVertex.id')],
        target : vertexIdToNode[e.get('toVertex.id')],
        edgeType : e.get('edgeType')
      });
    });
    // Sort nodes so that parents stay together
    for ( var depth = 0; depth < depthToNodes.length; depth++) {
      var nodes = depthToNodes[depth];
      nodes.sort(function(n1, n2) {
        var ck1 = '';
        var ck2 = '';
        if (n1.children) {
          n1.children.forEach(function(c) {
            ck1 += c.name;
          });
        }
        if (n2.children) {
          n2.children.forEach(function(c) {
            ck2 += c.name;
          });
        }
        if (ck1 < ck2) {
          return -1
        }
        if (ck1 > ck2) {
          return 1
        }
        return 0
      });
      depthToNodes[depth] = nodes;
    }

    //
    // LAYOUT - Now with correct depth, we calculate layouts
    //
    // When a node's effective width changes, all its parent nodes are updated.
    var updateNodeEffectiveWidth = function(node, newEffectiveWidth) {
      console.debug("Updating effective width of (" + node.id + ") to " + newEffectiveWidth);
      if (numberUtils.validateInteger(node.effectiveWidth) != null) {
        node.effectiveWidth = newEffectiveWidth;
      }
      var diff = newEffectiveWidth - node.effectiveWidth;
      if (diff > 0) {
        var oldEffectiveWidth = node.effectiveWidth;
        node.effectiveWidth = newEffectiveWidth;
        if (node.parents != null) {
          node.parents.forEach(function(parent) {
            updateNodeEffectiveWidth(parent, parent.effectiveWidth + diff);
          })
        }
      }
    }
    var xGap = 20;
    var yGap = 70;
    var currentY = 40;
    // First pass - calculate layout widths, and Y coordinates
    for ( var depth = 0; depth < depthToNodes.length; depth++) {
      var nodes = depthToNodes[depth];
      var maxNodeHeight = 0;
      for ( var nodeIndex = 0; nodeIndex < nodes.length; nodeIndex++) {
        var node = nodes[nodeIndex];
        var nodeDim = this.getNodeCalculatedDimensions(node);
        node.width = nodeDim.width;
        node.height = nodeDim.height;
        if (maxNodeHeight < node.height) {
          maxNodeHeight = node.height;
        }
        if (depth == 0) {
          // Top nodes - position uniformly
          updateNodeEffectiveWidth(node, xGap + node.width);
        }
        if (node.children && node.children.length > 0) {
          // There can be dedicated or shared children.
          // Dedicated children increase effective width of parent by their
          // width.
          // Shared children increase effective width of parent only by the
          // fraction of parentage
          var childrenWidth = 0;
          node.children.forEach(function(child) {
            childrenWidth += ((node.width + xGap) / child.parents.length);
          });
          updateNodeEffectiveWidth(node, Math.max(childrenWidth, (node.width+xGap)));
        } else {
          updateNodeEffectiveWidth(node, xGap + node.width);
        }
        node.y = currentY;
        node.incomingY = node.y;
        node.outgoingY = node.incomingY + node.height;
      }
      currentY += maxNodeHeight;
      currentY += yGap;
    }
    // Second pass - determine actual X coordinates
    var maxX = 0;
    for ( var depth = 0; depth < depthToNodes.length; depth++) {
      var nodes = depthToNodes[depth];
      var currentX = -1;
      var parentCurrentXMap = {};
      for ( var nodeIndex = 0; nodeIndex < nodes.length; nodeIndex++) {
        var node = nodes[nodeIndex];
        var parentsKey = null;
        if (node.parents != null && node.parents.length > 0) {
          var parentMidX = 0;
          var parentsKey = '';
          var childrenEffectiveWidth = -1;
          node.parents.forEach(function(parent) {
            parentMidX += (parent.x + (parent.width / 2));
            parentsKey += (parent.id + '//');
            if (childrenEffectiveWidth < 0) {
              parent.children.forEach(function(c){
                childrenEffectiveWidth += (c.effectiveWidth);
              });
            }
          });
          parentMidX = parentMidX / node.parents.length;
          var parentCurrentX = parentCurrentXMap[parentsKey];
          if (parentCurrentX == null || parentCurrentX == undefined) {
            parentCurrentX = parentMidX - (childrenEffectiveWidth/2);
            parentCurrentXMap[parentsKey] = parentCurrentX;
          }
          currentX = parentCurrentX;
        } else {
          if (currentX < 0) {
            currentX = 0;
          }
        }
        node.x = (currentX + (node.effectiveWidth - node.width) / 2);
        node.outgoingX = (node.x + node.width / 2);
        node.incomingX = node.outgoingX;
        console.log("drawTezDag(). Layout Node: ", node);
        dagVisualModel.nodes.push(node);
        if (parentsKey != null) {
          parentCurrentXMap[parentsKey] = currentX + node.effectiveWidth;
        } else {
          currentX += node.effectiveWidth;
        }
        if ((node.x + node.width) > maxX) {
          maxX = node.x + node.width;
        }
      }
    }
    var canvasHeight = currentY;
    var canvasWidth = maxX + (xGap << 1);

    //
    // Draw SVG
    //
    var self = this;
    var force = d3.layout.force().nodes(dagVisualModel.nodes).links(dagVisualModel.links).start();
    var nodeDragData = {
      nodeRelativeX : 0,
      nodeRelativeY : 0
    };
    var nodeDrag = d3.behavior.drag().on('dragstart', function(node){
      d3.event.sourceEvent.stopPropagation();
      var rc = d3.mouse(this);
      nodeDragData.nodeRelativeX = rc[0];
      nodeDragData.nodeRelativeY = rc[1];
    }).on('drag', function(node){
      var nx = d3.event.x - nodeDragData.nodeRelativeX;
      var ny = d3.event.y - nodeDragData.nodeRelativeY;
      self.dragVertex(d3.select(this), node, [nx, ny], diagonal);
    }).on('dragend', function(){
      nodeDragData.nodeRelativeX = 0;
      nodeDragData.nodeRelativeY = 0;
    });
    // Create Links
    var diagonal = d3.svg.diagonal().source(function(d) {
      return {
        x : d.source.outgoingX,
        y : d.source.outgoingY
      };
    }).target(function(d) {
      return {
        x : d.target.incomingX,
        y : d.target.incomingY - 12
      }
    });
    var link = svgLayer.selectAll(".link-g").data(dagVisualModel.links).enter().append("g").attr("class", "link-g").attr("marker-end", "url(#arrow)");
    link.append("path").attr("class", function(l) {
      var classes = "link svg-tooltip ";
      if (l.edgeType) {
        classes += ("type-" + l.edgeType.toLowerCase() + " ");
      } else {
        classes += "type-unknown ";
      }
      return classes;
    }).attr("d", diagonal).attr("title", function(l) {
      var lower = l.edgeType ? l.edgeType.toLowerCase() : '';
      return Em.I18n.t("jobs.hive.tez.edge." + lower);
    });
    // Create Nodes
    var node = svgLayer.selectAll(".node").data(dagVisualModel.nodes).enter().append("g").attr("class", "node").call(nodeDrag);
    node.append("rect").attr("class", "background").attr("width", function(n) {
      return n.width;
    }).attr("height", function(n) {
      return n.height;
    }).attr("rx", "10").attr("filter", "url(#shadow)").on('mousedown', function(n) {
      var vertex = App.TezDagVertex.find(n.id);
      if (vertex != null) {
        self.get('parentView').doSelectVertex({
          context : vertex
        });
      }
    });
    node.each(function(n, nodeIndex) {
      var ops = n.operations;
      var opCount = {};
      if (ops != null && ops.length > 0) {
        var opGroups = d3.select(this).selectAll(".operation").data(ops).enter().append("g").attr("class", "operation").attr("transform", function(op, opIndex) {
          var row = Math.floor(opIndex / 3);
          var column = opIndex % 3;
          return "translate(" + (10 + column * 55) + "," + (37 + row * 20) + ")";
        }).attr("clip-path", "url(#operatorClipPath)").attr("opIndex", function(op){
          if(!opCount[op]) {
            opCount[op] = 1;
          } else {
            opCount[op] = opCount[op]+1;
          }
          return opCount[op];
        }).on('mousedown', function(op) {
          var opIndex = this.getAttribute ? this.getAttribute("opIndex") : null;
          if (numberUtils.validateInteger(opIndex) == null) {
            console.log("Clicked on operator: ", op, " [", opIndex, "]");
            var textArea = document.getElementById('tez-vertex-operator-plan-textarea');
            if (textArea && textArea.value) {
              var text = textArea.value;
              var opText = "\"" + op + "\"";
              var count = 1;
              var index = text.indexOf(opText);
              while (index > -1 && count < opIndex) {
                index = text.indexOf(opText, index + 1);
                count++;
              }
              if (index > -1) {
                var start = index;
                var end = index;
                var matchCount = 0;
                var splits = text.substring(start).split(/({|})/);
                splits.every(function(s) {
                  if (s == '{') {
                    matchCount++;
                  } else if (s == '}') {
                    matchCount--;
                    if (matchCount == 0) {
                      end += s.length;
                      return false;
                    }
                  }
                  end += s.length;
                  return true;
                });
                textArea.setSelectionRange(start, end);
                // Now scroll to the selection
                var lines = 0;
                var totalLines = 0;
                var index = text.indexOf("\n");
                while (index > 0) {
                  index = text.indexOf("\n", index + 1);
                  if (index < start) {
                    lines++;
                  }
                  totalLines++;
                }
                console.log("Selection is from row ", lines, " out of ", totalLines);
                lines -= 5;
                var lineHeight = Math.floor(textArea.scrollHeight / totalLines);
                var scrollHeight = Math.round(lines * lineHeight);
                textArea.scrollTop = scrollHeight;
              }
            }
          }
        });
        opGroups.append("rect").attr("class", "operation svg-tooltip ").attr("width", "50").attr("height", "16").attr("title", function(op) {
          return op;
        });
        opGroups.append("text").attr("x", "2").attr("dy", "1em").text(function(op) {
          return op != null ? op.split(' ')[0] : '';
        });
      }
    });
    var metricNodes = node.append("g").attr("class", "metric").attr("transform", "translate(112,7)");
    metricNodes.append("rect").attr("width", function(n) {
      if (n.type == App.TezDagVertexType.UNION) {
        return 0;
      }
      return 60;
    }).attr("height", function(n) {
      if (n.type == App.TezDagVertexType.UNION) {
        return 0;
      }
      return 18;
    }).attr("rx", "3").attr("class", "metric-title svg-tooltip");
    metricNodes.append("text").attr("class", "metric-text").attr("x", "2").attr("dy", "1em");
    node.append("text").attr("x", "1.9em").attr("dy", "1.5em").text(function(d) {
      return d.name;
    });
    var iconContainer = node.append('g').attr('class', 'vertex-icon-container').attr('transform', 'translate(10,10)');
    iconContainer.append('rect').attr('width', '1em').attr('height', '1em').attr('class', 'vertex-icon-rect  svg-tooltip ');
    iconContainer.append('text').attr('dy', '10px').attr("font-family", "FontAwesome").attr('class', 'vertex-icon-text');
    node.attr("transform", function(d) {
      return "translate(" + d.x + "," + d.y + ")";
    });
    this.vertexMetricsUpdated();
    $('.svg-tooltip').tooltip({
      placement : 'left'
    });

    if (App.supports.debugJobsDag) {
      // Draws node bounding box - for debug purposes
      node.append("rect").attr("width", function(n) {
        return n.effectiveWidth;
      }).attr("height", function(n) {
        return n.height;
      }).attr("x", function(n) {
        return -1 * ((n.effectiveWidth - n.width) / 2);
      }).attr("y", function(n) {
        return 0;
      }).attr("style", "opacity: 0.2;fill:yellow;");
    }

    // Position in center
    var translateX = Math.round((width - canvasWidth) / 2);
    if (translateX > 0) {
      svgLayer.attr("transform", "translate("+translateX+",0)");
    }
    return {
      width : canvasWidth,
      height : canvasHeight
    }
  },

  dragVertex: function(d3Vertex, node, newPosition, diagonal){
    console.debug("Dragging vertex [", node.name, "] to (", newPosition[0], ",", newPosition[1], ")");
    // Move vertex
    node.x = newPosition[0];
    node.y = newPosition[1];
    node.incomingX = newPosition[0] + (node.width/2);
    node.incomingY = newPosition[1];
    node.outgoingX = newPosition[0] + (node.width/2);
    node.outgoingY = newPosition[1] + node.height;
    d3Vertex.attr('transform', 'translate(' + newPosition[0] + ',' + newPosition[1] + ')');
    // Move links
    d3.selectAll('.link').filter(function(l) {
      if (l && (l.source === node || l.target === node)) {
        return this
      }
      return null;
    }).attr('d', diagonal);
  },

  /**
   * Refreshes UI of the Tez graph with latest values
   */
  refreshGraphUI: function () {
    var svgLayer = this.get('svgVerticesLayer');
    if (svgLayer!=null) {
      var self = this;
      var metricNodes = svgLayer.selectAll(".metric");
      var metricNodeTexts = svgLayer.selectAll(".metric-text");
      var metricNodeTitles = svgLayer.selectAll(".metric-title");
      var nodeBackgrounds =svgLayer.selectAll(".background");
      var vertexIconTexts = svgLayer.selectAll(".vertex-icon-text");
      var vertexIconRects = svgLayer.selectAll(".vertex-icon-rect");
      metricNodes.attr("class", function(node) {
        var classes = "metric ";
        var percent = node.metricPercent;
        if (numberUtils.validateInteger(percent) == null && percent >= 0) {
          if (percent <= 20) {
            classes += "heat-0-20 ";
          } else if (percent <= 40) {
            classes += "heat-20-40 ";
          } else if (percent <= 60) {
            classes += "heat-40-60 ";
          } else if (percent <= 80) {
            classes += "heat-60-80 ";
          } else if (percent <= 100) {
            classes += "heat-80-100 ";
          } else {
            classes += "heat-none";
          }
        } else {
          classes += "heat-none";
        }
        return classes;
      });
      metricNodeTexts.text(function(node){
        if (node.type == App.TezDagVertexType.UNION) {
          return '';
        }
        return node.metricDisplay;
      });
      metricNodeTitles.attr("title", function(node){
        return node.metricType;
      }).attr("data-original-title", function(node){
        return node.metricType;
      });
      nodeBackgrounds.attr("class", function(n) {
        var classes = "background ";
        if (n.type) {
          classes += (n.type.toLowerCase() + " ");
        } else {
          classes += "unknown-vertex-type ";
        }
        if (n.selected) {
          classes += "selected ";
        }
        return classes;
      });
      vertexIconRects.attr('title', function(node) {
        return stringUtils.getCamelCase(node.state);
      }).attr('data-original-title', function(node) {
        return stringUtils.getCamelCase(node.state);
      });
      vertexIconTexts.text(function(n) {
        return self.getVertexIcon(n)
      }).attr('class', function(n) {
        var classes = 'vertex-icon-text ';
        if (n.state != null) {
          classes += n.state.toLowerCase();
        }
        return classes;
      });
    }
  },

  getVertexIcon : function(node){
    var icon = "";
    switch (node.state) {
    case App.TezDagVertexState.NEW:
      icon = '\uF10C'; //icon-circle-blank
    case App.TezDagVertexState.RUNNING:
    case App.TezDagVertexState.FAILED:
      icon = '\uF111'; //icon-circle
      break;
    case App.TezDagVertexState.SUCCEEDED:
      icon = '\uF00C'; //icon-ok
      break;
    case App.TezDagVertexState.KILLED:
    case App.TezDagVertexState.ERROR:
      icon = '\uF057'; //icon-remove-sign
      break;
    case App.TezDagVertexState.INITED:
    case App.TezDagVertexState.INITIALIZING:
    case App.TezDagVertexState.TERMINATING:
      icon = '\uF141'; //icon-ellipsis-horizontal
      break;
    }
    return icon;
  },

  /**
   * Determines the node width and height in pixels.
   *
   * Takes into account the various contents of the a node. { width: 200,
   * height: 60 }
   */
  getNodeCalculatedDimensions : function(node) {
    var size = {
      width : 180,
      height : 40
    }
    if (node.operations && node.operations.length > 0) {
      var opsHeight = Math.ceil(node.operations.length / 3);
      size.height += (opsHeight * 20);
    }
    return size;
  }

});
