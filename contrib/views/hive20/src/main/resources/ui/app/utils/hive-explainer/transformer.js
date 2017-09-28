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

import doEnhance from './enhancer';
import {getProcessedVertices, getEdgesWithCorrectedUnion, getAdjustedVerticesAndEdges} from './processor';

export default function doTransform(data) {
  const tez = getTezPlan(data);
  const fetch = getFetchPlan(data);

  let vertices = [
    ...(tez ? getTezVertices(tez) : []),
    getFetchVertex(fetch),
  ];

  let edges = [], verticesData;
  if(tez) {
    edges = getEdges(tez, vertices);
    edges = getEdgesWithCorrectedUnion(edges);
    edges = getEdgesWithFetch(edges, vertices);
  }

  vertices = doEnhance(vertices);

  vertices = getProcessedVertices(vertices, edges);
  verticesData = vertices;
  const {adjustedVertices, adjustedEdges} = getAdjustedVerticesAndEdges(vertices, edges);
  vertices = adjustedVertices;
  edges = adjustedEdges;

  vertices = getVerticesWithIndexOfChildren(vertices);

  let tree = (edges.length > 0) ? getVertexTree(edges) : Object.assign({}, vertices[0], {
    _vertices: []
  });

  const connections = getConnections(vertices, edges);
  tree = getTreeWithOffsetAndHeight(tree, vertices, connections);

  const nodes = getNodes(vertices);

  return ({
    vertices,
    edges,
    tree,
    nodes,
    connections,
    verticesData
  });
}

function getVerticesWithIndexOfChildren(vertices) {
  const verticesWithIndexX = vertices.map(cVertex => Object.assign({}, cVertex, {
    _children: doGetChildrenWithIndexX(cVertex._children, 0)
  }));

  const verticesWithIndexY = verticesWithIndexX.map(cVertex => Object.assign({}, cVertex, {
    _children: doGetChildrenWithIndexY(cVertex._children, 0)
  }));

  return verticesWithIndexY;
}

function doGetChildrenWithIndexX(children, cIndex) {
  return children.map(cChild => Object.assign({}, cChild, {
    _indexX: cIndex,
    _children: doGetChildrenWithIndexX(cChild._children, cIndex + 1)
  }));
}

function doGetChildrenWithIndexY(children, cIndex) {
  return children.map((cChild, index) => Object.assign({}, cChild, {
    _indexY: cIndex + index,
    _children: doGetChildrenWithIndexY(cChild._children, cIndex + index)
  }));
}

function getTezPlan(data) {
  const stages = data['STAGE PLANS'];
  const tezStageKey = Object.keys(stages).find(cStageKey => stages[cStageKey].hasOwnProperty('Tez'));
  return stages[tezStageKey] && stages[tezStageKey]['Tez'];
}

function getFetchPlan(data) {
  const stages = data['STAGE PLANS'];
  const fetchStageKey = Object.keys(stages).find(cStageKey => stages[cStageKey].hasOwnProperty('Fetch Operator'));
  return stages[fetchStageKey] && stages[fetchStageKey]['Fetch Operator'];
}

function getVertexTree(edges) {
  const rootKey = edges.find(cEdge => edges.every(tcEdge => cEdge._target !== tcEdge._source))._target;
  const root = buildTree(rootKey, edges);

  return getPrunedTree(root);
}

function getPrunedTree(node, used = {}) {
  const vertices = node._vertices.filter(cVertex => used[cVertex._vertex] !== true);
  vertices.forEach(cVertex => {
    used[cVertex._vertex] = true;
  });
  return Object.assign({}, node, {
    _vertices: vertices.map(cVertex => getPrunedTree(cVertex, used))
  });
}

function buildTree(vertexKey, edges) {
  const edgesWithVertexAsSource = edges.filter(cEdge => cEdge._target === vertexKey);

  return Object.assign({
    _vertex: vertexKey,
    _vertices: edgesWithVertexAsSource.map(cEdge => buildTree(cEdge._source, edges))
  });
}

function getEdgesWithFetch(tezEdges, vertices) {
  const rootKeys =
    tezEdges
      .filter(cEdge => tezEdges.every(tcEdge => cEdge._target !== tcEdge._source))
      .map(cRootEdge => cRootEdge._target);

  const uniqueRootKeys = [...new Set(rootKeys)];

  const fetchVertex = vertices.find(cVertex => cVertex._vertex === 'Fetch');

  return ([
    ...tezEdges,
    ...uniqueRootKeys.map(cRootKey => ({
      _source: cRootKey,
      _target: fetchVertex._vertex,
      parent: cRootKey,
      type: '_PSEUDO_STAGE_EDGE',
    }))
  ]);
}

