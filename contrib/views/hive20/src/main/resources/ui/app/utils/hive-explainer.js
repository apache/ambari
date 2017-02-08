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


export default function render(data, svg, container, zoom, onRequestDetail){

  const steps = createOrder(data).steps;
  const plans = data['STAGE PLANS'];
  const stageKey =
    Object
      .keys(plans)
      .find(cStageKey => plans[cStageKey].hasOwnProperty('Fetch Operator'));
  let rows = 'Unknown';
  if(stageKey && plans[stageKey]['Fetch Operator']['limit:']) {
    rows = plans[stageKey]['Fetch Operator']['limit:'];
  }
  const root = [{
    "type": "sink",
    "sink-type": "table",
    "sink-label": "Limit",
    "rows": rows,
    "children": [{
      steps: steps
    }]
  }];
  const transformed = getTransformed(root);
  update(transformed, svg, container, zoom, onRequestDetail);
}

const RENDER_GROUP = {
  join: d => `
    <div style='display:flex;'>
      <div class='step-meta'>
        <i class='fa ${getIcon(d.type, d['join-type'])}' aria-hidden='true'></i>
      </div>
      <div class='step-body' style='margin-left: 10px;'>
        <div>${d['join-type'] === 'merge' ? 'Merge' : 'Map'} Join</div>
        <div><span style='font-weight: lighter;'>Rows:</span> ${abbreviate(d.rows)}</div>
      </div>
    </div>
  `,
  vectorization: d => '<div class="step__pill">U</div>',
  job: d => `<div class="step__pill">${d.label.toUpperCase()}</div>`,
  broadcast: d => `
    <div style='display:flex;'>
      <div class='step-meta'>
        <i class='fa ${getIcon(d.type)}' aria-hidden='true'></i>
      </div>
      <div class='step-body' style='margin-left: 10px;'>
        <div>Broadcast</div>
        <!--div><span style='font-weight: lighter;'>Rows:</span> ${abbreviate(d.rows)}</div-->
      </div>
    </div>
  `,
  'partition-sort': d => `
    <div style='display:flex;'>
      <div class='step-meta'>
        <i class='fa ${getIcon(d.type)}' aria-hidden='true'></i>
      </div>
      <div class='step-body' style='margin-left: 10px;'>
        <div>Partition / Sort</div>
        <div><span style='font-weight: lighter;'>Rows:</span> ${abbreviate(d.rows)}</div>
      </div>
    </div>
  `,
  sink: d => `
// TODO
  `,
  'group-by': d => `
    <div style='display:flex;'>
      <div class='step-meta'>
        <i class='fa ${getIcon(d.type)}' aria-hidden='true'></i>
      </div>
      <div class='step-body' style='margin-left: 10px;'>
        <div>Group By</div>
        <div><span style='font-weight: lighter;'>Rows:</span> ${abbreviate(d.rows)}</div>
      </div>
    </div>
  `,
  select: d => `
    <div style='display:flex;'>
      <div class='step-meta'>
        <i class='fa ${getIcon(d.type)}' aria-hidden='true'></i>
      </div>
      <div class='step-body' style='margin-left: 10px;'>
        <div>Select</div>
        <div><span style='font-weight: lighter;'>Rows:</span> ${abbreviate(d.rows)}</div>
      </div>
    </div>
  `,
  source: d => `
    <div style='display:flex;'>
      <div class='step-meta'>
        <i class='fa ${getIcon(d.type, d['source-type'])}' aria-hidden='true'></i>
      </div>
      <div class='step-body' style='margin-left: 10px;'>
        <div>${d.label}</div>
        <div><span style='font-weight: lighter;'>${d.isPartitioned ? 'Partitioned' : 'Unpartitioned'} | Rows:</span> ${abbreviate(d.rows)}</div>
      </div>
    </div>
  `
};

