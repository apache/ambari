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


import Ember from 'ember';

export default function doRender(data, selector, onRequestDetail, draggable) {

  const width = '100%', height = '800';
  var zoomInit = null;
  d3.select(selector).select('*').remove();
  var isSingleReducer = isSingleReducerAvailable(data);

  const svg =
    d3.select(selector)
      .append('svg')
        .attr('width', width)
        .attr('height', height);

  const container = svg.append('g');

  d3.selectAll('button').on('click', function() {
    if (this.id === 'zoom_in') {
      transition(1.2); // increase on 0.2 each time
    }
    if (this.id === 'zoom_out') {
      transition(0.8); // deacrease on 0.2 each time
    }
  });

  function transition(zoomLevel) {

    zoomInit = zoomInit || 1;
    let newScale = parseFloat(zoomInit*zoomLevel).toFixed(5) ;

    if(newScale < 0.2){
      newScale = 0.2;
    }else {
      newScale = newScale;
    }

    zoomInit = newScale;

    container.transition()
      .duration(100)
      .attr("transform", "translate(" + zoom.translate()[0] + "," + zoom.translate()[1] +")scale(" + newScale + ")");
  }

  const zoom = d3.behavior.zoom()
      .scale(zoomInit)
      .on('zoom', zoomed );

  function zoomed() {
    var presentScale = d3.transform(container[0][0].getAttribute('transform')).scale[0] || d3.event.scale ;
    container.attr('transform', 'translate(' + d3.event.translate + ') scale(' + presentScale + ')');
    draggable.set('zoom' , true);
  };

  var currentTransform = null;

  const drag = d3.behavior.drag()
    .on("dragstart", (event) => {
      draggable.set('dragstart', true);
      draggable.set('zoom',false);

      let evt = window.event || event;
      currentTransform = d3.transform(evt.currentTarget.firstElementChild.getAttribute('transform'));
    })
    .on("dragend", () => {
      draggable.set('dragend', true);

      var latestTransformation = d3.transform(container[0][0].getAttribute('transform'));
      container.transition()
        .duration(100)
        .attr("transform", "translate(" + latestTransformation.translate[0] + "," + latestTransformation.translate[1] +")scale(" + currentTransform.scale[0] + ")");
    });

    svg
      .call(zoom)
      .call(drag);

    svg
      .on("mousewheel.zoom", null)
      .on("DOMMouseScroll.zoom", null) // disables older versions of Firefox
      .on("wheel.zoom", null) // disables newer versions of Firefox

  const root =
    container
      .selectAll('g.vertex')
        .data([data.tree])
      .enter()
        .append('g')
      .attr('class', 'vertex')
      .attr('data-vertex', d => d._vertex);

  root
    .call(recurseC, onRequestDetail, isSingleReducer);

  root
    .call(recurseV, onRequestDetail, isSingleReducer);

  container.selectAll('path.edge')
    .data(data.connections)
    .enter()
      .insert('path', ':first-child')
    .attr('class', 'edge')
    .attr('d', d => (navigator.userAgent.toLowerCase().indexOf('firefox') > -1) ? getConnectionPathFF(d, svg, container, data) : getConnectionPath(d, svg, container, data));
  if(navigator.userAgent.toLowerCase().indexOf('firefox') === -1) {
    root.selectAll('path.arrow')
      .data(data.connections)
      .enter()
      .insert('path')
      .attr('class', 'arrow')
      .style("stroke-width", function(d) { return Math.sqrt(d.value); })
      .attr('d', d => getConnectionPathArrow(d, svg, container, data));
  }

  reset(zoom, svg, container);

}
function isSingleReducerAvailable(data){
  let reducerCount = data.verticesData.filter(function(item){
    return item['_vertex'].indexOf("Reducer") === 0;
  });
  if(reducerCount && reducerCount.length === 1) {
    return true;
  }
  return false;
}
function recurseV(vertices, onRequestDetail, isSingleReducer) {
  vertices.each(function(cVertx) {
    const vertex = d3.select(this);

    const vertices =
      vertex
        .selectAll('g.vertex')
          .data(d => d._vertices)
        .enter()
          .append('g')
        .attr('class', 'vertex')
        .attr('data-vertex', d => d._vertex)
        .style('transform', d => `translate(${d._widthOfSelf * 200}px, ${d._offsetY * 100}px)`);

      vertices
        .call(recurseC, onRequestDetail, isSingleReducer);

      vertices
        .call(recurseV, onRequestDetail, isSingleReducer);
  });
}

