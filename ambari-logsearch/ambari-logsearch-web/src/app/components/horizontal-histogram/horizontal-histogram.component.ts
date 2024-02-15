/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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
import {GraphComponent} from '@app/classes/components/graph/graph.component';
import {HomogeneousObject} from '@app/classes/object';

@Component({
  selector: 'horizontal-histogram',
  templateUrl: './horizontal-histogram.component.html',
  styleUrls: ['../../classes/components/graph/graph.component.less', './horizontal-histogram.component.less']
})
export class HorizontalHistogramComponent extends GraphComponent {

  /**
   * Thickness of horizontal bar of the graph
   * @type {number}
   */
  @Input()
  barSize: number = 5;

  rowsCount: number;

  readonly reverseYRange: boolean = true;

  protected populate(): void {
    const barSize = this.barSize,
      data = this.data,
      yValues = Object.keys(data),
      keys = Object.keys(this.labels),
      rowsCount = yValues.reduce((currentCount: number, currentKey: string): number => {
        return currentCount + Object.keys(this.data[currentKey]).length;
      }, 0),
      formattedData = yValues.reduce((currentData, currentKey: string) => {
        const currentValues = data[currentKey],
          currentObjects = keys.map((key: string): HomogeneousObject<number> => {
            return {
              [key]: currentValues[key] || 0
            };
          });
        return [...currentData, Object.assign({
          tick: currentKey
        }, ...currentObjects)];
      }, []),
      layers = d3.stack().keys(keys)(formattedData),
      formattedLayers = d3.transpose<any>(layers);

    this.rowsCount = rowsCount;

    this.setXScaleDomain();
    this.setYScaleDomain();

    // drawing the axis
    this.drawXAxis();
    this.drawYAxis(rowsCount);

    let i = 0;

    // populate the data and drawing the bars
    this.svg.selectAll().data(formattedLayers).enter().append('g').attr('class', 'value')
      .selectAll().data(item => item).enter().append('rect')
      .attr('x', item => this.xScale(0) + 1).attr('y', item => {
        if (item [0] !== item[1]) {
          return this.yScale(i++) - this.barSize / 2;
        }
      }).attr('height', item => item[0] === item[1] ? '0' : barSize.toString())
      .attr('width', item => this.xScale(item[1]) - this.xScale(item[0]))
      .style('fill', (item, index) => this.orderedColors[index])
      .on('mouseover', this.handleMouseOver)
      .on('mousemove', this.handleMouseMove)
      .on('mouseout', this.handleMouseOut);
  }

  protected setXScaleDomain(): void {
    const keys = Object.keys(this.data),
      maxValues = keys.map((currentKey: string): number => this.utils.getMaxNumberInObject(this.data[currentKey]), 0),
      maximum = Math.max(...maxValues);
    this.xScale.domain([0, maximum]);
  }

  protected setYScaleDomain(): void {
    this.yScale.domain([0, this.rowsCount]);
  }

  protected yAxisTickFormatter = (tick: any, index: number): string | undefined => {
    const data = this.data,
      keys = Object.keys(data);
    let currentIndex = 0;
    for (let i = 0; i < keys.length && i <= index; i++) {
      const currentKey = keys[i];
      if (currentIndex === index) {
        return currentKey;
      } else {
        currentIndex += Object.keys(data[currentKey]).length;
      }
    }
  };

}
