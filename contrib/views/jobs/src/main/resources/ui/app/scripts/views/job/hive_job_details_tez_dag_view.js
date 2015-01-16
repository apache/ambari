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

App.MainHiveJobDetailsTezDagView = Em.View.extend({

  templateName: 'job/hive_job_details_tez_dag',

  /**
   * Selected Vertex
   * @type {App.TezDagVertex}
   */
  selectedVertex: null,

  /**
   * @type {string}
   */
  summaryMetricType: null,

  /**
   * The contents of the <svg> element
   */
  svgVerticesLayer: null,

  svgTezRoot: null,

  svgWidth: -1,

  svgHeight: -1,

  // zoomScaleFom: -1, // Bound from parent view
  // zoomScaleTo: -1, // Bound from parent view
  // zoomScale: -1, // Bound from parent view

  zoomTranslate: [0, 0],

  zoomBehavior: null,

  svgCreated: false,

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
  dagVisualModel: {
    nodes: [],
    links: [],
    maxMetrics: {},
    minMetrics: {}
  },

  didInsertElement: function () {
    this._super();
    this.createSvg();
  },

  willDestroyElement: function () {
    $('.svg-tooltip').tooltip('destroy');
  },

  /**
   * Basic init for graph
   * @method createSvg
   */
  createSvg: function () {
    var self = this;
    var dagVisualModel = this.get('dagVisualModel');
    dagVisualModel.nodes.clear();
    dagVisualModel.links.clear();
    dagVisualModel.maxMetrics = {};
    dagVisualModel.minMetrics = {};

    //this.set('content', this.get('content'));
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
    var zoom = d3.behavior.zoom().scaleExtent([ minScale, 2 ]).on("zoom", function () {
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

  /**
   * Change graph's zoom
   * @method zoomScaleObserver
   */
  zoomScaleObserver: function () {
    var tezRoot = this.get("svgTezRoot"),
      newScale = this.get('zoomScale'),
      zoomTranslate = this.get('zoomTranslate'),
      zoomBehavior = this.get('zoomBehavior');
    if (d3.event == null && this.get('svgCreated')) {
      // Values were set from actions instead of UI events
      // We need to center in on selected vertex if available.
      var selectedNode = null;
      var dagVisualModel = this.get('dagVisualModel');
      if (dagVisualModel && dagVisualModel.nodes && dagVisualModel.nodes.length > 0) {
        dagVisualModel.nodes.every(function (node) {
          if (node.selected) {
            selectedNode = node;
            return false;
          }
          return true;
        })
      }
      if (selectedNode != null) {
        var cX = selectedNode.x + selectedNode.width / 2,
          cY = selectedNode.y + selectedNode.height / 2,
          mX = (cX * zoomBehavior.scale()) + zoomTranslate[0],
          mY = (cY * zoomBehavior.scale()) + zoomTranslate[1],
          pX = (cX * newScale) + zoomTranslate[0],
          pY = (cY * newScale) + zoomTranslate[1],
          nX = (mX - pX),
          nY = (mY - pY);
        zoomTranslate[0] += nX;
        zoomTranslate[1] += nY;
        this.set('zoomTranslate', zoomTranslate);
      }
    }
    zoomBehavior.scale(newScale);
    zoomBehavior.translate(zoomTranslate);
    tezRoot.attr("transform", "translate(" + zoomTranslate + ")scale(" + newScale + ")");
  }.observes('zoomScale', 'zoomScaleFrom', 'zoomScaleTo', 'zoomTranslate'),

  /**
   * We have to make the height of the DAG section match the height of the Summary section.
   * @method adjustGraphHeight
   */
  adjustGraphHeight: function () {
    var rhsDiv = document.getElementById('tez-vertices-rhs'),
      lhsDiv = document.getElementById('tez-dag-section');
    if (lhsDiv && rhsDiv) {
      var rhsHeight = rhsDiv.clientHeight - 26, // box boundary
      currentWidth = lhsDiv.clientWidth;
      $(lhsDiv).attr('style', "height:" + rhsHeight + "px;");
      var svgHeight = rhsHeight - 20;
      d3.select("#tez-dag-svg").attr('height', svgHeight).attr('width', '100%');
      this.set('svgWidth', currentWidth);
      this.set('svgHeight', svgHeight);
    }
  },

  /**
   * Update graph when <code>selectedVertex</code> changed
   * @method vertexSelectionUpdated
   */
  vertexSelectionUpdated: function () {
    var vertexId = this.get('selectedVertex.id'),
      zoomTranslate = [],
      zoomBehavior = this.get('zoomBehavior'),
      selectedNode = this.get('dagVisualModel').nodes.findProperty('id', vertexId),
      dagVisualModel = this.get('dagVisualModel');
    if (dagVisualModel && dagVisualModel.nodes && dagVisualModel.nodes.length > 0) {
      dagVisualModel.nodes.forEach(function (node) {
        node.selected = node.id == vertexId;
      })
    }
    if (!this.get('selectedVertex.notTableClick')) {
      var cX = selectedNode.x + (selectedNode.width) / 2,
        cY = selectedNode.y + (selectedNode.height) / 2;
      zoomTranslate[0] = (225 / zoomBehavior.scale() - cX);
      zoomTranslate[1] = (250 / zoomBehavior.scale() - cY);
      this.set('zoomTranslate', [0, 0]);
      this.get('svgVerticesLayer').attr("transform", "translate(0,0)");
      this.get('svgVerticesLayer').attr("transform", "translate(" + zoomTranslate[0] + "," + zoomTranslate[1] + ")");
    }
    this.refreshGraphUI();
  }.observes('selectedVertex'),

  /**
   * Update graph when new summary metric is selected
   * @method summaryMetricTypeUpdated
   */
  summaryMetricTypeUpdated: function () {
    var summaryMetricType = this.get('summaryMetricType'),
      dagVisualModel = this.get('dagVisualModel'),
      min = dagVisualModel.minMetrics[summaryMetricType],
      max = dagVisualModel.maxMetrics[summaryMetricType];
    dagVisualModel.nodes.forEach(function (node) {
      var value = node.metrics[summaryMetricType],
        percent = -1;
      if (App.Helpers.number.validateInteger(value) == null && value >= 0) {
        if (App.Helpers.number.validateInteger(min) == null && App.Helpers.number.validateInteger(max) == null) {
          if (max > min && value >= 0) {
            percent = Math.round((value - min) * 100 / (max - min));
          }
        }
      }
      else {
        value = '';
      }
      if (['input', 'output'].contains(summaryMetricType)) {
        value = App.Helpers.number.bytesToSize(value);
      }
      node.metricType = Em.I18n.t('jobs.hive.tez.metric.' + summaryMetricType);
      node.metricDisplay = value;
      node.metricPercent = percent;
    });
    this.refreshGraphUI();
  }.observes('summaryMetricType'),

  /**
   * Observes metrics of all vertices
   * @method vertexMetricsUpdated
   */
  vertexMetricsUpdated: function () {
    var dagVisualModel = this.get('dagVisualModel');
    dagVisualModel.minMetrics = {
      input: Number.MAX_VALUE,
      output: Number.MAX_VALUE,
      recordsRead: Number.MAX_VALUE,
      recordsWrite: Number.MAX_VALUE,
      tezTasks: Number.MAX_VALUE,
      spilledRecords: Number.MAX_VALUE
    };
    dagVisualModel.maxMetrics = {
      input: 0,
      output: 0,
      recordsRead: 0,
      recordsWrite: 0,
      tezTasks: 0,
      spilledRecords: 0
    };
    if (dagVisualModel.nodes) {
      dagVisualModel.nodes.forEach(function (node) {
        var vertex = App.HiveJob.store.getById('tezDagVertex', node.id);
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
  }.observes(
      'content.tezDag.vertices.@each.fileReadBytes',
      'content.tezDag.vertices.@each.fileWriteBytes',
      'content.tezDag.vertices.@each.hdfsReadBytes',
      'content.tezDag.vertices.@each.hdfsWriteBytes',
      'content.tezDag.vertices.@each.recordReadCount',
      'content.tezDag.vertices.@each.recordWriteCount',
      'content.tezDag.vertices.@each.state',
      'content.tezDag.vertices.@each.spilledRecords'
    ),

  /**
   * Create object with data for graph popups
   * @param {string} vertexName
   * @param {string} op
   * @param {number} opIndex
   * @returns {{name: string, value: string}[]}
   * @method createOperationPlanObj
   */
  createOperationPlanObj: function (vertexName, op, opIndex) {
    var operatorPlanObj = [],
      text = this.get('content.tezDag.vertices').findBy('name', vertexName).get('operationPlan');
    text = text.replace(/:"/g, '"').replace(/([:,])(?=\S)/g, '$1 ');
    var jsonText = $.parseJSON(text);
    opIndex = opIndex ? parseInt(opIndex) - 1 : 0;
    jsonText = App.Helpers.string.findIn(op, jsonText, opIndex);
    if (jsonText != null) {
      for (var key in jsonText) {
        if (jsonText.hasOwnProperty(key) && typeof(jsonText[key]) == "string") {
          operatorPlanObj.push({
            name: key,
            value: jsonText[key]
          });
        }
      }
    }
    return operatorPlanObj;
  },

  /**
   * Determines layout and creates Tez graph. In the process it populates the
   * visual model into 'dagVisualModel' field.
   *
   * Terminology: 'vertices' and 'edges' are Tez terms. 'nodes' and 'links' are
   * visual (d3) terms.
   * @method drawTezDag
   */
  drawTezDag: function () {
    var self = this,
      width = this.get('svgWidth'),
      svgLayer = this.get('svgVerticesLayer'),
      vertices = this.get('content.tezDag.vertices'),
      edges = this.get('content.tezDag.edges'),
      constants = this.get('constants'),
      vertexIdToNode = {},
      depthToNodes = [], // Array of id arrays
      dagVisualModel = this.get('dagVisualModel'),
      selectedVertex = this.get('selectedVertex'),
      minVertexDuration = Number.MAX_VALUE,
      maxVertexDuration = Number.MIN_VALUE;

    //
    // CALCULATE DEPTH - BFS to get correct graph depth
    //
    var visitEdges = [];
    var maxRowLength = 0;
    var maxRowDepth = 0;
    vertices.forEach(function (v) {
      if (v.get('incomingEdges.length') < 1) {
        visitEdges.push({
          depth: 0,
          parent: null,
          toVertex: v
        });
      }
    });
    function getNodeFromEdge(edgeObj) {
      var vertex = edgeObj.toVertex;
      var pName = edgeObj.parent ? edgeObj.parent.name : null;
      var cName = edgeObj.toVertex ? edgeObj.toVertex.get('name') : null;
      if (edgeObj.parent && edgeObj.depth < edgeObj.parent.depth + 1) {
        edgeObj.depth = edgeObj.parent.depth + 1;
      }
      var node = vertexIdToNode[vertex.get('id')];
      for (var k = depthToNodes.length; k <= edgeObj.depth; k++) {
        depthToNodes.push([]);
      }
      if (!node) {
        // New node
        node = {
          id: vertex.get('id'),
          name: vertex.get('name'),
          state: vertex.get('state'),
          type: vertex.get('type'),
          operations: vertex.get('operations'),
          depth: edgeObj.depth,
          parents: [],
          children: [],
          x: 0,
          y: 0,
          metricType: null,
          metricDisplay: null,
          metricPercent: -1,
          selected: selectedVertex != null ? selectedVertex.get('id') == vertex.get('id') : false,
          fixed: true,
          metrics: {
            input: -1,
            output: -1,
            recordsRead: -1,
            recordsWrite: -1,
            tezTasks: -1
          },
          duration: vertex.get('duration')
        };
        if (node.duration < minVertexDuration && node.duration > 0) {
          minVertexDuration = node.duration;
        }
        if (node.duration > maxVertexDuration && node.duration > 0) {
          maxVertexDuration = node.duration;
        }
        vertexIdToNode[vertex.get('id')] = node;
        depthToNodes[node.depth].push(node);
      } else {
        // Existing node
        if (edgeObj.depth > node.depth) {
          function moveNodeToDepth(node, newDepth) {
            var oldIndex = depthToNodes[node.depth].indexOf(node);
            depthToNodes[node.depth].splice(oldIndex, 1);
            node.depth = newDepth;
            if (!depthToNodes[node.depth]) {
              depthToNodes[node.depth] = [];
            }
            depthToNodes[node.depth].push(node);
            if (node.children) {
              // Move children down depth
              node.children.forEach(function (c) {
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
        outEdges.forEach(function (oe) {
          var childVertex = oe.get('toVertex');
          visitEdges.push({
            depth: node.depth + 1,
            parent: node,
            toVertex: childVertex
          });
        });
      }
    }
    edges.forEach(function (e) {
      dagVisualModel.links.push({
        source: vertexIdToNode[e.get('fromVertex.id')],
        target: vertexIdToNode[e.get('toVertex.id')],
        edgeType: e.get('edgeType')
      });
    });
    // Sort nodes so that parents stay together
    for (var depth = 0; depth < depthToNodes.length; depth++) {
      var nodes = depthToNodes[depth];
      nodes.sort(function (n1, n2) {
        var ck1 = '';
        var ck2 = '';
        if (n1.children) {
          n1.children.forEach(function (c) {
            ck1 += c.name;
          });
        }
        if (n2.children) {
          n2.children.forEach(function (c) {
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
    var updateNodeEffectiveWidth = function (node, newEffectiveWidth) {
      if (App.Helpers.number.validateInteger(node.effectiveWidth) != null) {
        node.effectiveWidth = newEffectiveWidth;
      }
      var diff = newEffectiveWidth - node.effectiveWidth;
      if (diff > 0) {
        var oldEffectiveWidth = node.effectiveWidth;
        node.effectiveWidth = newEffectiveWidth;
        if (node.parents != null) {
          node.parents.forEach(function (parent) {
            updateNodeEffectiveWidth(parent, parent.effectiveWidth + diff);
          })
        }
      }
    };
    var xGap = 20;
    var yGap = 70;
    var currentY = 40;
    // First pass - calculate layout widths, and Y coordinates
    for (var depth = 0; depth < depthToNodes.length; depth++) {
      var nodes = depthToNodes[depth];
      var maxNodeHeight = 0;
      for (var nodeIndex = 0; nodeIndex < nodes.length; nodeIndex++) {
        var node = nodes[nodeIndex];
        var nodeDim = this.getNodeCalculatedDimensions(node, minVertexDuration, maxVertexDuration);
        node.drawWidth = nodeDim.drawWidth;
        node.drawHeight = nodeDim.drawHeight;
        node.scale = nodeDim.scale;
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
          node.children.forEach(function (child) {
            var childDim = self.getNodeCalculatedDimensions(child, minVertexDuration, maxVertexDuration);
            childrenWidth += ((childDim.width + xGap) / child.parents.length);
          });
          updateNodeEffectiveWidth(node, Math.max(childrenWidth, (node.width + xGap)));
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
    for (var depth = 0; depth < depthToNodes.length; depth++) {
      var nodes = depthToNodes[depth];
      var currentX = -1;
      var parentCurrentXMap = {};
      for (var nodeIndex = 0; nodeIndex < nodes.length; nodeIndex++) {
        var node = nodes[nodeIndex];
        var parentsKey = null;
        if (node.parents != null && node.parents.length > 0) {
          var parentMidX = 0;
          var parentsKey = '';
          var childrenEffectiveWidth = -1;
          node.parents.forEach(function (parent) {
            parentMidX += (parent.x + (parent.width / 2));
            parentsKey += (parent.id + '//');
            if (childrenEffectiveWidth < 0) {
              parent.children.forEach(function (c) {
                childrenEffectiveWidth += (c.effectiveWidth);
              });
            }
          });
          parentMidX = parentMidX / node.parents.length;
          var parentCurrentX = parentCurrentXMap[parentsKey];
          if (parentCurrentX == null || parentCurrentX == undefined) {
            parentCurrentX = parentMidX - (childrenEffectiveWidth / 2);
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
    var force = d3.layout.force().nodes(dagVisualModel.nodes).links(dagVisualModel.links).start();
    var nodeDragData = {
      nodeRelativeX: 0,
      nodeRelativeY: 0
    };
    var nodeDrag = d3.behavior.drag().on('dragstart',function (node) {
      d3.event.sourceEvent.stopPropagation();
      var rc = d3.mouse(this);
      nodeDragData.nodeRelativeX = (rc[0] * node.scale);
      nodeDragData.nodeRelativeY = (rc[1] * node.scale);
    }).on('drag',function (node) {
        var nx = d3.event.x - nodeDragData.nodeRelativeX;
        var ny = d3.event.y - nodeDragData.nodeRelativeY;
        self.dragVertex(d3.select(this), node, [nx, ny], diagonal);
      }).on('dragend', function () {
        nodeDragData.nodeRelativeX = 0;
        nodeDragData.nodeRelativeY = 0;
      });
    // Create Links
    var diagonal = d3.svg.diagonal().source(function (d) {
      return {
        x: d.source.outgoingX,
        y: d.source.outgoingY
      };
    }).target(function (d) {
        return {
          x: d.target.incomingX,
          y: d.target.incomingY - 12
        }
      });
    var link = svgLayer.selectAll(".link-g").data(dagVisualModel.links).enter().append("g").attr("class", "link-g").attr("marker-end", "url(#arrow)");
    link.append("path").attr("class",function (l) {
      var classes = "link svg-tooltip ";
      if (l.edgeType) {
        classes += ("type-" + l.edgeType.toLowerCase() + " ");
      } else {
        classes += "type-unknown ";
      }
      return classes;
    }).attr("d", diagonal).attr("title", function (l) {
        var lower = l.edgeType ? l.edgeType.toLowerCase() : '';
        return Em.I18n.t("jobs.hive.tez.edge." + lower);
      });
    // Create Nodes
    var node = svgLayer.selectAll(".node").data(dagVisualModel.nodes).enter().append("g").attr("class", "node").call(nodeDrag);
    node.append("rect").attr("class", "background").attr("width",function (n) {
      return n.drawWidth;
    }).attr("height",function (n) {
        return n.drawHeight;
      }).attr("rx", "10").attr("filter", "url(#shadow)").on('mousedown', function (n) {
        //var vertex = App.TezDagVertex.find(n.id);
        var vertex = App.HiveJob.store.getById('tezDagVertex', n.id);
        if (vertex != null) {
          self.get('parentView').doSelectVertex(vertex, true);
        }
      });
    node.each(function (n) {
      var ops = n.operations;
      var opCount = {};
      if (ops != null && ops.length > 0) {
        var opGroups = d3.select(this).selectAll(".operation").data(ops).enter().append("g").attr("class", "operation").attr("transform",function (op, opIndex) {
          var row = Math.floor(opIndex / 3);
          var column = opIndex % 3;
          return "translate(" + (10 + column * 55) + "," + (37 + row * 20) + ")";
        }).attr("clip-path", "url(#operatorClipPath)").attr("opIndex",function (op) {
            if (!opCount[op]) {
              opCount[op] = 1;
            }
            else {
              opCount[op] = opCount[op] + 1;
            }
            return opCount[op];
          }).on('mouseover', function (op) {
            var template = self.createChildView(App.HoverOpTable, {
              content: {
                operationName: op,
                operatorPlanObj: self.createOperationPlanObj(n.name, op, this.getAttribute('opIndex'))
              }
            });
            $(this).find('.svg-tooltip').
              attr('title', template.renderToBuffer().string()).
              tooltip('fixTitle').tooltip('show');
          });

        opGroups.append("rect").attr("class", "operation svg-tooltip ").attr("width", "50").attr("height", "16");
        opGroups.append("text").attr("x", "2").attr("dy", "1em").text(function (op) {
          return op != null ? op.split(' ')[0] : '';
        });
      }
    });
    var metricNodes = node.append("g").attr("class", "metric").attr("transform", "translate(112,7)");
    metricNodes.append("rect").attr("width",function (n) {
      if (n.type == App.TezDagVertexType.UNION) {
        return 0;
      }
      return 60;
    }).attr("height",function (n) {
        if (n.type == App.TezDagVertexType.UNION) {
          return 0;
        }
        return 18;
      }).attr("rx", "3").attr("class", "metric-title svg-tooltip");
    metricNodes.append("text").attr("class", "metric-text").attr("x", "2").attr("dy", "1em");
    node.append("text").attr("x", "1.9em").attr("dy", "1.5em").text(function (d) {
      return d.name;
    });
    var iconContainer = node.append('g').attr('class', 'vertex-icon-container').attr('transform', 'translate(10,10)');
    iconContainer.append('rect').attr('width', '1em').attr('height', '1em').attr('class', 'vertex-icon-rect  svg-tooltip ');
    iconContainer.append('text').attr('dy', '10px').attr("font-family", "FontAwesome").attr('class', 'vertex-icon-text');
    node.attr("transform", function (d) {
      return "translate(" + d.x + "," + d.y + ") scale(" + d.scale + ") ";
    });
    this.vertexMetricsUpdated();
    $('.svg-tooltip').each(function () {
      var item = $(this);
      if (item.prop('tagName') == 'path') {
        item.hover(function (e) {
          var offset = $(this).offset();
          item.prop('offsetWidth', function () {
            return 2 * (e.pageX - offset.left);
          });
          item.prop('offsetHeight', function () {
            return 2 * (e.pageY - offset.top);
          });
        });
      }
      if (item.prop('offsetWidth') == undefined) {
        item.prop('offsetWidth', function () {
          return item.width();
        });
      }
      if (item.prop('offsetHeight') == undefined) {
        item.prop('offsetHeight', function () {
          return item.height();
        });
      }
    });
    App.tooltip($('.svg-tooltip'), {
      container: 'body',
      html: true,
      placement: 'bottom',
      template: '<div class="tooltip jobs-tooltip"><div class="tooltip-arrow"></div><div class="tooltip-inner"></div></div>'
    });

    // Position in center
    var translateX = Math.round((width - canvasWidth) / 2);
    if (translateX > 0) {
      svgLayer.attr("transform", "translate(" + translateX + ",0)");
    }
    return {
      width: canvasWidth,
      height: canvasHeight
    }
  },

  dragVertex: function (d3Vertex, node, newPosition, diagonal) {
    // Move vertex
    node.x = newPosition[0];
    node.y = newPosition[1];
    node.incomingX = newPosition[0] + (node.width / 2);
    node.incomingY = newPosition[1];
    node.outgoingX = newPosition[0] + (node.width / 2);
    node.outgoingY = newPosition[1] + node.height;
    d3Vertex.attr('transform', 'translate(' + newPosition[0] + ',' + newPosition[1] + ') scale(' + node.scale + ') ');
    // Move links
    d3.selectAll('.link').filter(function (l) {
      if (l && (l.source === node || l.target === node)) {
        return this
      }
      return null;
    }).attr('d', diagonal);
  },

  /**
   * Refreshes UI of the Tez graph with latest values
   * @method refreshGraphUI
   */
  refreshGraphUI: function () {
    var svgLayer = this.get('svgVerticesLayer');
    if (svgLayer != null) {
      var self = this,
        metricNodes = svgLayer.selectAll(".metric"),
        metricNodeTexts = svgLayer.selectAll(".metric-text"),
        metricNodeTitles = svgLayer.selectAll(".metric-title"),
        nodeBackgrounds = svgLayer.selectAll(".background"),
        vertexIconTexts = svgLayer.selectAll(".vertex-icon-text"),
        vertexIconRects = svgLayer.selectAll(".vertex-icon-rect");
      metricNodes.attr("class", function (node) {
        var classes = "metric ",
          percent = node.metricPercent;
        if (App.Helpers.number.validateInteger(percent) == null && percent >= 0) {
          if (percent <= 20) {
            classes += "heat-0-20 ";
          }
          else
            if (percent <= 40) {
              classes += "heat-20-40 ";
            }
            else
              if (percent <= 60) {
                classes += "heat-40-60 ";
              }
              else
                if (percent <= 80) {
                  classes += "heat-60-80 ";
                }
                else
                  if (percent <= 100) {
                    classes += "heat-80-100 ";
                  }
                  else {
                    classes += "heat-none";
                  }
        }
        else {
          classes += "heat-none";
        }
        return classes;
      });
      metricNodeTexts.text(function (node) {
        if (node.type == App.TezDagVertexType.UNION) {
          return '';
        }
        return node.metricDisplay;
      });
      metricNodeTitles.attr("title",function (node) {
        return node.metricType;
      }).attr("data-original-title", function (node) {
          return node.metricType;
        });
      nodeBackgrounds.attr("class", function (n) {
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
      vertexIconRects.attr('title',function (node) {
        return App.Helpers.string.getCamelCase(node.state);
      }).attr('data-original-title', function (node) {
          return App.Helpers.string.getCamelCase(node.state);
        });
      vertexIconTexts.text(function (n) {
        return self.getVertexIcon(n)
      }).attr('class', function (n) {
          var classes = 'vertex-icon-text ';
          if (n.state != null) {
            if (n.state == App.TezDagVertexState.JOBFAILED) {
              classes += App.TezDagVertexState.FAILED.toLowerCase();
            }
            else {
              classes += n.state.toLowerCase();
            }
          }
          return classes;
        });
    }
  },

  /**
   * Get icon for vertex according to node state
   * @param {object} node
   * @returns {string}
   * @method getVertexIcon
   */
  getVertexIcon: function (node) {
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
      case App.TezDagVertexState.JOBFAILED:
        icon = '\uF05C'; //icon-remove-circle
        break;
    }
    return icon;
  },

  /**
   * Determines the size of a node by taking into account its duration and
   * number of operations performed.
   *
   * @return {Object} Provides various metrics necessary in drawing a node.
   * <code>
   * {
   *  width: 360, // Scaled width of the node
   *  height: 80, // Scaled height of the node
   *  scale: 2, // Scale used on vertex dimensions. Quickest vertex is scaled to 1 and slowest vertex is scaled to 10.
   *  drawWidth: 180, // Width of actual drawing (that will be scaled)
   *  drawHeight: 40 // Height of actual drawing (that will be scaled)
   * }
   * </code>
   * @method getNodeCalculatedDimensions
   */
  getNodeCalculatedDimensions: function (node) {
    var size = {
      width: 180,
      height: 40,
      drawWidth: 180,
      drawHeight: 40,
      scale: 1
    };
    if (node.operations && node.operations.length > 0) {
      var opsHeight = Math.ceil(node.operations.length / 3);
      size.drawHeight += (opsHeight * 20);
    }
    size.width = size.drawWidth * size.scale;
    size.height = size.drawHeight * size.scale;
    return size;
  }

});

App.HoverOpTable = Em.View.extend({
  templateName: 'job/hover_op_table'
});
