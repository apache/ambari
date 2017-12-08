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

import {Component, OnInit, AfterViewInit, OnChanges, Input, Output, ViewChild, ElementRef, EventEmitter} from '@angular/core';
import {ContainerElement, Selection} from 'd3';
import * as d3 from 'd3';
import * as moment from 'moment-timezone';
import {AppSettingsService} from '@app/services/storage/app-settings.service';
import {HistogramStyleOptions, HistogramOptions} from '@app/classes/histogram-options';

@Component({
  selector: 'time-histogram',
  templateUrl: './time-histogram.component.html',
  styleUrls: ['./time-histogram.component.less']
})
export class TimeHistogramComponent implements OnInit, AfterViewInit, OnChanges {

  constructor(private appSettings: AppSettingsService) {}

  ngOnInit() {
    this.appSettings.getParameter('timeZone').subscribe((value: string): void => {
      this.timeZone = value;
      this.createHistogram();
    });
    this.options = Object.assign({}, this.defaultOptions, this.customOptions);
  }

  ngAfterViewInit() {
    this.htmlElement = this.element.nativeElement;
    this.tooltipElement = this.tooltipEl.nativeElement;
    this.host = d3.select(this.htmlElement);
  }

  ngOnChanges() {
    this.createHistogram();
  }

  @ViewChild('container')
  element: ElementRef;

  @ViewChild('tooltipEl')
  tooltipEl: ElementRef;

  @Input()
  svgId: string;

  @Input()
  customOptions: HistogramOptions;

  @Input()
  data: {[key: string]: number};

  @Output()
  selectArea: EventEmitter<number[]> = new EventEmitter();

  private readonly defaultOptions: HistogramStyleOptions = {
    margin: {
      top: 5,
      right: 50,
      bottom: 30,
      left: 50
    },
    height: 150,
    tickPadding: 10,
    columnWidth: {
      second: 40,
      minute: 30,
      hour: 25,
      day: 20,
      base: 20
    }
  };

  private options: HistogramOptions;

  private timeZone: string;

  private host;

  private svg;

  private width: number;

  private xScale;

  private yScale;

  private colorScale;

  private xAxis;

  private yAxis;

  private htmlElement: HTMLElement;
  private tooltipElement: HTMLElement;

  private dragArea: Selection<SVGGraphicsElement, undefined, SVGGraphicsElement, undefined>;

  private dragStartX: number;

  private minDragX: number;

  private maxDragX: number;

  private readonly tickTimeFormat: string = 'MM/DD HH:mm';
  private readonly historyStartEndTimeFormat = 'dddd, MMMM DD, YYYY';

  histogram: any;

  /**
   * This property is to hold the data of the bar where the mouse is over.
   */
  private tooltipInfo: {data: object, timeStamp: number};
  /**
   * This is the computed position of the tooltip relative to the @htmlElement which is the container of the histogram.
   * It is set when the mousemoving over the bars in the @handleRectMouseMove method.
   */
  private tooltipPosition: {top: number, left: number};
  /**
   * This property indicates if the tooltip should be positioned on the left side of the cursor or not.
   * It should be true when the tooltip is out from the window.
   * @type {boolean}
   */
  private tooltipOnTheLeft: boolean = false;
  /**
   * This property holds the data structure describing the gaps between the xAxis ticks.
   * The unit property can be: second, minute, hour, day
   * The value is the number of the given unit.
   */
  private chartTimeGap: {value: number, unit: string, label: string} | null;
  /**
   * This is the rectangle element to represent the unselected time range on the left side of the selected time range
   */
  private leftDragArea: Selection<SVGGraphicsElement, undefined, SVGGraphicsElement, undefined>;
  /**
   * This is the rectangle element to represent the unselected time range on the right side of the selected time range
   */
  private rightDragArea: Selection<SVGGraphicsElement, undefined, SVGGraphicsElement, undefined>;
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

  /**
   * This will return the information about the used levels and the connected colors and labels.
   * The goal is to provide an easy property to the template to display the legend of the levels.
   * @returns {Array<{level: string; label: string; color: string}>}
   */
  private get legends(): Array<{level: string, label: string, color: string}> {
    return Object.keys(this.options.keysWithColors).map(level => Object.assign({},{
      level,
      label: `levels.${level.toLowerCase()}`,
      color: this.options.keysWithColors[level]
    }));
  }