function getFetchVertex(plan) {

  let children = [];
  if(plan.hasOwnProperty('Processor Tree:')) {
    const cVertex = plan['Processor Tree:'];

    let root = [{['Processor Tree:']: {}}];
    //if(cTreeKey) {
      // children available
      root = cVertex;
    //}
    children = doHarmonize(root);
  }

  const vertex = {
    _vertex: 'Fetch',
    _children: children
  };

  const lastOperator = getLastOperatorOf(vertex);
  if(lastOperator._operator === 'ListSink') {
    Object.assign(lastOperator, {
      _operator: 'Fetch Operator',
      _children: []
    });
  } else {
    // append children to last vertex
    lastOperator._children = [
      Object.assign({}, plan, {
        _operator: 'Fetch Operator',
        _children: []
      })
    ];
  }

  return vertex;
}

function getTezVertices(plan) {
  const VERTEX_TREE_KEYS = ['Reduce Operator Tree:', 'Map Operator Tree:'];
  const vertexObj = plan['Vertices:'];

  const vertices =
    Object
      .keys(vertexObj)
      .map(cVertexKey => {
        const cVertex = vertexObj[cVertexKey];

        const cTreeKey = VERTEX_TREE_KEYS.find(cVertexTreeKey => cVertex.hasOwnProperty(cVertexTreeKey));
        let root = [{[cVertexKey]: {}}];
        if(cTreeKey) {
          // children available
          root = cVertex[cTreeKey];
        }
        const children = doHarmonize(root);

        return Object.assign({}, doCloneAndOmit(cVertex, VERTEX_TREE_KEYS), {
          _vertex: cVertexKey,
          _execution: vertexObj['Execution mode:'] || 'Unavailable',
          _children: children,
        });
      });

  return vertices;
}

function doHarmonize(nodes) {
  if(Array.isArray(nodes) === false) {
    return doHarmonize([ nodes ]);
  }

  return nodes.map(cNode => {
    const cNodeOperatorKey = Object.keys(cNode)[0];
    const cNodeItem = Object.assign({}, cNode[cNodeOperatorKey], {
      _operator: cNodeOperatorKey
    });

    if(!cNodeItem.children) {
      return Object.assign({}, cNodeItem, {
        _children: []
      });
    }

    if(Array.isArray(cNodeItem.children)) {
      return Object.assign({}, doCloneAndOmit(cNodeItem, ['children']), {
        _children: doHarmonize(cNodeItem.children)
      });
    }

    return Object.assign({}, doCloneAndOmit(cNodeItem, ['children']), {
      _children: doHarmonize([ cNodeItem.children ])
    });
  });
}

function getTreeWithOffsetAndHeight(node, vertices, connections) {
  const treeWithCumulativeHeight = getTreeWithCumulativeHeight(node, vertices, connections);
  const treeWithIndividualWidth = getTreeWithIndividualWidth(treeWithCumulativeHeight, vertices);
  //const treeWithCumulativeWidth = getTreeWithCumulativeWidth(treeWithIndividualWidth);
  const treeWithOffsetY = Object.assign({}, getTreeWithOffsetYInHiererchy(treeWithIndividualWidth, connections), {
    _offsetY: 0
  });
  const treeWithEffectiveOffsetX =  getTreeWithEffectiveOffsetX(treeWithOffsetY, 0);
  const treeWithEffectiveOffsetY =  getTreeWithEffectiveOffsetY(treeWithEffectiveOffsetX, 0);

  return treeWithEffectiveOffsetY;
}

function doGetWidthOfNodes(children = []) {
  if(children.length === 0) {
    return 0;
  }
  return 1 + Math.max(0, ...children.map(cChild => doGetWidthOfNodes(cChild._children)));
}

function getTreeWithEffectiveOffsetX(node, positionX) {
  const _vertices = node._vertices.map(cVertex => getTreeWithEffectiveOffsetX(cVertex, positionX + node._widthOfSelf));

  return Object.assign({}, node, {
    _X: positionX,
    _vertices,
  });
}

function getTreeWithEffectiveOffsetY(node, positionY) {
  const _vertices = node._vertices.map(cVertex => getTreeWithEffectiveOffsetY(cVertex, positionY + cVertex._offsetY));

  return Object.assign({}, node, {
    _Y: positionY,
    _vertices,
  });
}

function doGetHeightOfNodes(children) {
  if(children.length > 0) {
    return children.reduce((height, cChild) => height + doGetHeightOfNodes(cChild._children), 0);
  }
  return 1;
}

