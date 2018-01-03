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

import {Component, OnInit, Input, Output, EventEmitter} from '@angular/core';
import * as d3 from 'd3';
import * as moment from 'moment-timezone';
import {AppSettingsService} from '@app/services/storage/app-settings.service';
import {GraphComponent} from '@app/classes/components/graph/graph.component';
import {GraphScaleItem} from '@app/classes/graph';

@Component({
  selector: 'time-histogram',
  templateUrl: './time-histogram.component.html',
  styleUrls: ['../../classes/components/graph/graph.component.less', './time-histogram.component.less']
})
export class TimeHistogramComponent extends GraphComponent implements OnInit {

  constructor(private appSettings: AppSettingsService) {
    super();
  }

  ngOnInit() {
    this.appSettings.getParameter('timeZone').subscribe((value: string): void => {
      this.timeZone = value;
      this.createGraph();
    });
  }

  @Input()
  columnWidth = {
    second: 40,
    minute: 30,
    hour: 25,
    day: 20,
    base: 20
  };

  @Output()
  selectArea: EventEmitter<number[]> = new EventEmitter();

  readonly isTimeGraph: boolean = true;

  private timeZone: string;

  private dragArea: d3.Selection<SVGGraphicsElement, undefined, SVGGraphicsElement, undefined>;

  private dragStartX: number;

  private minDragX: number;

  private maxDragX: number;

  private readonly tickTimeFormat: string = 'MM/DD HH:mm';

  private readonly historyStartEndTimeFormat: string = 'dddd, MMMM DD, YYYY';

  histogram: any;

  /**
   * This property holds the data structure describing the gaps between the xAxis ticks.
   * The unit property can be: second, minute, hour, day
   * The value is the number of the given unit.
   */
  private chartTimeGap: {value: number, unit: string, label: string} | null;
  /**
   * This is the rectangle element to represent the unselected time range on the left side of the selected time range
   */
  private leftDragArea: d3.Selection<SVGGraphicsElement, undefined, SVGGraphicsElement, undefined>;
  /**
   * This is the rectangle element to represent the unselected time range on the right side of the selected time range
   */
  private rightDragArea: d3.Selection<SVGGraphicsElement, undefined, SVGGraphicsElement, undefined>;
  /**
   * This is a Date object holding the value of the first tick of the xAxis. It is a helper getter for the template.
   */
  private get firstDateTick(): Date | undefined {
    const ticks = this.xScale && this.xScale.ticks();
    return (ticks && ticks.length && ticks[0]) || undefined;
  }
  /**
   * This is a Date object holding the value of the last tick of the xAxis. It is a helper getter for the template.
   */
  private get lastDateTick(): Date | undefined {
    const ticks = this.xScale && this.xScale.ticks();
    return (ticks && ticks.length && ticks[ticks.length-1]) || undefined;
  }

  protected xAxisTickFormatter = (tick: Date): string => {
    return moment(tick).tz(this.timeZone).format(this.tickTimeFormat);
  };

  protected yAxisTickFormatter = (tick: number): string | undefined => {
    return Number.isInteger(tick) ? tick.toFixed(0) : undefined;
  };