  private createHistogram(): void {
    if (this.host) {
      this.setup();
      this.buildSVG();
      this.populate();
    }
  }

  private setup(): void {
    const margin = this.options.margin,
      keysWithColors = this.options.keysWithColors,
      keys = Object.keys(keysWithColors),
      colors = keys.reduce((array: string[], key: string): string[] => [...array, keysWithColors[key]], []);
    this.width = this.htmlElement.clientWidth - margin.left - margin.right;
    this.xScale = d3.scaleTime().range([0, this.width]);
    this.yScale = d3.scaleLinear().range([this.options.height, 0]);
    this.colorScale = d3.scaleOrdinal(colors);
  }

  private buildSVG(): void {
    const margin = this.options.margin;
    this.host.html('');
    this.svg = this.host.append('svg').attr('id', this.svgId).attr('width', this.htmlElement.clientWidth)
      .attr('height', this.options.height + margin.top + margin.bottom).append('g')
      .attr('transform', `translate(${margin.left},${margin.top})`);
  }

  /**
   * It draws the svg representation of the x axis. The goal is to set the ticks here, add the axis to the svg element
   * and set the position of the axis.
   */
  private drawXAxis(): void {
    this.xAxis = d3.axisBottom(this.xScale)
      .tickFormat(tick => moment(tick).tz(this.timeZone).format(this.tickTimeFormat))
      .tickPadding(this.options.tickPadding);
    this.svg.append('g').attr('class', 'axis axis-x').attr('transform', `translate(0,${this.options.height})`).call(this.xAxis);
  }

  /**
   * It draws the svg representation of the y axis. The goal is to set the ticks here, add the axis to the svg element
   * and set the position of the axis.
   */
  private drawYAxis(): void {
    this.yAxis = d3.axisLeft(this.yScale).tickFormat((tick: number): string | undefined => {
      if (Number.isInteger(tick)) {
        return tick.toFixed(0);
      } else {
        return;
      }
    }).tickPadding(this.options.tickPadding);
    this.svg.append('g').attr('class', 'axis axis-y').call(this.yAxis).append('text');
  };

  /**
   * The goal is to handle the mouse over event on the rect svg elements so that we can populate the tooltip info object
   * and set the initial position of the tooltip. So we call the corresponding methods.
   * @param d The data for the currently "selected" bar
   * @param {number} index The index of the current element in the selection
   * @param elements The selection of the elements
   */
  private handleRectMouseOver = (d: any, index: number, elements: any):void => {
    this.setTooltipDataFromChartData(d);
    this.setTooltipPosition();
  };

  /**
   * The goal is to handle the movement of the mouse over the rect svg elements, so that we can set the position of
   * the tooltip by calling the @setTooltipPosition method.
   */
  private handleRectMouseMove = ():void => {
    this.setTooltipPosition();
  };

  /**
   * The goal is to reset the tooltipInfo object so that the tooltip will be hidden.
   */
  private handleRectMouseOut = ():void => {
    this.tooltipInfo = null;
  };

  /**
   * The goal is set the tooltip
   * @param d
   */
  private setTooltipDataFromChartData(d: {data: any, [key: string]: any}): void {
    let {timeStamp, ...data} = d.data;
    let levelColors = this.options.keysWithColors;
    this.tooltipInfo = {
      data: Object.keys(levelColors).map(key => Object.assign({}, {
        level: key,
        levelLabel: `levels.${key.toLowerCase()}`,
        value: data[key]
      })),
      timeStamp
    };
  }