function getTreeWithCumulativeHeight(node, vertices, connections) {
  const vertexKey = node._vertex;
  const vertex = vertices.find(cVertex => cVertex._vertex === vertexKey);

  // if does not overlap > add 1
  const vertexNext = node._vertices[0] && vertices.find(cVertex => cVertex._vertex === node._vertices[0]._vertex);
  const source = vertexNext && getLastOperatorOf(vertexNext);
  const target = getFirstOperatorOf(vertex);
  const isFirstConnectedToLast = connections.some(cConnection => source && target && cConnection._source._uuid === source._uuid && cConnection._target._uuid === target._uuid);


  let _height = doGetHeightOfNodes(vertex._children);
  let _vertices = [];
  if(Array.isArray(node._vertices)){
    _vertices = node._vertices.map(cVertex => getTreeWithCumulativeHeight(cVertex, vertices, connections));
    _height = Math.max(_height, _vertices.reduce((height, cVertex) => height + cVertex._height, 0));
  }
  if(source && !isFirstConnectedToLast) {
    _height = _height + 1;
  }
  return Object.assign({}, node, vertex, {
    _height,
    _vertices
  });
}

function getTreeWithIndividualWidth(node, vertices) {
  const vertexKey = node._vertex;
  const vertex = vertices.find(cVertex => cVertex._vertex === vertexKey);

  const _widthOfSelf = doGetWidthOfNodes(vertex._children);

  let _vertices = [];
  if(Array.isArray(node._vertices) && node._vertices.length > 0){
    _vertices = node._vertices.map(cVertex => getTreeWithIndividualWidth(cVertex, vertices));
  }
  return Object.assign({}, node, vertex, {
    _widthOfSelf,
    _vertices
  });
}

function getTreeWithOffsetYInHiererchy(node, connections) {
  const _vertices = [];
  const source = node._vertices[0] && getLastOperatorOf(node._vertices[0]);
  const target = getFirstOperatorOf(node);
  const isFirstConnectedToLast = connections.some(cConnection => source && target && cConnection._source._uuid === source._uuid && cConnection._target._uuid === target._uuid);
  let offsetY = 0;
  if(!isFirstConnectedToLast) {
    // if parent has a connection but not this && offset y are same, add offset
    offsetY = 1;
  }
  for(let index = 0; index < node._vertices.length; index++) {
    const cNode = node._vertices[index];
    const height = cNode._height;

    _vertices.push(Object.assign({}, getTreeWithOffsetYInHiererchy(cNode, connections), {
      _offsetY: offsetY
    }));
    offsetY = offsetY + height;
  }

  return Object.assign({}, node, {
    _vertices
  });
}

function getEdges(plan, vertices) {
  const edgeObj = plan['Edges:'];

  const edges =
    Object
      .keys(edgeObj)
      .reduce((accumulator, cEdgeKey) => {
        const cEdge = edgeObj[cEdgeKey];

        if(Array.isArray(cEdge)) {
          return ([
            ...accumulator,
            ...cEdge.map(tcEdge => Object.assign({}, tcEdge, {
              _source: tcEdge.parent,
              _target: cEdgeKey,
            }))
          ]);
        } else {
          return ([
            ...accumulator,
            Object.assign({}, cEdge, {
              _source: cEdge.parent,
              _target: cEdgeKey,
            })
          ]);
        }
      }, []);

  return edges;
}

function doCloneAndOmit(obj, keys) {
  return Object
    .keys(obj)
    .filter(cObjKey => keys.indexOf(cObjKey) === -1)
    .reduce((tObj, cObjKey) => Object.assign({}, tObj, {
      [cObjKey]: obj[cObjKey]
    }), {});
}

