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

export function getProcessedVertices(vertices, edges) {
  const edgedVertices = processEdges(vertices, edges);
  return processSource(edgedVertices);
}

function processEdges(vertices, edges) {

  return vertices
    .map(cVertex => {
      const isVertexPartOfSimpleEdge = edges.some(cEdge => cEdge.type === 'SIMPLE_EDGE' && cEdge._source === cVertex._vertex);
      const isVertexPartOfBroadcastEdge = edges.some(cEdge => cEdge.type === 'BROADCAST_EDGE' && cEdge._source === cVertex._vertex);
      const isVertexPartOfCustomSimpleEdge = edges.some(cEdge => cEdge.type === 'CUSTOM_SIMPLE_EDGE' && cEdge._source === cVertex._vertex);
      const isVertexPartOfCustomEdge = edges.some(cEdge => cEdge.type === 'CUSTOM_EDGE' && cEdge._source === cVertex._vertex);
      const isVertexPartOfXProdEdge = edges.some(cEdge => cEdge.type === 'XPROD_EDGE' && cEdge._source === cVertex._vertex);
      const isVertexPartOfUnionEdge = edges.some(cEdge => cEdge.type === 'CONTAINS' && cEdge._source === cVertex._vertex);

      let tVertex = cVertex;

      if(isVertexPartOfSimpleEdge) {
        tVertex = appendIfTerminusOfOperator(tVertex, {
          _operator: 'Partition/Sort Pseudo-Edge'
        });
      }
      if(isVertexPartOfBroadcastEdge) {
        tVertex = appendIfTerminusOfOperator(tVertex, {
          _operator: 'Broadcast Pseudo-Edge'
        });
      }
      if(isVertexPartOfCustomSimpleEdge) {
        tVertex = appendIfTerminusOfOperator(tVertex, {
          _operator: 'Partition Pseudo-Edge'
        });
      }
      if(isVertexPartOfCustomEdge) {
        tVertex = appendIfTerminusOfOperator(tVertex, {
          _operator: 'Co-partition Pseudo-Edge'
        });
      }
      if(isVertexPartOfXProdEdge) {
        tVertex = appendIfTerminusOfOperator(tVertex, {
          _operator: 'Cross-product Distribute Pseudo-Edge'
        });
      }
      if(isVertexPartOfUnionEdge) {
        tVertex = appendIfTerminusOfOperator(tVertex, {
          _operator: 'Partition/Sort Pseudo-Edge'
        });
      }

      return tVertex;
    });
}

function appendIfTerminusOfOperator(node, pseudoNode) {
  if(Array.isArray(node._children) === false || node._children.length === 0) {
    // is terminus
    switch(node._operator) {
      case 'Reduce Output Operator':
        return Object.assign({}, node, pseudoNode);
      default:
        return node;
    }
  }

  return Object.assign({}, node, {
    _children: node._children.map(cChild => appendIfTerminusOfOperator(cChild, pseudoNode))
  });
}

function processSource(vertices) {
  return vertices.map(cVertex => Object.assign({}, cVertex, {
    _children: cVertex._children.map(cChild => getProcessedSequenceViaStack(cChild))
  }));
}

// DANGER: impure function
function getProcessedSequenceViaStack(root) {
  const stack = [];

  let cNode = root;
  stack.push(cNode);
  doCompaction(stack);
  while(cNode._children.length === 1) {
    cNode = cNode._children[0];

    stack.push(cNode);
    doCompaction(stack);
  }

  const lNode = stack[stack.length - 1];
  if(lNode._children.length > 1) {
    // begin processing new subtree
    lNode._children = lNode._children.map(cChild => getProcessedSequenceViaStack(cChild));
  }

  return stack[0];
}