function recurseC(children, onRequestDetail, isSingleReducer) {
  children.each(function(d) {
    const child = d3.select(this);

    const children =
      child
          .selectAll('g.child')
        .data(d => d._children || []).enter()
          .append('g')
          .attr('class', 'child')
          .style('transform', (d, index) => `translate(-${200}px, ${index * 100}px)`);

      children
          .append('rect')
        .attr('id', d => d._uuid)
        .attr('data-operator', d => d._operator)
        .attr('class', d => `operator__box operator__box--${d._operator.toString().replace(/[ ]/g, '_')}`)
        .attr('height', d => d._operator === 'Fetch Operator' ? 150 : 55)
        .attr('width', 140);

      children
          .append('foreignObject')
        .attr('data-uuid', d => d._uuid)
        .attr('data-operator', d => d._operator)
        .attr('class', d => `operator operator--${d._operator.toString().replace(/[ ]/g, '_')}`)
        .attr('height', d => d._operator === 'Fetch Operator' ? 150 : 55)
        .attr('width', 140)
          .append('xhtml:body')
        .style('height', d => d._operator === 'Fetch Operator' ? '150px' : 'auto')
        .style('margin', 0 )
          .html(d => getRenderer(d._operator, isSingleReducer)(d))
        .on('click', d => {
          const vertex = d3.select(Ember.$(d3.select(this).node()).closest('.vertex').get(0)).data()[0];
          onRequestDetail(doClean(d), vertex);
        });

      children
        .call(recurseC, onRequestDetail, isSingleReducer);
    });
}

function getRenderer(type, isSingleReducer) {
  if(type === 'Fetch Operator') {
    return (d => {
      return (`
        <div style='display:flex;align-items: center;'>
          <div class='operator-meta'>
            <i class='fa ${getOperatorIcon(d._operator)}' aria-hidden='true'></i>
          </div>
          <div class='operator-body' style='margin-left: 10px;'>
            <div class="ellipsis-node" title=${getOperatorLabel(d, isSingleReducer)}>${getOperatorLabel(d, isSingleReducer)}</div>
            ${(d['limit:'] && d['limit:'] > -1) ? '<div><span style="font-weight: lighter;">Limit:</span> ' + d['limit:'] + ' </div>' : ''}
          </div>
        </div>
      `);
    });
  }

  return (d => {
    const stats = d['Statistics:'] ?  `<div><span style='font-weight: lighter;'>Rows:</span> ${abbreviate(getNumberOfRows(d['Statistics:']))}</div>` : '';
    return (`
      <div style='display:flex;'>
        <div class='operator-meta'>
          <i class='fa ${getOperatorIcon(d._operator)}' aria-hidden='true'></i>
        </div>
        <div class='operator-body' style='margin-left: 10px;'>
          <div class="ellipsis-node" title=${getOperatorLabel(d, isSingleReducer)}>${getOperatorLabel(d, isSingleReducer)}</div>
          ${stats}
        </div>
      </div>
    `);
  });

}

function getNumberOfRows(statistics) {
  const match = statistics.match(/([^\?]*)\Num rows: (\d*)/);
  return (match.length === 3 && Number.isNaN(Number(match[2])) === false) ? match[2] : 0;
}
function getOperatorLabel(d, isSingleReducer) {
  const operator = d._operator;
  if(operator === 'Partition/Sort Pseudo-Edge' && isSingleReducer) {
    return "Sort";
  }
  if(operator === 'TableScan') {
    return d['alias:'];
  }

  const operatorStr = operator.toString();
  if(operatorStr.endsWith(' Operator')) {
    return operatorStr.substring(0, operatorStr.length - ' Operator'.length);
  }
  if(operatorStr.endsWith(' Pseudo-Edge')) {
    return operatorStr.substring(0, operatorStr.length - ' Pseudo-Edge'.length);
  }
  return operatorStr ? operatorStr : 'Unknown';
}
function getOperatorIcon(operator) {
  switch(operator) {
    case 'File Output Operator':
      return 'fa-file-o';
    case 'Partition/Sort Pseudo-Edge':
    case 'Broadcast Pseudo-Edge':
    case 'Partition Pseudo-Edge':
    case 'Co-partition Pseudo-Edge':
    case 'Cross-product Distribute Pseudo-Edge':
    case 'Reduce Output Operator':
      return 'fa-compress';
    case 'Filter Operator':
      return 'fa-filter';
    case 'Dynamic Partitioning Event Operator':
      return 'fa-columns';
    case 'Map Join Operator':
      return 'fa-code-fork';
    case 'Limit':
    case 'Group By Operator':
    case 'Select Operator':
    case 'TableScan':
    case 'Fetch Operator':
      return 'fa-table';
    default:
      return '';
  }
}
function getIcon (type, subtype) {
  switch(type) {
    case 'join':
      return 'fa-code-fork';
    case 'vectorization':
    case 'job':
      return;
    case 'broadcast':
    case 'partition-sort':
      return 'fa-compress';
    case 'source':
    case 'sink':
    case 'group-by':
    case 'select':
      return 'fa-table';
  }
}

