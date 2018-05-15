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

import {Component, Input} from '@angular/core';
import * as d3 from 'd3';
import {TimeGraphComponent} from '@app/classes/components/graph/time-graph.component';
import {GraphScaleItem} from '@app/classes/graph';

@Component({
  selector: 'time-histogram',
  templateUrl: './time-histogram.component.html',
  styleUrls: [
    '../../classes/components/graph/graph.component.less', '../../classes/components/graph/time-graph.component.less',
    './time-histogram.component.less'
  ]
})
export class TimeHistogramComponent extends TimeGraphComponent {

  @Input()
  columnWidth = {
    second: 40,
    minute: 30,
    hour: 25,
    day: 20,
    base: 20
  };

  constructor() {
    super();
  }

  protected setYScaleDomain(data: GraphScaleItem[]): void {
    const keys = Object.keys(this.labels);
    const maxYValue = d3.max(data, item => keys.reduce((sum: number, key: string): number => sum + item[key], 0));
    this.yScale.domain([0, maxYValue]);
  }

  protected populate(): void {
    const keys = Object.keys(this.colors);
    const data = this.data;
    const timeStamps = Object.keys(data);
    // we create a more consumable data structure for d3
    const formattedData = timeStamps.map((timeStamp: string): GraphScaleItem => Object.assign({
        tick: Number(timeStamp)
      }, data[timeStamp]));
    const layers = d3.stack().keys(keys)(formattedData);

    // after we have the data we set the domain values both scales
    this.setXScaleDomain(formattedData);
    this.setYScaleDomain(formattedData);

    // Setting the timegap label above the chart
    this.setChartTimeGapByXScale();

    const unitD3TimeProp = this.chartTimeGap.unit.charAt(0).toUpperCase() + this.chartTimeGap.unit.slice(1);
    this.xScale.nice(d3[`time${unitD3TimeProp}`], 2);

    const columnWidth = this.columnWidth[this.chartTimeGap.unit] || this.columnWidth.base;

    // drawing the axis
    this.drawXAxis(null, (columnWidth / 2) + 2);
    this.drawYAxis();

    // populate the data and drawing the bars
    const layer = this.svg.selectAll().data(d3.transpose<any>(layers))
      .enter().append('g')
      .attr('class', 'value');
    layer.selectAll().data(item => item).enter().append('rect')
        .attr('transform', `translate(${(columnWidth / 2) + 2}, 0)`)
        .attr('x', item => this.xScale(item.data.tick) - columnWidth / 2)
        .attr('y', item => this.yScale(item[1]))
        .attr('height', item => this.yScale(item[0]) - this.yScale(item[1]))
        .attr('width', columnWidth.toString())
        .style('fill', (item, index) => this.orderedColors[index])
        .on('mouseover', this.handleMouseOver)
        .on('mousemove', this.handleMouseMove)
        .on('mouseout', this.handleMouseOut);
    this.setDragBehavior();
  }

}
