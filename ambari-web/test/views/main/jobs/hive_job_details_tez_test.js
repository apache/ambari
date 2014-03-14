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
module.exports = {
  _createVertex : function(row, col, state, type, numOps, inEdges, outEdges, vertexJsons) {
    var v = {
      id : 'v_' + row + '_' + col,
      instance_id : 'vi_' + row + '_' + col,
      name : 'Vertex ' + row + ', ' + col,
      state : state,
      type : type,
      operations : [],
      outgoing_edges : outEdges,
      incoming_edges : inEdges
    };
    for ( var c = 0; c < numOps; c++) {
      v.operations.push("Op " + c);
    }
    vertexJsons.push(v);
    return v;
  },

  _createEdge : function(id, type, from, to, edgeJsons) {
    var e = {
      id : id,
      instance_id : 'i_' + id,
      from_vertex_id : from.id,
      to_vertex_id : to.id,
      edge_type : type
    }
    edgeJsons.push(e);
    return e;
  },

  /**
   * Creates a Tez DAG for test purposes with 6 nodes in row 1, 1 node in row 2
   * and 5 nodes in row 3.
   *
   * Usage: <code>
   *     var testDag = jobUtils.createTezDag_6x1x5();
   *     vertices = testDag.get('vertices');
   *     edges = testDag.get('edges');
   * </code>
   */
  createTezDag_6x1x5 : function() {
    var vertices = [];
    var dagJson = {
      id : 'dag1',
      instance_id : 'dag1',
      name : 'Test DAG 1',
      stage : 'My stage',
      vertices : [],
      edges : []
    };
    var vertexJsons = [];
    var edgeJsons = [];
    // Row 1
    var v1 = this._createVertex(1, 1, "FAILED", App.TezDagVertexType.MAP, 30, [], [ 'e1' ], vertexJsons);
    var v2 = this._createVertex(1, 2, "RUNNING", App.TezDagVertexType.REDUCE, 2, [], [ 'e2' ], vertexJsons);
    var v3 = this._createVertex(1, 3, "FAILED", App.TezDagVertexType.MAP, 5, [], [ 'e3' ], vertexJsons);
    var v4 = this._createVertex(1, 4, "FAILED", App.TezDagVertexType.REDUCE, 10, [], [ 'e4' ], vertexJsons);
    var v5 = this._createVertex(1, 5, "FAILED", App.TezDagVertexType.MAP, 15, [], [ 'e5' ], vertexJsons);
    var v6 = this._createVertex(1, 6, "FAILED", App.TezDagVertexType.REDUCE, 20, [], [ 'e6' ], vertexJsons);
    // Row 2
    var v7 = this._createVertex(2, 1, "SUCCEEDED", App.TezDagVertexType.UNION, 30, [ 'e1', 'e2', 'e3', 'e4', 'e5', 'e6' ], [ 'e7', 'e8', 'e9', 'e10', 'e11' ], vertexJsons);
    // Row 3
    var v8 = this._createVertex(3, 1, "FAILED", App.TezDagVertexType.REDUCE, 30, [ 'e7' ], [], vertexJsons);
    var v9 = this._createVertex(3, 2, "RUNNING", App.TezDagVertexType.MAP, 2, [ 'e8' ], [], vertexJsons);
    var v10 = this._createVertex(3, 3, "FAILED", App.TezDagVertexType.REDUCE, 5, [ 'e9' ], [], vertexJsons);
    var v11 = this._createVertex(3, 4, "FAILED", App.TezDagVertexType.MAP, 10, [ 'e10' ], [], vertexJsons);
    var v12 = this._createVertex(3, 5, "FAILED", App.TezDagVertexType.REDUCE, 15, [ 'e11' ], [], vertexJsons);
    // Edges 1-2
    this._createEdge('e1', 'BROADCAST', v1, v7, edgeJsons);
    this._createEdge('e2', 'BROADCAST', v2, v7, edgeJsons);
    this._createEdge('e3', 'BROADCAST', v3, v7, edgeJsons);
    this._createEdge('e4', 'SCATTER_GATHER', v4, v7, edgeJsons);
    this._createEdge('e5', 'SCATTER_GATHER', v5, v7, edgeJsons);
    this._createEdge('e6', 'SCATTER_GATHER', v6, v7, edgeJsons);
    // Edges 2-3
    this._createEdge('e7', 'SCATTER_GATHER', v7, v8, edgeJsons);
    this._createEdge('e8', 'SCATTER_GATHER', v7, v9, edgeJsons);
    this._createEdge('e9', 'SCATTER_GATHER', v7, v10, edgeJsons);
    this._createEdge('e10', 'BROADCAST', v7, v11, edgeJsons);
    this._createEdge('e11', 'BROADCAST', v7, v12, edgeJsons);
    vertexJsons.forEach(function(v) {
      dagJson.vertices.push(v.id);
    })
    edgeJsons.forEach(function(e) {
      dagJson.edges.push(e.id);
    })
    App.store.load(App.TezDag, dagJson);
    App.store.loadMany(App.TezDagVertex, vertexJsons);
    App.store.loadMany(App.TezDagEdge, edgeJsons);
    return App.TezDag.find('dag1');
  },

  /**
   * Creates a Tez DAG for test purposes with 6 nodes in row 1, 1 node in row 2
   * and 5 nodes in row 3.
   *
   * Usage: <code>
   *     var testDag = jobUtils.createTezDag_7x1_1x1();
   *     vertices = testDag.get('vertices');
   *     edges = testDag.get('edges');
   * </code>
   */
  createTezDag_7x1_1x1 : function() {
    var vertices = [];
    var dagJson = {
      id : 'dag1',
      instance_id : 'dag1',
      name : 'Test DAG 1',
      stage : 'My stage',
      vertices : [],
      edges : []
    };
    var vertexJsons = [];
    var edgeJsons = [];
    // Row 1
    var v1 = this._createVertex(1, 1, "FAILED", App.TezDagVertexType.REDUCE, 30, [], [ 'e1' ], vertexJsons);
    var v4 = this._createVertex(1, 4, "FAILED", App.TezDagVertexType.MAP, 10, [], [ 'e4' ], vertexJsons);
    var v6 = this._createVertex(1, 6, "FAILED", App.TezDagVertexType.REDUCE, 20, [], [ 'e6' ], vertexJsons);
    var v2 = this._createVertex(1, 2, "RUNNING", App.TezDagVertexType.MAP, 2, [], [ 'e2' ], vertexJsons);
    var v3 = this._createVertex(1, 3, "FAILED", App.TezDagVertexType.REDUCE, 5, [], [ 'e3' ], vertexJsons);
    var v5 = this._createVertex(1, 5, "FAILED", App.TezDagVertexType.MAP, 15, [], [ 'e5' ], vertexJsons);
    var v7 = this._createVertex(1, 7, "FAILED", App.TezDagVertexType.REDUCE, 4, [], [ 'e7' ], vertexJsons);
    // Row 2
    var v8 = this._createVertex(2, 1, "SUCCEEDED", App.TezDagVertexType.MAP, 30, [ 'e1', 'e2', 'e3', 'e4' ], [ 'e8' ], vertexJsons);
    var v9 = this._createVertex(2, 2, "FAILED", App.TezDagVertexType.REDUCE, 30, [ 'e5', 'e6', 'e7' ], ['e9'], vertexJsons);
    // Row 3
    var v10 = this._createVertex(3, 1, "RUNNING", App.TezDagVertexType.UNION, 2, [ 'e8', 'e9' ], [], vertexJsons);
    // Edges 1-2
    this._createEdge('e1', 'BROADCAST', v1, v8, edgeJsons);
    this._createEdge('e2', 'BROADCAST', v2, v8, edgeJsons);
    this._createEdge('e3', 'BROADCAST', v3, v8, edgeJsons);
    this._createEdge('e4', 'SCATTER_GATHER', v4, v8, edgeJsons);
    this._createEdge('e5', 'SCATTER_GATHER', v5, v9, edgeJsons);
    this._createEdge('e6', 'SCATTER_GATHER', v6, v9, edgeJsons);
    this._createEdge('e7', 'SCATTER_GATHER', v7, v9, edgeJsons);
    // Edges 2-3
    this._createEdge('e8', 'SCATTER_GATHER', v8, v10, edgeJsons);
    this._createEdge('e9', 'SCATTER_GATHER', v9, v10, edgeJsons);
    vertexJsons.forEach(function(v) {
      dagJson.vertices.push(v.id);
    })
    edgeJsons.forEach(function(e) {
      dagJson.edges.push(e.id);
    })
    App.store.load(App.TezDag, dagJson);
    App.store.loadMany(App.TezDagVertex, vertexJsons);
    App.store.loadMany(App.TezDagEdge, edgeJsons);
    return App.TezDag.find('dag1');
  },

  /**
   * Creates a Tez DAG for test purposes. Each row in the graph is fully
   * connected to the next row. The number of nodes in each row is passed as
   * input.
   *
   * Usage:
   * <code>
   *  var testDag = jobUtils._test_createTezDag_fullyConnected([10,3,8]);
   *  vertices = testDag.get('vertices');
   *  edges = testDag.get('edges');
   * </code>
   */
  createTezDag_fullyConnected : function(rowCounts) {
    var vertices = [];
    var dagJson = {
      id : 'dag1',
      instance_id : 'dag1',
      name : 'Test DAG 1',
      stage : 'My stage',
      vertices : [],
      edges : []
    };
    var vertexJsons = [];
    var edgeJsons = [];
    var matrix = new Array(rowCounts.length);
    for ( var r = 0; r < rowCounts.length; r++) {
      matrix[r] = new Array(rowCounts[r]);
      for ( var c = 0; c < rowCounts[r]; c++) {
        var outs = [];
        var ins = [];
        if (r < rowCounts.length - 1) {
          for ( var c2 = 0; c2 < rowCounts[r + 1]; c2++) {
            outs.push('e_' + r + c + '_' + (r + 1) + c2);
          }
        }
        if (r > 0) {
          for ( var c2 = 0; c2 < rowCounts[r - 1]; c2++) {
            ins.push('e_' + (r - 1) + c2 + '_' + r + c);
          }
        }
        matrix[r][c] = this._createVertex(r, c, "RUNNING", true, (r + 1) * (c + 1), ins, outs, vertexJsons);
        if (r > 0) {
          for ( var c2 = 0; c2 < rowCounts[r - 1]; c2++) {
            this._createEdge('e_' + (r - 1) + c2 + '_' + r + c, 'BROADCAST', matrix[r - 1][c2], matrix[r][c], edgeJsons);
          }
        }
      }
    }
    vertexJsons.forEach(function(v) {
      dagJson.vertices.push(v.id);
    })
    edgeJsons.forEach(function(e) {
      dagJson.edges.push(e.id);
    })
    App.store.load(App.TezDag, dagJson);
    App.store.loadMany(App.TezDagVertex, vertexJsons);
    App.store.loadMany(App.TezDagEdge, edgeJsons);
    return App.TezDag.find('dag1');
  }
}