function abbreviate(value) {
  let newValue = value;
  if (value >= 1000) {
    const suffixes = ["", "k", "m", "b","t"];
    const suffixNum = Math.floor(("" + value).length / 3);
    let shortValue = '';
    for (var precision = 2; precision >= 1; precision--) {
      shortValue = parseFloat( (suffixNum !== 0 ? (value / Math.pow(1000,suffixNum) ) : value).toPrecision(precision));
      const dotLessShortValue = (shortValue + '').replace(/[^a-zA-Z 0-9]+/g,'');
      if (dotLessShortValue.length <= 2) { break; }
    }
    if (shortValue % 1 !== 0) {
      const  shortNum = shortValue.toFixed(1);
    }
    newValue = shortValue+suffixes[suffixNum];
  }
  return newValue;
}
function reset(zoom, svg, container) {
  const vertices = container.selectAll('g.vertex');
  const bounds = [];
  vertices.each(function(d) {
    const cVertex = d3.select(this);
    const box = cVertex.node().getBoundingClientRect();
    bounds.push(box);
  });
  const PADDING_PERCENT = 0.95;
  const svgRect = svg.node().getBoundingClientRect();
  const fullWidth = svgRect.width;
  const fullHeight = svgRect.height;
  const offsetY = svgRect.top;
  const top = Math.min(...bounds.map(cBound => cBound.top));
  const left = Math.min(...bounds.map(cBound => cBound.left));
  const width = Math.max(...bounds.map(cBound => cBound.right)) - left;
  const height = Math.max(...bounds.map(cBound => cBound.bottom)) - top;
  const midX = left + width / 2;
  const midY = top + height / 2;
  if (width === 0 || height === 0){
    // nothing to fit
    return;
  }
  const scale = PADDING_PERCENT / Math.max(width / fullWidth, height / fullHeight);
  const translate = [fullWidth / 2 - scale * midX, fullHeight / 2 - scale * midY];

  zoom.scale(scale).translate([translate[0], 50]);

  svg
    .transition()
    .delay(750)
    .call( zoom.event );
}

function getConnectionPathFF(connector, svg, container, data) {
  const source = container.select(`#${connector._source._uuid}`).node();
  const target = container.select(`#${connector._target._uuid}`).node();
  const rSource = d3.select(source).data()[0];
  const rTarget = d3.select(target).data()[0];
  const rSourceVertex = d3.select(Ember.$(source).closest('.vertex').get(0)).data()[0];
  const rTargetVertex = d3.select(Ember.$(target).closest('.vertex').get(0)).data()[0];

  const offsetBox = Ember.$(container.node()).children('.vertex').get(0).getBoundingClientRect();
  let connectionComplexity, connection = data.nodes.find((item)=>item["filterExpr:"]);

  if(connection){
    connectionComplexity = 0;
  } else {
    connectionComplexity = data.connections.length;
  }
  const pSource = {
    x: offsetBox.left - 200 - 200*connectionComplexity + (rSourceVertex._X + (rSourceVertex._widthOfSelf - (rSource._indexX + 1))) * 200 + 140 / 2,
    y: offsetBox.top + (rSourceVertex._Y + rSource._indexY) * 100 + 55 / 2,
  };
  const pTarget = {
    x: offsetBox.left - 200 - 200*connectionComplexity + (rTargetVertex._X + (rTargetVertex._widthOfSelf - (rTarget._indexX + 1))) * 200 + 140 / 2,
    y: offsetBox.top + (rTargetVertex._Y + rTarget._indexY) * 100 + 55 / 2,
  };
  const path = [
    pTarget
  ];
  const junctionXMultiplier = (pTarget.x - pSource.x < 0) ? +1 : -1;
  if(pSource.y !== pTarget.y) {
    path.push({
      x: pTarget.x + junctionXMultiplier * 90,
      y: pTarget.y
    }, {
      x: pTarget.x + junctionXMultiplier * 90,
      y: pSource.y
    });
  }
  path.push(pSource);
  const offsetY = svg.node().getBoundingClientRect().top;
  return path.reduce((accumulator, cPoint, index) => {
    if(index === 0) {
      return accumulator + `M ${cPoint.x}, ${cPoint.y - offsetY}\n`;
    } else {
      return accumulator + `L ${cPoint.x}, ${cPoint.y - offsetY}\n`;
    }
  }, '');
}