function update(data, svg, container, zoom, onRequestDetail) {
  const steps = container.selectAll('g.step')
    .data(data)
    .enter().append('g')
    .attr('class', 'step');
  steps
    .append('foreignObject')
    .attr('id', d => d.uuid)
    .attr('class', 'step step-sink')
    .attr('height', 300)
    .attr('width', 220)
    .append('xhtml:body')
    .style('margin', 0)
    .html(d => `
        <div>
          <div class='step-meta' style='display:flex;'>
            <i class='fa ${getIcon(d.type, d['sink-type'])}' aria-hidden='true'></i>
            <div class='step-header' style='margin-left: 10px;'>
              <div class='step-title'>${d['sink-label']}</div>
              <div class='step-caption'>${abbreviate(d.rows)} ${d.row === 1 ? 'row' : 'rows'}</div>
            </div>
          </div>
          <div class='step-body'>${d['sink-description'] || ''}</div>
        </div>
      `)
    .on('click', d => onRequestDetail(d));
  steps
    .call(recurse);
  const edges =
    container.selectAll('p.edge')
      .data(getEdges(data))
      .enter().insert('path', ':first-child')
      .attr('class', 'edge')
      .attr('d', d => getConnectionPath(d, svg, container));
  reset(zoom, svg, container);


  function recurse(step) {
    const children =
      step
        .selectAll('g.child')
        .data(d => d.children || []).enter()
        .append('g')
        .attr('class', 'child')
        .style('transform', (d, index) => `translateY(${index * 100}px)`);
    children.each(function(d) {
      const child = d3.select(this);
      const steps =
        child.selectAll('g.step')
          .data(d => d.steps || []).enter()
          .append('g')
          .attr('class', 'step')
          .style('transform', (d, index) => `translateX(${250 + index * 150}px)`);
      steps
        .append('foreignObject')
        .attr('id', d => d.uuid)
        .attr('class', d => `step step-${d.type}`)
        .classed('step-source', d => d.operator === 'TableScan')
        .attr('height', 55)
        .attr('width', d => d.type === 'source' ? 200 : 140)
        .append('xhtml:body')
        .style('margin', 0)
        .html(d => getRenderer(d.type)(d))
        .on('click', d => onRequestDetail(d));
      steps.filter(d => Array.isArray(d.children))
        .call(recurse);
    });
  }
}