function getConnections(vertices, edges) {
  const connections = [];

  // iterate inside vertices to build connections between children
  vertices.forEach(cVertex => {
    cVertex._children.forEach(cChild => {
      connections.push(...getIntraNodeConnections(cChild));
    });
  });

  // iterate over vertices to find dynamic partitioning event operator
  // - build connection from dpp to tablescan of target vertex
  vertices.forEach(cVertex => {
    // recurse over children to find dpp > source
    const sourceOperators = findOperatorsInNode(cVertex, 'Dynamic Partitioning Event Operator', []);
    // find first operator of target vertex > target
    sourceOperators.forEach(cOperator => {
      const targetVertexKey = cOperator['Target Vertex:'];
      const targetVertex = vertices.find(cVertex => cVertex._vertex === targetVertexKey);

      const targetOperator = getFirstOperatorOf(targetVertex);

      // push connection
      connections.push({
        _source: cOperator,
        _target: targetOperator,
      });
    });
  });

  // iterate over edges to build connections
  edges.forEach(cEdge => {
    // get source uuid from source vertex
    const sourceVertex = vertices.find(cVertex => cVertex._vertex === cEdge._source);
    // get target uuid from target vertex
    const targetVertex = vertices.find(cVertex => cVertex._vertex === cEdge._target);

    const sourceOperator = getLastOperatorOf(sourceVertex);
    const targetOperator = findVertexAsInputInNode(targetVertex, cEdge._source) || getFirstOperatorOf(targetVertex);

    let sourceOperatorList = [], targetOperatorList = [], srcNode = {}, targNode = {}, sourceVertexDef = undefined, targetVertexDef = undefined;
    findAllOperatorsInSourceVertex(sourceVertex, sourceOperatorList, srcNode);
    findAllOperatorsInTargetVertex(targetVertex, targetOperatorList, targNode);

    if(sourceOperatorList.length && targetOperatorList.length) {
      sourceOperatorList.forEach(function(item){
        if(targetOperatorList.indexOf(item)>-1) {
          sourceVertexDef = srcNode[item];
          targetVertexDef = targNode[item];
          console.log(sourceVertexDef, targetVertexDef)
        }
      });
    }

    connections.push({
      _source: sourceVertexDef?sourceVertexDef:sourceOperator,
      _target: targetVertexDef?targetVertexDef:targetOperator,
    });
  });



  return connections;
}

function findVertexAsInputInNode(node, vertexId) {
  let isInputPresent = false;

  const inputs = node['input vertices:'];
  if(inputs) {
    isInputPresent = Object.keys(inputs).some(cInputKey => inputs[cInputKey] === vertexId);
  }
  if(Array.isArray(node._groups)) {
    isInputPresent = isInputPresent || node._groups.some(cGroupedOperator => {
      const inputs = cGroupedOperator['input vertices:'];
      if(inputs) {
        return Object.keys(inputs).some(cInputKey => inputs[cInputKey] === vertexId);
      }
      return false;
    });
  }

  if(isInputPresent) {
    return node;
  } else {
    for(let i = 0; i < node._children.length; i++) {
      const cChild = node._children[i];
      const operator = findVertexAsInputInNode(cChild, vertexId);

      if(operator) {
        return operator;
      }
    }
  }

  return false;
}

function findAllOperatorsInTargetVertex(node, resultsAggregator, targetNode) {
  let outputOperator = node["OperatorId:"];
  if(outputOperator) {
    resultsAggregator.push(outputOperator);
    targetNode[outputOperator] = node;
  }
  node._children.forEach(cChild => findAllOperatorsInTargetVertex(cChild, resultsAggregator, targetNode));
  if(!node._children) {
    return resultsAggregator;
  }
}

function findAllOperatorsInSourceVertex(node, resultsAggregator, srcNode) {
  let outputOperator = node["outputOperator:"];
  if(outputOperator) {
    resultsAggregator.push(outputOperator[0]);
    srcNode[outputOperator[0]] = node;
  }
  node._children.forEach(cChild => findAllOperatorsInSourceVertex(cChild, resultsAggregator, srcNode));
  if(!node._children) {
    return resultsAggregator;
  }
}

function getLastOperatorOf(vertex) {
  let operator = vertex._children[0];
  while(operator._children.length > 0) {
    operator = operator._children[0];
  }
  return operator;
}

function getFirstOperatorOf(vertex) {
  return vertex._children[0];
}

function findOperatorsInNode(node, operatorKey, resultsAggregator) {
  if(node._operator === operatorKey) {
    return resultsAggregator.push(node);
  }

  node._children.forEach(cChild => findOperatorsInNode(cChild, operatorKey, resultsAggregator));

  return resultsAggregator;
}

function getIntraNodeConnections(node) {
  return node._children.reduce((aggregator, cChild) => {
    aggregator.push({
      _source: node,
      _target: cChild,
    });
    aggregator.push(
      ...getIntraNodeConnections(cChild)
    );
    return aggregator;
  }, []);
}

function getNodes(vertices) {
  return vertices.reduce((accumulator, cVertex) => ([
    ...accumulator,
    ...getNodesFromChildren(cVertex._children)
  ]), []);
}

function getNodesFromChildren(children) {
  return children.reduce((accumulator, cChild) => ([
    ...accumulator,
    cChild,
    ...getNodesFromChildren(cChild._children)
  ]), []);
}