function getConnectionPathFFArrow(connector, svg, container, data) {
  const source = container.select(`#${connector._source._uuid}`).node();
  const target = container.select(`#${connector._target._uuid}`).node();
  const rSource = d3.select(source).data()[0];
  const rTarget = d3.select(target).data()[0];
  const rSourceVertex = d3.select(Ember.$(source).closest('.vertex').get(0)).data()[0];
  const rTargetVertex = d3.select(Ember.$(target).closest('.vertex').get(0)).data()[0];

  const offsetBox = Ember.$(container.node()).children('.vertex').get(0).getBoundingClientRect();

  let connectionComplexity, connection = data.nodes.find((item)=>item["filterExpr:"]);
  if(connection){
    connectionComplexity = 0;
  } else {
    connectionComplexity = data.connections.length;
  }
  const pSource = {
    x: offsetBox.left - 200 - 200*connectionComplexity + (rSourceVertex._X + (rSourceVertex._widthOfSelf - (rSource._indexX + 1))) * 200 + 140 / 2,
    y: offsetBox.top + (rSourceVertex._Y + rSource._indexY) * 100 + 55 / 2,
  };
  const pTarget = {
    x: offsetBox.left - 200 - 200*connectionComplexity + (rTargetVertex._X + (rTargetVertex._widthOfSelf - (rTarget._indexX + 1))) * 200 + 140 / 2,
    y: offsetBox.top + (rTargetVertex._Y + rTarget._indexY) * 100 + 55 / 2,
  };
  const path = [
    pTarget
  ];
  const junctionXMultiplier = (pTarget.x - pSource.x < 0) ? +1 : -1;
  if(pSource.y !== pTarget.y) {
    path.push({
      x: pTarget.x + junctionXMultiplier * 90,
      y: pTarget.y
    }, {
      x: pTarget.x + junctionXMultiplier * 90,
      y: pSource.y
    });
  }
  path.push(pSource);
  const offsetY = svg.node().getBoundingClientRect().top;
  return path.reduce((accumulator, cPoint, index) => {
    if(path.length === 2){
      if(index === 0) {
        if(cPoint.x > 0){
          return accumulator + `M ${cPoint.x + 60}, ${cPoint.y - offsetY+10} V 10, ${(cPoint.x+200)%(cPoint.x) === 0 ? 40:100}  L ${cPoint.x + 50}, ${cPoint.y - offsetY} Z\n`;
        } else {
          return accumulator + `M ${cPoint.x + 60}, ${cPoint.y - offsetY-10} V 10, ${Math.ceil((cPoint.x))%Math.ceil((cPoint.x+200)) === -0 ? 40:100}  L ${cPoint.x + 50}, ${cPoint.y - offsetY} Z\n`;
        }
      } else {
        return accumulator;
      }
    } else {
      if(index === 0) {
        if(cPoint.x > 0) {
          return accumulator + `M ${cPoint.x + 60}, ${cPoint.y - offsetY-10} V 10, ${40} L ${cPoint.x + 50}, ${cPoint.y - offsetY} Z\n`;
        } else {
          return accumulator + `M ${cPoint.x + 60}, ${cPoint.y - offsetY+10} V 10, ${40} L ${cPoint.x + 50}, ${cPoint.y - offsetY} Z\n`;
        }
      } else {
        return accumulator;
      }
    }

  }, '');
}