  /**
   * The goal of this function is to set the tooltip position regarding the d3.mouse event relative to the @htmlElement.
   * Onlty if we have @tooltipInfo
   */
  private setTooltipPosition():void {
    if (this.tooltipInfo) {
      let tEl = this.tooltipElement;
      let pos = d3.mouse(this.htmlElement);
      let left = pos[0];
      let top = pos[1] - (tEl.offsetHeight / 2);
      let tooltipWidth = tEl.offsetWidth;
      let windowSize = window.innerWidth;
      if (left + tooltipWidth > windowSize) {
        left = pos[0] - (tooltipWidth + 25);
      }
      this.tooltipOnTheLeft = left < pos[0];
      this.tooltipPosition = {left, top};
    }
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
   * An example data: [{timeStamp: 1233455677, WARN: 12, ERROR: 123}]
   * @param {Array<{timeStamp: number; [p: string]: number}>} data
   */
  private setYScaleDomain(data: Array<{timeStamp: number, [key: string]: number}>): void {
    const keys = Object.keys(this.options.keysWithColors);
    const maxYValue = d3.max(data, item => keys.reduce((sum: number, key: string): number => sum + item[key], 0));
    this.yScale.domain([0, maxYValue]);
  }

  /**
   * Set the domain values for the x scale regarding the given data.
   * An example data: [{timeStamp: 1233455677, WARN: 12, ERROR: 123}]
   * @param {Array<{timeStamp: number; [p: string]: any}>} data
   */
  private setXScaleDomain(data: Array<{timeStamp: number, [key: string]: any}>): void {
    this.xScale.domain(d3.extent(data, item => item.timeStamp)).nice();
  }

  private populate(): void {
    const keys = Object.keys(this.options.keysWithColors);
    const data = this.data;
    const timeStamps = Object.keys(data);
    // we create a more consumable data structure for d3
    const formattedData = timeStamps.map((timeStamp: string): {timeStamp: number, [key: string]: number} => Object.assign({
        timeStamp: Number(timeStamp)
      }, data[timeStamp]));
    const layers = (d3.stack().keys(keys)(formattedData));

    // after we have the data we set the domain values both scales
    this.setXScaleDomain(formattedData);
    this.setYScaleDomain(formattedData);

    // Setting the timegap label above the chart
    this.setChartTimeGapByXScale();

    let unitD3TimeProp = this.chartTimeGap.unit.charAt(0).toUpperCase() + this.chartTimeGap.unit.slice(1);
    this.xScale.nice(d3[`time${unitD3TimeProp}`], 2);

    let columnWidth = this.options.columnWidth[this.chartTimeGap.unit] || this.options.columnWidth.base;

    // drawing the axis
    this.drawXAxis();
    this.drawYAxis();

    // populate the data and drawing the bars
    const layer = this.svg.selectAll('.value').data(d3.transpose<any>(layers))
                    .attr('class', 'value')
                  .enter().append('g')
                    .attr('class', 'value');
    layer.selectAll('.value rect').data(item => item)
        .attr('x', item => this.xScale(item.data.timeStamp) - columnWidth / 2)
        .attr('y', item => this.yScale(item[1]))
        .attr('height', item => this.yScale(item[0]) - this.yScale(item[1]))
        .attr('width', columnWidth.toString())
        .style('fill', (item, index) => this.colorScale(index))
      .enter().append('rect')
        .attr('x', item => this.xScale(item.data.timeStamp) - columnWidth / 2)
        .attr('y', item => this.yScale(item[1]))
        .attr('height', item => this.yScale(item[0]) - this.yScale(item[1]))
        .attr('width', columnWidth.toString())
        .style('fill', (item, index) => this.colorScale(index))
        .on('mouseover', this.handleRectMouseOver)
        .on('mousemove', this.handleRectMouseMove)
        .on('mouseout', this.handleRectMouseOut);
    this.setDragBehavior();
  }

  private getTimeRangeByXRanges(startX: number, endX:number): [number, number] {
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
    const height: number = this.options.height + this.options.margin.top + this.options.margin.bottom;
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
    let rightAreaWidth: number = this.width - right;
    rightAreaWidth = rightAreaWidth > 0 ? rightAreaWidth : 0;
    let leftAreaWidth: number = left > 0 ? left : 0;
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
    this.minDragX = this.options.margin.left;
    this.maxDragX = this.htmlElement.clientWidth;
    d3.selectAll(`svg#${this.svgId}`).call(d3.drag()
      .on('start', (datum: undefined, index: number, containers: ContainerElement[]): void => {
        if (this.dragArea) {
          this.dragArea.remove();
        }
        this.dragStartX = Math.max(0, this.getDragX(containers[0]) - this.options.margin.left);
        this.dragArea = this.svg.insert('rect', ':first-child').attr('x', this.dragStartX).attr('y', 0).attr('width', 0)
          .attr('height', this.options.height).attr('class', 'drag-area');
      })
      .on('drag', (datum: undefined, index: number, containers: ContainerElement[]): void => {
        const mousePos = this.getDragX(containers[0]);
        const currentX = Math.max(mousePos, this.minDragX) - this.options.margin.left;
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

  private getDragX(element: ContainerElement): number {
    return d3.mouse(element)[0];
  }

}
