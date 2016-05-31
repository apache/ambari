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

/* globals dagre */

import Ember from 'ember';
import dagRules from '../utils/dag-rules';
import utils from 'hive/utils/functions';

export default Ember.View.extend({
  verticesGroups: [],
  edges: [],

  willInsertElement: function () {
    this.set('graph', new dagre.graphlib.Graph());
  },

  didInsertElement: function () {
    this._super();

    var target = this.$('#visual-explain');
    var panel = this.$('#visual-explain .panel-body');

    panel.css('min-height', $('.main-content').height());
    target.animate({ width: $('.main-content').width() }, 'fast');

    this.$('#visual-explain-graph').draggable();

    if (this.get('controller.rerender')) {
      this.renderDag();
    }
  },

  willDestroyElement: function () {
    var target = this.$('#visual-explain');
    var panel = this.$('#visual-explain .panel-body');

    panel.css('min-height', 0);
    target.css('width', 0);
  },

  updateProgress: function () {
    var verticesProgress = this.get('controller.verticesProgress');
    var verticesGroups = this.get('verticesGroups');

    if (!verticesGroups || !verticesProgress || !verticesProgress.length) {
      return;
    }

    verticesGroups.forEach(function (verticesGroup) {
      verticesGroup.contents.forEach(function (node) {
        var progress = verticesProgress.findBy('name', node.get('label'));

        if (progress) {
          node.set('progress', progress.get('value'));
        }
      });
    });
  }.observes('controller.verticesProgress.@each.value', 'verticesGroups'),

  jsonChanged: function () {
    var json = this.get('controller.json');
    this.renderDag();
  }.observes('controller.json'),

  getOffset: function (el) {
    var _x = 0;
    var _y = 0;
    var _w = el.offsetWidth|0;
    var _h = el.offsetHeight|0;
    while( el && !isNaN( el.offsetLeft ) && !isNaN( el.offsetTop ) ) {
        _x += el.offsetLeft - el.scrollLeft;
        _y += el.offsetTop - el.scrollTop;
        el = el.offsetParent;
    }
    return { top: _y, left: _x, width: _w, height: _h };
  },

  addEdge: function (div1, div2, thickness, type) {
    var off1 = this.getOffset(div1);
    var off2 = this.getOffset(div2);
    // bottom right
    var x1 = off1.left + off1.width / 2;
    var y1 = off1.top + off1.height;
    // top right
    var x2 = off2.left + off2.width / 2;
    var y2 = off2.top;
    // distance
    var length = Math.sqrt(((x2-x1) * (x2-x1)) + ((y2-y1) * (y2-y1)));
    // center
    var cx = ((x1 + x2) / 2) - (length / 2);
    var cy = ((y1 + y2) / 2) - (thickness / 2) - 73;
    // angle
    var angle = Math.round(Math.atan2((y1-y2), (x1-x2)) * (180 / Math.PI));

    if (angle < -90) {
      angle = 180 + angle;
    }

    var style = "left: %@px; top: %@px; width: %@px; transform:rotate(%@4deg);";
    style = style.fmt(cx, cy, length, angle);

    var edgeType;

    if (type) {
      if (type === 'BROADCAST_EDGE') {
        edgeType = 'BROADCAST';
      } else {
        edgeType = 'SHUFFLE';
      }
    }

    this.get('edges').pushObject({
      style: style,
      type: edgeType
    });
  },

  getNodeContents: function (operator, contents, table, vertex) {
    var currentTable = table,
      contents = contents || [],
      nodeName,
      node,
      ruleNode,
      nodeLabelValue,
      self = this;

    if (operator.constructor === Array) {
      operator.forEach(function (childOperator) {
        self.getNodeContents(childOperator, contents, currentTable, vertex);
      });

      return contents;
    } else {
      nodeName = Object.getOwnPropertyNames(operator)[0];
      node = operator[nodeName];
      ruleNode = dagRules.findBy('targetOperator', nodeName);

      if (ruleNode) {
        if (nodeName.indexOf('Map Join') > -1) {
          nodeLabelValue = this.handleMapJoinNode(node, currentTable);
          currentTable = null;
        } else if (nodeName.indexOf('Merge Join') > -1) {
          nodeLabelValue = this.handleMergeJoinNode(node, vertex);
        } else {
          nodeLabelValue = node[ruleNode.targetProperty];
        }

        contents.pushObject({
          title: ruleNode.label,
          statistics: node["Statistics:"],
          index: contents.length + 1,
          value: nodeLabelValue,
          fields: ruleNode.fields.map(function (field) {
            var value = node[field.targetProperty || field.targetProperties];

            return {
              label: field.label,
              value: value
            };
          })
        });

        if (node.children) {
          return this.getNodeContents(node.children, contents, currentTable, vertex);
        } else {
          return contents;
        }
      } else {
        return contents;
      }
    }
  },

  handleMapJoinNode: function (node, table) {
    var rows = table || "<rows from above>";
    var firstTable = node["input vertices:"][0] || rows;
    var secondTable = node["input vertices:"][1] || rows;

    var joinString = node["condition map:"][0][""];
    joinString = joinString.replace("0", firstTable);
    joinString = joinString.replace("1", secondTable);
    joinString += " on ";
    joinString += node["keys:"][0] + "=";
    joinString += node["keys:"][1];

    return joinString;
  },

  handleMergeJoinNode: function (node, vertex) {
    var graphData = this.get('controller.json')['STAGE PLANS']['Stage-1']['Tez'];
    var edges = graphData['Edges:'];
    var index = 0;
    var joinString = node["condition map:"][0][""];

    edges[vertex].toArray().forEach(function (edge) {
      if (edge.type === "SIMPLE_EDGE") {
        joinString.replace(String(index), edge.parent);
        index++;
      }
    });

    return joinString;
  },

  //sets operator nodes
  setNodes: function (vertices) {
    var g = this.get('graph');
    var self = this;

    vertices.forEach(function (vertex) {
      var contents = [];
      var operator;
      var currentTable;

      if (vertex.name.indexOf('Map') > -1) {
        if (vertex.value && vertex.value['Map Operator Tree:']) {
          operator = vertex.value['Map Operator Tree:'][0];
          currentTable = operator["TableScan"]["alias:"];
        } else {
          //https://hortonworks.jira.com/browse/BUG-36168
          operator = "None";
        }
      } else if (vertex.name.indexOf('Reducer') > -1) {
        operator = vertex.value['Reduce Operator Tree:'];
      }

      if (operator) {
        contents = self.getNodeContents(operator, null, currentTable, vertex.name);

        g.setNode(vertex.name, {
          contents: contents,
          id: vertex.name,
          label: vertex.name
        });
      }
    });

    return this;
  },

  //sets edges between operator nodes
  setEdges: function (edges) {
    var g = this.get('graph');
    var invalidEdges = [];
    var edgesToBeRemoved = [];
    var isValidEdgeType = function (type) {
      return type === "SIMPLE_EDGE" ||
             type === "BROADCAST_EDGE";
    };

    edges.forEach(function (edge) {
      var parent;
      var type;

      if (edge.value.constructor === Array) {
        edge.value.forEach(function (childEdge) {
          parent = childEdge.parent;
          type = childEdge.type;

          if (isValidEdgeType(type)) {
            g.setEdge(parent, edge.name);
            g.edge({v: parent, w: edge.name}).type = type;
          } else {
            invalidEdges.pushObject({
              vertex: edge.name,
              edge: childEdge
            });
          }
        });
      } else {
        parent = edge.value.parent;
        type = edge.value.type;

        if (isValidEdgeType(type)) {
          g.setEdge(parent, edge.name);
          g.edge({v: parent, w: edge.name}).type = type;
        } else {
          invalidEdges.pushObject({
            vertex: edge.name,
            edge: edge.name
          });
        }
      }
    });

    invalidEdges.forEach(function (invalidEdge) {
      var parent;
      var targetEdge = g.edges().find(function (graphEdge) {
        return graphEdge.v === invalidEdge.edge.parent ||
               graphEdge.w === invalidEdge.edge.parent;
      });

      var targetVertex;

      if (targetEdge) {
        edgesToBeRemoved.pushObject(targetEdge);

        if (targetEdge.v === invalidEdge.edge.parent) {
          targetVertex = targetEdge.w;
        } else {
          targetVertex = targetEdge.v;
        }

        parent = invalidEdge.vertex;

        g.setEdge({v: parent, w: targetVertex});
        g.setEdge({v: parent, w: targetVertex}).type = "BROADCAST_EDGE";
      }
    });

    edgesToBeRemoved.uniq().forEach(function (edge) {
      g.removeEdge(edge.v, edge.w, edge.name);
    });

    return this;
  },

  //sets nodes for tables and their edges
  setTableNodesAndEdges: function (vertices) {
    var g = this.get('graph');

    vertices.forEach(function (vertex) {
      var operator;
      var table;
      var id;

      if (vertex.name.indexOf('Map') > -1 && vertex.value && vertex.value['Map Operator Tree:']) {
        operator = vertex.value['Map Operator Tree:'][0];
        for (var node in operator) {
          table = operator[node]['alias:'];

          //create unique identifier by using table + map pairs so that we have
          //different nodes for the same table if it's a table connected to multiple Map operators
          id = table + ' for ' + vertex.name;

          g.setNode(id, { id: id, label: table, isTableNode: true });
          g.setEdge(id, vertex.name);
        }
      }
    });

    dagre.layout(g);

    return this;
  },

  createNodeGroups: function () {
    var groupedNodes = [];
    var g = this.get('graph');
    var lastRowNode;
    var fileOutputOperator;

    g.nodes().forEach(function (value) {
      var node = g.node(value);

      if (node) {
        var existentRow = groupedNodes.findBy('topOffset', node.y);

        if (!existentRow) {
           groupedNodes.pushObject({
              topOffset: node.y,
              contents: [ Ember.Object.create(node) ]
           });
        } else {
          existentRow.contents.pushObject(Ember.Object.create(node));
        }
      }
    });

    groupedNodes = groupedNodes.sortBy('topOffset');
    groupedNodes.forEach(function (group) {
      group.contents = group.contents.sortBy('x');
    });

    lastRowNode = groupedNodes.get('lastObject.contents.lastObject');
    fileOutputOperator = lastRowNode.contents.get('lastObject');

    g.setNode(fileOutputOperator.title, { id: fileOutputOperator.title, label: fileOutputOperator.title, isOutputNode: true });
    g.setEdge(fileOutputOperator.title, lastRowNode.id);

    groupedNodes.pushObject({
      contents: [ Ember.Object.create(g.node(fileOutputOperator.title)) ]
    });

    lastRowNode.contents.removeObject(fileOutputOperator);

    this.set('verticesGroups', groupedNodes);

    return this;
  },

  renderEdges: function () {
    var self = this;
    var g = this.get('graph');

    Ember.run.later(function () {
      g.edges().forEach(function (value) {
        var firstNode = self.$("[title='" + value.v + "']");
        var secondNode = self.$("[title='" + value.w + "']");

        if (firstNode && secondNode) {
          self.addEdge(firstNode[0], secondNode[0], 2, g.edge(value).type);
        }

      });
    }, 400);
  },

  renderDag: function () {
    var json = this.get('controller.json');
    var isVisualExplain = json && (json['STAGE PLANS'] != undefined) &&  (json['STAGE PLANS']['Stage-1'] != undefined) && (json['STAGE PLANS']['Stage-1']['Tez'] != undefined);
    if (isVisualExplain) {
      this.set('edges', []);

      // Create a new directed graph
      var g = this.get('graph');

      var graphData = json['STAGE PLANS']['Stage-1']['Tez'];
      var vertices = utils.convertToArray(graphData['Vertices:']);
      var edges = utils.convertToArray(graphData['Edges:']);

      // Set an object for the graph label
      g.setGraph({});

      // Default to assigning a new object as a label for each new edge.
      g.setDefaultEdgeLabel(function () { return {}; });

      this.setNodes(vertices)
          .setEdges(edges)
          .setTableNodesAndEdges(vertices)
          .createNodeGroups()
          .renderEdges();

      this.set('controller.showSpinner', true);

    } else {

      if(!this.get('controller.noquery')) {
        $('#no-visual-explain-graph').html('Visual explain is not available.');
      }

    }

  }
});