  /**
   * The goal is to calculate the time gap between the given dates. It will return an object representing the unit and
   * the value in the given unit. Eg.: {unit: 'minute', value: 5}
   * @param {Date} startDate
   * @param {Date} endDate
   * @returns {{value: number; unit: string, label: string}}
   */
  private getTimeGap(startDate: Date, endDate: Date): {value: number, unit: string, label: string} {
    const startDateMoment = moment(startDate);
    const endDateMoment = moment(endDate);
    const diffInWeek: number = endDateMoment.diff(startDateMoment, 'weeks');
    const diffInDay: number = endDateMoment.diff(startDateMoment, 'days');
    const diffInHour: number = endDateMoment.diff(startDateMoment, 'hours');
    const diffInMin: number = endDateMoment.diff(startDateMoment, 'minutes');
    const diffInSec: number = endDateMoment.diff(startDateMoment, 'seconds');
    const value = diffInWeek >= 1 ? diffInWeek : (
      diffInDay >= 1 ? diffInDay : (
        diffInHour >= 1 ? diffInHour : (diffInMin >= 1 ? diffInMin : diffInSec)
      )
    );
    const unit: string = diffInWeek >= 1 ? 'week' : (
      diffInDay >= 1 ? `day` : (
        diffInHour >= 1 ? `hour` : (diffInMin >= 1 ? `minute` : `second`)
      )
    );
    const label = `histogram.gap.${unit}${value>1 ? 's' : ''}`;
    return {
      value,
      unit,
      label
    };
  }

  /**
   * The goal is to have a simple function to set the time gap corresponding to the xScale ticks.
   * It will reset the time gap if the xScale is not set or there are no ticks.
   */
  private setChartTimeGapByXScale() {
    let ticks = this.xScale && this.xScale.ticks();
    if (ticks && ticks.length) {
      this.setChartTimeGap(ticks[0], ticks[1] || ticks[0]);
    } else {
      this.resetChartTimeGap();
    }
  }

  /**
   * Simply reset the time gap property to null.
   */
  private resetChartTimeGap(): void {
    this.chartTimeGap = null;
  }

  /**
   * The goal is to have a single point where we set the chartTimeGap property corresponding the given timerange.
   * @param {Date} startDate
   * @param {Date} endDate
   */
  private setChartTimeGap(startDate: Date, endDate: Date): void {
    this.chartTimeGap = this.getTimeGap(startDate, endDate);
  }

  /**
   * Set the domain for the y scale regarding the given data. The maximum value of the data is the sum of the log level
   * values.
   * An example data: [{tick: 1233455677, WARN: 12, ERROR: 123}]
   * @param {GraphScaleItem[]} data
   */
  protected setYScaleDomain(data: GraphScaleItem[]): void {
    const keys = Object.keys(this.labels);
    const maxYValue = d3.max(data, item => keys.reduce((sum: number, key: string): number => sum + item[key], 0));
    this.yScale.domain([0, maxYValue]);
  }

  /**
   * Set the domain values for the x scale regarding the given data.
   * An example data: [{tick: 1233455677, WARN: 12, ERROR: 123}]
   * @param {GraphScaleItem[]} data
   */
  protected setXScaleDomain(data: GraphScaleItem[]): void {
    this.xScale.domain(d3.extent(data, item => item.tick)).nice();
  }

