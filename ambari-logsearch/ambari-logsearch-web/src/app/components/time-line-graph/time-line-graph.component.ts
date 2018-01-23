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
import {GraphScaleItem, GraphLinePoint, GraphLineData} from '@app/classes/graph';
import {TimeGraphComponent} from '@app/classes/components/graph/time-graph.component';

@Component({
  selector: 'time-line-graph',
  templateUrl: './time-line-graph.component.html',
  styleUrls: [
    '../../classes/components/graph/graph.component.less', '../../classes/components/graph/time-graph.component.less',
    './time-line-graph.component.less'
  ]
})
export class TimeLineGraphComponent extends TimeGraphComponent {

  @Input()
  pointRadius: number = 3.5;

  protected populate(): void {
    const keys = Object.keys(this.colors),
      data = this.data,
      timeStamps = Object.keys(data),
      dataForDomain = timeStamps.map((timeStamp: string): GraphScaleItem => Object.assign({
        tick: Number(timeStamp)
      }, data[timeStamp])),
      dataForSvg = keys.map((key: string): GraphLineData => {
        return {
          points: timeStamps.map((timeStamp: string): GraphScaleItem => {
            return {
              tick: Number(timeStamp),
              y: data[timeStamp][key]
            };
          }),
          key: key
        };
      }),
      line = d3.line<GraphScaleItem>().x(item => this.xScale(item.tick)).y(item => this.yScale(item.y));

    // after we have the data we set the domain values both scales
    this.setXScaleDomain(dataForDomain);
    this.setYScaleDomain();

    // drawing the axis
    this.drawXAxis();
    this.drawYAxis();

    // populate the data and drawing the lines and points
    const layer = this.svg.selectAll().data(dataForSvg);
    layer.enter().append('path')
      .attr('class', 'line').attr('d', (item: GraphLineData) => line(item.points))
      .style('stroke', (item: GraphLineData): string => this.colors[item.key]);
    layer.enter().append('g').selectAll('circle')
      .data((item: GraphLineData): GraphLinePoint[] => item.points.map((point: GraphScaleItem): GraphLinePoint => {
        return Object.assign({}, point, {
          color: this.colors[item.key]
        });
      }))
      .enter().append('circle')
      .attr('cx', (item: GraphLinePoint): number => this.xScale(item.tick))
      .attr('cy', (item: GraphLinePoint): number => this.yScale(item.y))
      .attr('r', this.pointRadius)
      .style('fill', (item: GraphLinePoint): string => item.color);
    const gridLinesParent = this.svg.selectAll().data(dataForDomain).enter().append('g').selectAll()
      .data((item: GraphScaleItem): GraphScaleItem[] => [item]).enter();
    gridLinesParent.append('rect').attr('class', 'grid-line-area')
      .attr('x', (item: GraphScaleItem): number => this.xScale(item.tick) - this.pointRadius).attr('y', 0)
      .style('width', `${this.pointRadius * 2}px`).style('height', `${this.height}px`)
      .on('mouseover', (d: GraphScaleItem, index: number, elements: HTMLElement[]): void => {
        elements.forEach((element: HTMLElement) => element.classList.add('visible-grid-line-area'));
        this.handleMouseOver(Object.assign([], d, {
          data: d
        }), index, elements);
      })
      .on('mousemove', this.handleMouseMove)
      .on('mouseout', (d: GraphScaleItem, index: number, elements: HTMLElement[]): void => {
        elements.forEach((element: HTMLElement) => element.classList.remove('visible-grid-line-area'));
        this.handleMouseOut();
      });
    gridLinesParent.append('line').attr('class', 'grid-line')
      .attr('x1', (item: GraphScaleItem): number => this.xScale(item.tick))
      .attr('x2', (item: GraphScaleItem): number => this.xScale(item.tick))
      .attr('y1', 0).attr('y2', this.height);
    this.setDragBehavior();
  }

  protected setYScaleDomain(): void {
    const keys = Object.keys(this.data),
      maxValues = keys.map((currentKey: string): number => this.utils.getMaxNumberInObject(this.data[currentKey]), 0),
      maximum = Math.max(...maxValues);
    this.yScale.domain([0, maximum]);
  }

}