function doCompaction(stack) {
  let index = stack.length;

  while(index > 0) {
    const cNode = stack[index - 0 - 1];
    const cNodeMinus1 = stack[index - 1 - 1];
    const cNodeMinus2 = stack[index - 2 - 1];
    const cNodeMinus3 = stack[index - 3 - 1];
    const cNodeMinus4 = stack[index - 4 - 1];

    if(cNodeMinus1) {

      if(cNode._operator === 'Select Operator' || cNode._operator === 'HASHTABLEDUMMY' || cNode._operator === 'File Output Operator') {
        // remove cNode from stack
        stack.pop();
        index--;
        // recreate groups
        cNodeMinus1._groups = [
          ...(cNodeMinus1._groups || [doCloneAndOmit(cNodeMinus1, ['_groups'])]),
          ...(cNode._groups || [doCloneAndOmit(cNode, ['_groups'])]),
        ];
        // move children
        cNodeMinus1._children = cNode._children;

        continue;
      }
      if(cNodeMinus1._operator === 'Select Operator' || cNodeMinus1._operator === 'HASHTABLEDUMMY' || cNodeMinus1._operator === 'File Output Operator') {
        // remove cNode and cNodeMinus1 from stack
        stack.pop();
        index--;
        stack.pop();
        index--;

        // recreate groups
        cNode._groups = [
          ...(cNodeMinus1._groups || [doCloneAndOmit(cNodeMinus1, ['_groups'])]),
          ...(cNode._groups || [doCloneAndOmit(cNode, ['_groups'])]),
        ];
        // no need to move chldren
        // reinsert cNode
        stack.push(cNode);
        index++;

        continue;
      }


      if(cNode._operator === 'Map Join Operator' && cNodeMinus1._operator === 'Map Join Operator') {
        // remove cNode from stack
        stack.pop();
        index--;
        // recreate groups
        cNodeMinus1._groups = [
          ...(cNodeMinus1._groups || [doCloneAndOmit(cNodeMinus1, ['_groups'])]),
          ...(cNode._groups || [doCloneAndOmit(cNode, ['_groups'])]),
        ];
        // move chldren
        cNodeMinus1._children = cNode._children;

        continue;
      }

      if(cNode._operator === 'Filter Operator' && cNodeMinus1._operator === 'TableScan') {
        // remove cNode from stack
        stack.pop();
        index--;
        // recreate groups
        cNodeMinus1._groups = [
          ...(cNodeMinus1._groups || [doCloneAndOmit(cNodeMinus1, ['_groups'])]),
          ...(cNode._groups || [doCloneAndOmit(cNode, ['_groups'])]),
        ];
        // move children
        cNodeMinus1._children = cNode._children;

        continue;
      }

      if(cNodeMinus2 && cNodeMinus3) {
        if(cNode._operator === 'Broadcast Pseudo-Edge' && cNodeMinus1._operator === 'Group By Operator' && cNodeMinus2._operator === 'Reduce Output Operator' && cNodeMinus3._operator === 'Group By Operator') {
          // remove cNode from stack
          stack.pop();
          index--;
          // remove cNodeMinus1 from stack
          stack.pop();
          index--;
          // remove cNodeMinus2 from stack
          stack.pop();
          index--;
          // remove cNodeMinus3 from stack
          stack.pop();
          index--;

          // recreate groups
          cNodeMinus1._groups = [
            ...(cNodeMinus3._groups || [doCloneAndOmit(cNodeMinus3, ['_groups'])]),
            ...(cNodeMinus2._groups || [doCloneAndOmit(cNodeMinus2, ['_groups'])]),
            ...(cNodeMinus1._groups || [doCloneAndOmit(cNodeMinus1, ['_groups'])]),
          ];
          // move children if required, cNodeMinus1 as child of cNodeMinus4
          if(cNodeMinus4) {
            cNodeMinus4._children = cNodeMinus2._children;
          }
          // rename
          cNodeMinus1._operator = 'Build Bloom Filter';
          // add renamed node
          stack.push(cNodeMinus1);
          index++;
          // add original broadcast edge node
          stack.push(cNode);
          index++;


          continue;
        }
      }

    }
    index--;

  }
}

function doCloneAndOmit(obj, keys) {
  return Object
    .keys(obj)
    .filter(cObjKey => keys.indexOf(cObjKey) === -1)
    .reduce((tObj, cObjKey) => Object.assign({}, tObj, {
      [cObjKey]: obj[cObjKey]
    }), {});
}

export function getEdgesWithCorrectedUnion(edges) {

  return edges
      .map(cEdge => {
        if(cEdge.type === 'CONTAINS') {
          return Object.assign({}, cEdge, {
            _source: cEdge._target,
            _target: cEdge._source,
          });
        } else {
          return cEdge;
        }
      });

}