  protected populate(): void {
    const keys = Object.keys(this.colors);
    const data = this.data;
    const timeStamps = Object.keys(data);
    // we create a more consumable data structure for d3
    const formattedData = timeStamps.map((timeStamp: string): {tick: number, [key: string]: number} => Object.assign({
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
    this.drawXAxis();
    this.drawYAxis();

    // populate the data and drawing the bars
    const layer = this.svg.selectAll().data(d3.transpose<any>(layers))
      .enter().append('g')
      .attr('class', 'value');
    layer.selectAll().data(item => item).enter().append('rect')
        .attr('x', item => this.xScale(item.data.tick) - columnWidth / 2)
        .attr('y', item => this.yScale(item[1]))
        .attr('height', item => this.yScale(item[0]) - this.yScale(item[1]))
        .attr('width', columnWidth.toString())
        .style('fill', (item, index) => this.orderedColors[index])
        .on('mouseover', this.handleRectMouseOver)
        .on('mousemove', this.handleRectMouseMove)
        .on('mouseout', this.handleRectMouseOut);
    this.setDragBehavior();
  }

  private getTimeRangeByXRanges(startX: number, endX: number): [number, number] {
    const xScaleInterval = this.xScale.domain().map((point: Date): number => point.valueOf());
    const xScaleLength = xScaleInterval[1] - xScaleInterval[0];
    const ratio = xScaleLength / this.width;
    return [Math.round(xScaleInterval[0] + ratio * startX), Math.round(xScaleInterval[0] + ratio * endX)];
  }

  /**
   * The goal is to create the two shadow rectangle beside the selected area. Actually we blurout the not selected
   * timeranges
   * @param {number} startX This is the starting position of the drag event withing the container
   * @param {number} currentX This is the ending point of the drag within the container
   */
  private createInvertDragArea(startX: number, currentX: number): void {
    const height: number = this.height + this.margin.top + this.margin.bottom;
    this.leftDragArea = this.svg.insert('rect').attr('height', height).attr('class', 'unselected-drag-area');
    this.rightDragArea = this.svg.insert('rect').attr('height', height).attr('class', 'unselected-drag-area');
    this.setInvertDragArea(startX, currentX);
  }

  /**
   * Set the position and the width of the blur/shadow rectangles of the unselected area(s).
   * @param {number} startX The start point of the selected area.
   * @param {number} currentX The end point of the selected area.
   */
  private setInvertDragArea(startX: number, currentX: number): void {
    const left: number = Math.min(startX, currentX);
    const right: number = Math.max(startX, currentX);
    const rightAreaWidth: number = Math.max(0, this.width - right);
    const leftAreaWidth: number = Math.max(0, left);
    this.leftDragArea.attr('x', 0).attr('width', leftAreaWidth);
    this.rightDragArea.attr('x', right).attr('width', rightAreaWidth);
  }

  /**
   * The goal is to have a single point where we remove the rectangles of the blur/shadow, unselected time range(s)
   */
  private clearInvertDragArea(): void {
    this.leftDragArea.remove();
    this.rightDragArea.remove();
  }

  private setDragBehavior(): void {
    this.minDragX = this.margin.left;
    this.maxDragX = this.graphContainer.clientWidth;
    d3.selectAll(`svg#${this.svgId}`).call(d3.drag()
      .on('start', (datum: undefined, index: number, containers: d3.ContainerElement[]): void => {
        if (this.dragArea) {
          this.dragArea.remove();
        }
        this.dragStartX = Math.max(0, this.getDragX(containers[0]) - this.margin.left);
        this.dragArea = this.svg.insert('rect', ':first-child').attr('x', this.dragStartX).attr('y', 0).attr('width', 0)
          .attr('height', this.height).attr('class', 'drag-area');
      })
      .on('drag', (datum: undefined, index: number, containers: d3.ContainerElement[]): void => {
        const mousePos = this.getDragX(containers[0]);
        const currentX = Math.max(mousePos, this.minDragX) - this.margin.left;
        const startX = Math.min(currentX, this.dragStartX);
        const currentWidth = Math.abs(currentX - this.dragStartX);
        this.dragArea.attr('x', startX).attr('width', currentWidth);
        let timeRange = this.getTimeRangeByXRanges(startX, startX + currentWidth);
        this.setChartTimeGap(new Date(timeRange[0]), new Date(timeRange[1]));
      })
      .on('end', (): void => {
        const dragAreaDetails = this.dragArea.node().getBBox();
        const startX = Math.max(0, dragAreaDetails.x);
        const endX = Math.min(this.width, dragAreaDetails.x + dragAreaDetails.width);
        const dateRange: [number, number] = this.getTimeRangeByXRanges(startX, endX);
        this.selectArea.emit(dateRange);
        this.dragArea.remove();
        this.setChartTimeGap(new Date(dateRange[0]), new Date(dateRange[1]));
      })
    );
    d3.selectAll(`svg#${this.svgId} .value, svg#${this.svgId} .axis`).call(d3.drag().on('start', (): void => {
      d3.event.sourceEvent.stopPropagation();
    }));
  }

  private getDragX(element: d3.ContainerElement): number {
    return d3.mouse(element)[0];
  }

}