function getRenderer(type) {
  const renderer = RENDER_GROUP[type];
  if(renderer) {
    return renderer;
  }

  if(type === 'stage') {
    return (d => `
      <div style='display:flex;'>
        <div class='step-meta'>
          <i class='fa ' aria-hidden='true'></i>
        </div>
        <div class='step-body' style='margin-left: 10px;'>
          <div>Stage</div>
          <!--div><span style='font-weight: lighter;'>Rows:</span> ${abbreviate(getNumberOfRows(d['Statistics:']))}</div-->
        </div>
      </div>
    `);
  }

  return (d => {
    const isSource = d.operator === 'TableScan';
    return (`
      <div style='display:flex;'>
        <div class='step-meta'>
          <i class='fa ${getOperatorIcon(d.operator)}' aria-hidden='true'></i>
        </div>
        <div class='step-body' style='margin-left: 10px;'>
          <div>${isSource ? d['alias:'] : getOperatorLabel(d.operator)}</div>
          <div><span style='font-weight: lighter;'>Rows:</span> ${abbreviate(getNumberOfRows(d['Statistics:']))}</div>
        </div>
      </div>
    `);
  });
}
function getNumberOfRows(statistics) {
  const match = statistics.match(/([^\?]*)\Num rows: (\d*)/);
  return (match.length === 3 && Number.isNaN(Number(match[2])) === false) ? match[2] : 0;
}
function getOperatorLabel(operator) {
  const operatorStr = operator.toString();
  if(operatorStr.endsWith(' Operator')) {
    return operatorStr.substring(0, operatorStr.length - ' Operator'.length);
  }
  if(operatorStr === 'TableScan') {
    return 'Scan';
  }
  return operatorStr ? operatorStr : 'Unknown';
}
function getOperatorIcon(operator) {
  switch(operator) {
    case 'File Output Operator':
      return 'fa-file-o';
    case 'Reduce Output Operator':
      return 'fa-compress';
    case 'Filter Operator':
      return 'fa-filter';
    case 'Dynamic Partitioning Event Operator':
      return 'fa-columns'
    case 'Map Join Operator':
      return 'fa-code-fork'
    case 'Limit':
    case 'Group By Operator':
    case 'Select Operator':
    case 'TableScan':
      return 'fa-table';
    default:
      return '';
  }
}
function getIcon (type, subtype) {
  switch(type) {
    case 'join':
      return 'fa-code-fork'
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
};
function abbreviate(value) {
  let newValue = value;
  if (value >= 1000) {
    const suffixes = ["", "k", "m", "b","t"];
    const suffixNum = Math.floor(("" + value).length / 3);
    let shortValue = '';
    for (var precision = 2; precision >= 1; precision--) {
      shortValue = parseFloat( (suffixNum != 0 ? (value / Math.pow(1000,suffixNum) ) : value).toPrecision(precision));
      const dotLessShortValue = (shortValue + '').replace(/[^a-zA-Z 0-9]+/g,'');
      if (dotLessShortValue.length <= 2) { break; }
    }
    if (shortValue % 1 != 0) {
      const  shortNum = shortValue.toFixed(1);
    }
    newValue = shortValue+suffixes[suffixNum];
  }
  return newValue;
}
function reset(zoom, svg, container) {
  const steps = container.selectAll('g.step');
  const bounds = [];
  steps.each(function(d) {
    const cStep = d3.select(this);
    const box = cStep.node().getBoundingClientRect();
    bounds.push(box);
  });
  const PADDING_PERCENT = 0.95;
  const fullWidth = svg.node().clientWidth;
  const fullHeight = svg.node().clientHeight;
  const offsetY = svg.node().getBoundingClientRect().top;
  const top = Math.min(...bounds.map(cBound => cBound.top));
  const left = Math.min(...bounds.map(cBound => cBound.left));
  const width = Math.max(...bounds.map(cBound => cBound.right)) - left;
  const height = Math.max(...bounds.map(cBound => cBound.bottom)) - top;
  const midX = left + width / 2;
  const midY = top + height / 2;
  if (width == 0 || height == 0) return; // nothing to fit
  const scale = PADDING_PERCENT / Math.max(width / fullWidth, height / fullHeight);
  const translate = [fullWidth / 2 - scale * midX, fullHeight / 2 - scale * midY];
  const zoomIdentity =
    d3.zoomIdentity
      .translate(translate[0], translate[1] + offsetY)
      .scale(scale);
  svg
    .transition()
    // .delay(750)
    .duration(750)
    // .call( zoom.transform, d3.zoomIdentity.translate(0, 0).scale(1) ); // not in d3 v4
    .call(zoom.transform, zoomIdentity);
}
function getConnectionPath(edge, svg, container) {
  const steps = container.selectAll('foreignObject.step');
  const source = container.select(`#${edge.source}`);
  const target = container.select(`#${edge.target}`);
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
    pSource
  ];
  if(pSource.y !== pTarget.y) {
    path.push({
      x: (pSource.x + pTarget.x) / 2,
      y: pSource.y
    }, {
      x: (pSource.x + pTarget.x) / 2,
      y: pTarget.y
    })
  }
  path.push(pTarget);
  const offsetY = svg.node().getBoundingClientRect().top;
  return path.reduce((accumulator, cPoint, index) => {
    if(index === 0) {
      return accumulator + `M ${cPoint.x}, ${cPoint.y - offsetY}\n`
    } else {
      return accumulator + `L ${cPoint.x}, ${cPoint.y - offsetY}\n`
    }
  }, '');
}
function uuid() {
  return 'step-xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
    const r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
    return v.toString(16);
  });
}
function getEdges(steps) {
  const edges = [];
  for (let prev, index = 0; index < steps.length; index++) {
    const cStep = steps[index];
    if(prev) {
      edges.push({
        source: prev.uuid,
        target: cStep.uuid
      });
    }
    prev = cStep;
    if(Array.isArray(cStep.children)) {
      cStep.children.forEach(cChild => {
        if(cChild.steps.length === 0) {
          return;
        }
        edges.push({
          source: cStep.uuid,
          target: cChild.steps[0].uuid
        });
        edges.push(...getEdges(cChild.steps));
      });
    }
  }
  return edges;
}
function getTransformed(steps) {
  return steps.map(cStep => {
    let cResStep = cStep;
    cResStep = Object.assign({}, cResStep, {
      uuid: uuid()
    });
    if(Array.isArray(cResStep.children)) {
      const children = cResStep.children.map(cChild => Object.assign({}, cChild, {
        steps: getTransformed(cChild.steps)
      }));
      cResStep = Object.assign({}, cResStep, {
        children: children
      });
    }
    return cResStep;
  });
}
function createOrder(data) {
  const stageDeps = data['STAGE DEPENDENCIES'];
  const stagePlans = data['STAGE PLANS'];
  const stageRootKey = Object.keys(stageDeps).find(cStageKey => stageDeps[cStageKey]['ROOT STAGE'] === 'TRUE');
  const root = Object.assign({}, getStageData(stageRootKey, stagePlans), {
    _stages: getDependentStageTreeInOrder(stageRootKey, stageDeps, stagePlans)
  });
  const expanded = doExpandChild(root);
  return doClean(expanded);
}
function getDependentStageTreeInOrder(sourceStageKey, stageDeps, stagePlans) {
  const stageKeys =
    Object
      .keys(stageDeps)
      .filter(cStageKey => stageDeps[cStageKey] && stageDeps[cStageKey]['DEPENDENT STAGES'] === sourceStageKey);
  const stages =
    stageKeys.map(cStageKey => Object.assign({}, getStageData(cStageKey, stagePlans), {
      _stages: getDependentStageTreeInOrder(cStageKey, stageDeps, stagePlans)
    }));
  return stages;
}
function getStageData(stageKey, stagePlans) {
  const plan = stagePlans[stageKey];
  const engineKeys = Object.keys(plan);
  if(engineKeys.length !== 1) {
    return plan;
  }
  const engineKey = engineKeys[0];
  // returns a job
  let step;
  switch(engineKey) {
    case 'Map Reduce':
      step = buildForMR(plan[engineKey]);
      break;
    case 'Map Reduce Local Work':
      step = buildForMRLocal(plan[engineKey]);
      break;
    case 'Tez':
      step = buildForTez(plan[engineKey]);
      break;
    case 'Fetch Operator':
      step = buildForFetch(plan[engineKey]);
      break;
    default:
      step = {
        type: 'placeholder',
        _engine: 'not_found',
        _plan: plan
      };
  }
  return ({
    steps: [
      step
    ]
  });
}
function buildForMR(plan) {
  return ({
    type: 'stage',
    _engine: 'mr',
    _plan: plan
  });
}
function buildForMRLocal(plan) {
  return ({
    type: 'stage',
    _engine: 'mr-local',
    _plan: plan
  });
}
function buildForTez(plan) {
  const edges = plan['Edges:'];
  const vertices = plan['Vertices:'];
  const fEdges =
    Object
      .keys(edges)
      .reduce((accumulator, cTargetKey) => {
        if(Array.isArray(edges[cTargetKey])) {
          const edgesFromSourceKey = edges[cTargetKey];
          accumulator.push(...edgesFromSourceKey.map(cEdgeFromSourceKey => ({
            source: cEdgeFromSourceKey['parent'],
            target: cTargetKey,
            type: cEdgeFromSourceKey['type']
          })));
        } else {
          const edgeFromSourceKey = edges[cTargetKey];
          accumulator.push({
            source: edgeFromSourceKey['parent'],
            target: cTargetKey,
            type: edgeFromSourceKey['type']
          });
        }
        return accumulator;
      }, []);
  const rootKey = fEdges.find(cEdge => fEdges.some(iEdge => iEdge.source === cEdge.target) === false).target;
  return Object.assign({}, doTezBuildTreeFromEdges(rootKey, fEdges, vertices), {
    _engine: 'tez',
    _plan: plan
  });
}
function buildForFetch(plan) {
  return ({
    type: 'stage',
    _engine: 'fetch',
    _plan: plan
  });
}
function doTezBuildTreeFromEdges(parentKey, edges, vertices) {
  const jobs =
    Object
      .keys(vertices)
      .map(cVertexKey => ({
        type: 'job',
        label: cVertexKey,
        _data: vertices[cVertexKey],
      }))
      .reduce((accumulator, cVertex) => Object.assign(accumulator, {
        [cVertex.label]: cVertex
      }), {});
  edges.forEach(cEdge => {
    const job = jobs[cEdge.target];
    if(!Array.isArray(job.children)) {
      job.children = [];
    }
    const steps = [];
    if(cEdge.type === 'BROADCAST_EDGE') {
      steps.push({
        type: 'broadcast',
        _data: jobs[cEdge.target],
      });
    }
    steps.push(jobs[cEdge.source]);
    job.children.push({
      steps
    });
  });
  return jobs[parentKey];
}
function doExpandChild(node) {
  return Object.assign({}, node, {
    steps: node.steps.reduce((accumulator, cStep) => [...accumulator, ...doExpandStep(cStep, 'step')], [])
  });
}
function doExpandStep(node) {
  switch(node.type) {
    case 'job':
      const key = Object.keys(doOmit(node._data, ['Execution mode:']))[0];
      let root = node._data[key];
      if(!Array.isArray(root)) {
        root = [root];
      }
      const steps = doGetOperators(root);
      const children = Array.isArray(node.children) ? node.children.map(cChild => doExpandChild(cChild)) : [];
      return ([
        doOmit(node, ['children']),
        ...steps.reverse().slice(0, steps.length - 1),
        Object.assign({}, steps[steps.length - 1], {
          children
        })
      ]);
    default:
      return [node];
  }
}
function doClean(node) {
  let cleaned =
    Object
      .keys(node)
      .filter(cNodeKey => cNodeKey.startsWith('_') === false)
      .reduce((accumulator, cNodeKey) => Object.assign(accumulator, {
        [cNodeKey]: node[cNodeKey]
      }), {});
  if(cleaned.hasOwnProperty('children')) {
    cleaned = Object.assign({}, cleaned, {
      children: cleaned.children.map(cChild => doClean(cChild))
    })
  }
  if(cleaned.hasOwnProperty('steps')) {
    cleaned = Object.assign({}, cleaned, {
      steps: cleaned.steps.map(cStep => doClean(cStep))
    })
  }
  return cleaned;
}
function doGetOperators(node) {
  let stepx = node;
  if(!Array.isArray(stepx)) {
    stepx = [stepx];
  }
  const steps =
    stepx
      .reduce((accumulator, cStep) => {
        const key = Object.keys(cStep)[0];
        const obj = cStep[key];
        let children = [];
        if(obj.children) {
          children = doGetOperators(obj.children);
        }
        const filtered =
          Object
            .keys(obj)
            .filter(cKey => cKey !== 'children')
            .reduce((accumulator, cKey) => {
              accumulator[cKey] = obj[cKey];
              return accumulator;
            }, {});
        return [
          ...accumulator,
          Object.assign({
            _data: cStep,
            operator: key
          }, filtered),
          ...children
        ];
      }, []);
  return steps;
}
function doGetStep(node) {
  const key = Object.keys(node)[0];
  const obj = node[key];
  return {
    operator: key,
    _data: obj
  };
}
function doOmit(object, keys) {
  return Object
    .keys(object)
    .filter(cKey => keys.indexOf(cKey) === -1)
    .reduce((accumulator, cKey) => {
      accumulator[cKey] = object[cKey];
      return accumulator;
    }, {});
}