function getConnectionPath(connector, svg, container, data){
  const operators = container.selectAll('.operator');
  const source = container.select(`#${connector._source._uuid}`);
  const target = container.select(`#${connector._target._uuid}`);
  const rSource = source.node().getBoundingClientRect();
  const rTarget = target.node().getBoundingClientRect();
  const pSource = {
    x: (rSource.left + rSource.right) / 2,
    y: (rSource.top + rSource.bottom) / 2,
  };
  const pTarget = {
    x: (rTarget.left + rTarget.right) / 2,
    y: (rTarget.top + rTarget.bottom) / 2,
  };
  const path = [
    pTarget
  ];
  const junctionXMultiplier = (pTarget.x - pSource.x < 0) ? +1 : -1;
  if(pSource.y !== pTarget.y) {
    path.push({
      x: pTarget.x + junctionXMultiplier * 90,
      y: pTarget.y
    }, {
      x: pTarget.x + junctionXMultiplier * 90,
      y: pSource.y
    });
  }
  path.push(pSource);
  const offsetY = svg.node().getBoundingClientRect().top;
  let isEdgeReversed = false, edgeReversalVal;
  return path.reduce((accumulator, cPoint, index) => {
    if(index === 0) {
      if(cPoint.x > (cPoint.y - offsetY)) {
        edgeReversalVal = cPoint.x;
        isEdgeReversed = true;
      }
      return accumulator + `M ${cPoint.x}, ${cPoint.y - offsetY}\n`;
    } else {
      if(isEdgeReversed && path.length === 4 && index !== path.length-1 && edgeReversalVal > cPoint.x){
        return accumulator + `L ${cPoint.x+150}, ${cPoint.y - offsetY}\n`;
      }
      return accumulator + `L ${cPoint.x}, ${cPoint.y - offsetY}\n`;
    }
  }, '');
}

function getConnectionPathArrow(connector, svg, container){

  const operators = container.selectAll('.operator');
  const source = container.select(`#${connector._source._uuid}`);
  const target = container.select(`#${connector._target._uuid}`);
  const rSource = source.node().getBoundingClientRect();
  const rTarget = target.node().getBoundingClientRect();
  const pSource = {
    x: (rSource.left + rSource.right) / 2,
    y: (rSource.top + rSource.bottom) / 2,
  };
  const pTarget = {
    x: (rTarget.left + rTarget.right) / 2,
    y: (rTarget.top + rTarget.bottom) / 2,
  };
  const path = [
    pTarget
  ];
  const junctionXMultiplier = (pTarget.x - pSource.x < 0) ? +1 : -1;
  if(pSource.y !== pTarget.y) {
    path.push({
      x: pTarget.x + junctionXMultiplier * 90,
      y: pTarget.y
    }, {
      x: pTarget.x + junctionXMultiplier * 90,
      y: pSource.y
    });
  }
  path.push(pSource);
  const offsetY = svg.node().getBoundingClientRect().top;
  return path.reduce((accumulator, cPoint, index) => {
    if(path.length === 2){
      if(index === 0) {
        if(cPoint.x > 0){
          return accumulator + `M ${cPoint.x + 45}, ${cPoint.y - offsetY-7} V 0, ${(((cPoint.y - offsetY-15)/100)*100)+23}  L ${cPoint.x + 40}, ${cPoint.y - offsetY} Z\n`;
        } else {
          return accumulator + `M ${cPoint.x + 45}, ${cPoint.y - offsetY-7} V 0, ${(((cPoint.y - offsetY-15)/100)*100)+23}  L ${cPoint.x + 40}, ${cPoint.y - offsetY} Z\n`;
        }
      } else {
        return accumulator;
      }
    } else {
      if(index === 0) {
        if(cPoint.x > 0) {
          return accumulator + `M ${cPoint.x + 45}, ${cPoint.y - offsetY-7} V 0, ${(((cPoint.y - offsetY-15)/100)*100)+23} L ${cPoint.x + 40}, ${cPoint.y - offsetY} Z\n`;
        } else {
          return accumulator + `M ${cPoint.x + 45}, ${cPoint.y - offsetY+9} V 0, 67 L ${cPoint.x + 40}, ${cPoint.y - offsetY} Z\n`;
        }
      } else {
        return accumulator;
      }
    }
  }, '');

}

function doClean(node) {
  if(Array.isArray(node._groups)) {
    return node._groups.map(cGroup => doClean(cGroup));
  } else {
    return (
      Object.keys(node)
        .filter(cNodeKey => cNodeKey === '_operator' || !cNodeKey.startsWith('_'))
        .reduce((accumulator, cNodeKey) => {
          accumulator[cNodeKey] = node[cNodeKey];
          return accumulator;
        }, {})
    );
  }
}
