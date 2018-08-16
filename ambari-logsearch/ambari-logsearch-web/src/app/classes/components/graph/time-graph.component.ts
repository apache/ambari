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

import {OnInit, Input, Output, EventEmitter} from '@angular/core';
import * as d3 from 'd3';
import * as moment from 'moment-timezone';
import {AppSettingsService} from '@app/services/storage/app-settings.service';
import {ChartTimeGap, GraphScaleItem} from '@app/classes/graph';
import {ServiceInjector} from '@app/classes/service-injector';
import {GraphComponent} from '@app/classes/components/graph/graph.component';

export class TimeGraphComponent extends GraphComponent implements OnInit {

  @Input()
  tickTimeFormat: string = 'MM/DD HH:mm';

  @Input()
  historyStartEndTimeFormat: string = 'dddd, MMMM DD, YYYY';

  @Input()
  defaultChartTimeGap: ChartTimeGap = {
    value: 1,
    unit: 'h',
    label: 'filter.timeRange.1hr'
  };

  @Output()
  selectArea: EventEmitter<number[]> = new EventEmitter();

  readonly isTimeGraph: boolean = true;

  readonly allowFractionalXTicks: boolean = false;

  protected appSettings: AppSettingsService;

  protected dragArea: d3.Selection<SVGGraphicsElement, undefined, SVGGraphicsElement, undefined>;

  protected dragStartX: number;

  protected minDragX: number;

  protected maxDragX: number;

  protected timeZone: string;

  /**
   * This property holds the data structure describing the gaps between the xAxis ticks.
   * The unit property can be: second, minute, hour, day
   * The value is the number of the given unit.
   */
  protected chartTimeGap: ChartTimeGap | null;
  /**
   * This is the rectangle element to represent the unselected time range on the left side of the selected time range
   */
  protected leftDragArea: d3.Selection<SVGGraphicsElement, undefined, SVGGraphicsElement, undefined>;
  /**
   * This is the rectangle element to represent the unselected time range on the right side of the selected time range
   */
  protected rightDragArea: d3.Selection<SVGGraphicsElement, undefined, SVGGraphicsElement, undefined>;

  constructor() {
    super();
    this.appSettings = ServiceInjector.injector.get(AppSettingsService);
  }

  ngOnInit() {
    this.subscriptions.push(
      this.appSettings.getParameter('timeZone').subscribe((value: string): void => {
        this.timeZone = value;
        this.createGraph();
      })
    );
    super.ngOnInit();
  }

  /**
   * This is a Date object holding the value of the first tick of the xAxis. It is a helper getter for the template.
   */
  protected get firstDateTick(): Date | undefined {
    const ticks = this.xScale && this.xScale.ticks();
    return (ticks && ticks.length && ticks[0]) || undefined;
  }

  /**
   * This is a Date object holding the value of the last tick of the xAxis. It is a helper getter for the template.
   */
  protected get lastDateTick(): Date | undefined {
    const ticks = this.xScale && this.xScale.ticks();
    return (ticks && ticks.length && ticks[ticks.length - 1]) || undefined;
  }

  protected xAxisTickFormatter = (tick: Date): string => {
    return moment(tick).tz(this.timeZone).format(this.tickTimeFormat);
  }

  protected setXScaleDomain(data: GraphScaleItem[]): void {
    this.xScale.domain(d3.extent(data, item => item.tick)).nice().domain();
  }

  /**
   * The goal is to calculate the time gap between the given dates. It will return an object representing the unit and
   * the value in the given unit. Eg.: {unit: 'minute', value: 5}
   * @param {Date} startDate
   * @param {Date} endDate
   * @returns {ChartTimeGap}
   */
  protected getTimeGap(startDate: Date, endDate: Date): ChartTimeGap {
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
    const label = `histogram.gap.${unit}${value > 1 ? 's' : ''}`;
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
  protected setChartTimeGapByXScale(): void {
    const ticks = this.xScale && this.xScale.ticks();
    if (ticks && ticks.length) {
      this.setChartTimeGap(ticks[0], ticks[1] || ticks[0]);
    } else {
      this.resetChartTimeGap();
    }
  }

  /**
   * Simply reset the time gap property to null.
   */
  protected resetChartTimeGap(): void {
    this.chartTimeGap = this.defaultChartTimeGap;
  }

  /**
   * The goal is to have a single point where we set the chartTimeGap property corresponding the given timerange.
   * @param {Date} startDate
   * @param {Date} endDate
   */
  protected setChartTimeGap(startDate: Date, endDate: Date): void {
    const gap: ChartTimeGap = this.getTimeGap(startDate, endDate);
    if (gap.value > 0) {
      this.chartTimeGap = gap;
    }
  }

  protected getTimeRangeByXRanges(startX: number, endX: number): [number, number] {
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
  protected createInvertDragArea(startX: number, currentX: number): void {
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
  protected setInvertDragArea(startX: number, currentX: number): void {
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
  protected clearInvertDragArea(): void {
    this.leftDragArea.remove();
    this.rightDragArea.remove();
  }

  protected setDragBehavior(): void {
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
        this.createInvertDragArea(this.dragStartX, this.dragStartX);
      })
      .on('drag', (datum: undefined, index: number, containers: d3.ContainerElement[]): void => {
        const mousePos = this.getDragX(containers[0]);
        const currentX = Math.max(mousePos, this.minDragX) - this.margin.left;
        const startX = Math.min(currentX, this.dragStartX);
        const currentWidth = Math.abs(currentX - this.dragStartX);
        this.dragArea.attr('x', startX).attr('width', currentWidth);
        const timeRange = this.getTimeRangeByXRanges(startX, startX + currentWidth);
        this.setChartTimeGap(new Date(timeRange[0]), new Date(timeRange[1]));
        this.setInvertDragArea(startX, startX + currentWidth);
      })
      .on('end', (): void => {
        const dragAreaDetails = this.dragArea.node().getBBox();
        const startX = Math.max(0, dragAreaDetails.x);
        const endX = Math.min(this.width, dragAreaDetails.x + dragAreaDetails.width);
        if (endX !== startX) {
          const dateRange: [number, number] = this.getTimeRangeByXRanges(startX, endX);
          this.selectArea.emit(dateRange);
          this.dragArea.remove();
          this.setChartTimeGap(new Date(dateRange[0]), new Date(dateRange[1]));
        }
        this.clearInvertDragArea();
      })
    );
    d3.selectAll(`svg#${this.svgId} .value, svg#${this.svgId} .axis`).call(d3.drag().on('start', (): void => {
      d3.event.sourceEvent.stopPropagation();
    }));
  }

  protected getDragX(element: d3.ContainerElement): number {
    return d3.mouse(element)[0];
  }


}
