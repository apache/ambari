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

export default function doRender(data, selector, onRequestDetail) {

  const {connections} = data;
  const dataNodes = data.nodes;
  const dataLinks = connections.map(cConnection => ({
    source: cConnection._target,
    target: cConnection._source
  }));

  const width = '960', height = '800';

  d3.select(selector).select('*').remove();
  const svg =
    d3.select(selector)
      .append('svg')
        .attr('width', width)
        .attr('height', height);

  const container = svg.append('g');
  const zoom =
    d3.behavior.zoom()
      .scaleExtent([1 / 10, 4])
      .on('zoom', () => {
        container.attr('transform', `translate(${d3.event.translate}) scale(${d3.event.scale})`);
      });

    svg
      .call(zoom);

  var force = cola.d3adaptor(d3)
        .avoidOverlaps(true)
        .flowLayout('x', 150)
        .convergenceThreshold(1e-3)
        .size([width, height])
        .jaccardLinkLengths(150);
    // .linkDistance(200)

  var links = container.selectAll('path.edge')
      .data(dataLinks)
    .enter().append('path')
      .attr('class', 'edge');

  var nodes = container.selectAll('g.operator-wrapper')
      .data(dataNodes)
    .enter().append('g')
      .attr('class', 'operator-wrapper');
    // .call(force.drag);

    nodes
        .append('rect')
      .attr('id', d => d._uuid)
      .attr('data-operator', d => d._operator)
      .attr('class', d => `operator__box operator__box--${d._operator.toString().replace(/[ ]/g, '_')}`)
      .attr('height', d => d._operator === 'Fetch Operator' ? 150 : 55)
      .attr('width', 140);

    nodes
        .append('foreignObject')
      .attr('data-uuid', d => d._uuid)
      .attr('data-operator', d => d._operator)
      .attr('class', d => `operator operator--${d._operator.toString().replace(/[ ]/g, '_')}`)
      .attr('height', d => d._operator === 'Fetch Operator' ? 150 : 55)
      .attr('width', 140)
        .append('xhtml:body')
      .style('margin', 0)
        .html(d => getRenderer(d._operator)(d))
      .on('click', d => onRequestDetail(doClean(d)));

  force
      .nodes(dataNodes)
      .links(dataLinks)
      // .constraints([
      //   {
      //     type: 'alignment',
      //     axis: 'y',
      //     offsets: dataNodes.map((cNode, index) => ({
      //       node: index,
      //       offset: cNode._offsetY
      //     }))
      //   }
      // ])
      .on('tick', e => {
        // node.each(function (d) { d.innerBounds = d.bounds.inflate(-margin); })
        //   .attr("x", function (d) { return d.innerBounds.x; })
        //   .attr("y", function (d) { return d.innerBounds.y; })
        //   .attr("width", function (d) {
        //       return d.innerBounds.width();
        //   })
        //   .attr("height", function (d) { return d.innerBounds.height(); });

        // link.attr("d", function (d) {
        //     var route = cola.makeEdgeBetween(d.source.innerBounds, d.target.innerBounds, 5);
        //     return lineFunction([route.sourceIntersection, route.arrowStart]);
        // });

        // const k = 6 * e.alpha;

        // Push sources up and targets down to form a weak tree.
        links
            // .each(function(d) { d.source.y -= k, d.target.y += k; })
            .attr('d', d => getConnectionPath({
                x: (d.source.bounds.X + d.source.bounds.x) / 2,
                y: (d.source.bounds.Y + d.source.bounds.y) / 2,
              }, {
                x: (d.target.bounds.X + d.target.bounds.x) / 2,
                y: (d.target.bounds.Y + d.target.bounds.y) / 2,
              }));

        nodes
            .attr("transform", d =>  `translate(${d.x}, ${d.y})`);

      })
      .on('end', () => {
        reset(zoom, svg, container);
      })
      .start();
}

function getRenderer(type) {
  if(type === 'Fetch Operator') {
    return (d => {
      return (`
        <div style='display:flex;align-items: center;'>
          <div class='operator-meta'>
            <i class='fa ${getOperatorIcon(d._operator)}' aria-hidden='true'></i>
          </div>
          <div class='operator-body' style='margin-left: 10px;'>
            <div>${getOperatorLabel(d)}</div>
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
          <div>${getOperatorLabel(d)}</div>
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
function getOperatorLabel(d) {
  const operator = d._operator;

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
  const vertices = container.selectAll('g.operator-wrapper');
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

  zoom.scale(scale).translate(translate);

  svg
    .transition()
    .delay(750)
    .call(zoom.event);
}

function getConnectionPath(pSource, pTarget) {
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
  const offsetY = 0;
  return path.reduce((accumulator, cPoint, index) => {
    if(index === 0) {
      return accumulator + `M ${cPoint.x}, ${cPoint.y - offsetY}\n`;
    } else {
      return accumulator + `L ${cPoint.x}, ${cPoint.y - offsetY}\n`;
    }
  }, '');
}

function doClean(node) {
  if(Array.isArray(node._groups)) {
    return node._groups.map(cGroup => doClean(cGroup));
  } else {
    return (
      Object.keys(node)
        .filter(cNodeKey => cNodeKey !== '_children')
        .reduce((accumulator, cNodeKey) => {
          accumulator[cNodeKey] = node[cNodeKey];
          return accumulator;
        }, {})
    );
  }
}
