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

import {AfterViewInit, OnChanges, SimpleChanges, ViewChild, ElementRef, Input} from '@angular/core';
import * as d3 from 'd3';
import * as d3sc from 'd3-scale-chromatic';
import {GraphPositionOptions, GraphMarginOptions, GraphTooltipInfo, LegendItem} from '@app/classes/graph';
import {HomogeneousObject} from '@app/classes/object';
import {ServiceInjector} from '@app/classes/service-injector';
import {UtilsService} from '@app/services/utils.service';

export class GraphComponent implements AfterViewInit, OnChanges {

  constructor() {
    this.utils = ServiceInjector.injector.get(UtilsService);
  }

  ngAfterViewInit() {
    this.graphContainer = this.graphContainerRef.nativeElement;
    this.tooltip = this.tooltipRef.nativeElement;
    this.host = d3.select(this.graphContainer);
  }

  ngOnChanges(changes: SimpleChanges) {
    const dataChange = changes.data;
    if (dataChange && dataChange.currentValue && !this.utils.isEmptyObject(dataChange.currentValue)
      && (!dataChange.previousValue || this.utils.isEmptyObject(dataChange.previousValue))
      && this.utils.isEmptyObject(this.labels)) {
      this.setDefaultLabels();
    }
    this.createGraph();
  }

  @Input()
  data: HomogeneousObject<HomogeneousObject<number>> = {};

  @Input()
  svgId: string = 'graph-svg';

  @Input()
  margin: GraphMarginOptions = {
    top: 5,
    right: 50,
    bottom: 30,
    left: 50
  };

  @Input()
  width: number;

  @Input()
  height: number = 150;

  @Input()
  tickPadding: number = 10;

  @Input()
  colors: HomogeneousObject<string> = {};

  @Input()
  labels: HomogeneousObject<string> = {};

  /**
   * Indicates whether the graph represents dependency on time
   * @type {boolean}
   */
  @Input()
  isTimeGraph: boolean = false;

  /**
   * Indicates whether X axis direction is right to left
   * @type {boolean}
   */
  @Input()
  reverseXRange: boolean = false;

  /**
   * Indicates whether Y axis direction is top to bottom
   * @type {boolean}
   */
  @Input()
  reverseYRange: boolean = false;

  @ViewChild('graphContainer')
  graphContainerRef: ElementRef;

  @ViewChild('tooltip', {
    read: ElementRef
  })
  tooltipRef: ElementRef;

  protected utils: UtilsService;

  protected graphContainer: HTMLElement;

  private tooltip: HTMLElement;

  protected host;

  protected svg;

  protected xScale;

  protected yScale;

  protected xAxis;

  protected yAxis;

  /**
   * Ordered array of color strings for data representation
   * @type {string[]}
   */
  protected orderedColors: string[];

  /**
   * This property is to hold the data of the bar where the mouse is over.
   */
  protected tooltipInfo: GraphTooltipInfo | {} = {};

  /**
   * This is the computed position of the tooltip relative to the @graphContainer which is the container of the histogram.
   * It is set when the mousemoving over the bars in the @handleRectMouseMove method.
   */
  private tooltipPosition: GraphPositionOptions;

  /**
   * This property indicates if the tooltip should be positioned on the left side of the cursor or not.
   * It should be true when the tooltip is out from the window.
   * @type {boolean}
   */
  private tooltipOnTheLeft: boolean = false;

  /**
   * This will return the information about the used levels and the connected colors and labels.
   * The goal is to provide an easy property to the template to display the legend of the levels.
   * @returns {LegendItem[]}
   */
  get legendItems(): LegendItem[] {
    return Object.keys(this.labels).map((key: string) => Object.assign({}, {
      label: this.labels[key],
      color: this.colors[key]
    }));
  }

  protected createGraph(): void {
    if (this.host && !this.utils.isEmptyObject(this.labels)) {
      this.setup();
      this.buildSVG();
      this.populate();
    }
  }

  /**
   * Method that sets default labels map object based on data if no custom one is specified
   */
  protected setDefaultLabels() {
    const data = this.data,
      keys = Object.keys(data),
      labels = keys.reduce((keys: HomogeneousObject<string>, dataKey: string): HomogeneousObject<string> => {
        const newKeys = Object.keys(data[dataKey]),
          newKeysObj = newKeys.reduce((subKeys: HomogeneousObject<string>, key: string): HomogeneousObject<string> => {
            return Object.assign(subKeys, {
              [key]: key
            });
        }, {});
        return Object.assign(keys, newKeysObj);
      }, {});
    this.labels = labels;
  }

  protected setup(): void {
    const margin = this.margin;
    if (this.utils.isEmptyObject(this.colors)) {
      // set default color scheme for different values if no custom colors specified
      const keys = Object.keys(this.labels),
        keysCount = keys.length,
        specterLength = keysCount > 2 ? keysCount : 3; // length of minimal available spectral scheme is 3
      let colorsArray;
      if (keysCount > 2) {
        colorsArray = Array.from(d3sc.schemeSpectral[keysCount]);
      } else {
        const minimalColorScheme = Array.from(d3sc.schemeSpectral[specterLength]);
        colorsArray = minimalColorScheme.slice(0, keysCount);
      }
      this.orderedColors = colorsArray;
      this.colors = keys.reduce((currentObject: HomogeneousObject<string>, currentKey: string, index: number) => {
        return Object.assign(currentObject, {
          [currentKey]: colorsArray[index]
        });
      }, {});
    } else {
      const keysWithColors = this.colors,
        keys = Object.keys(keysWithColors);
      this.orderedColors = keys.reduce((array: string[], key: string): string[] => [...array, keysWithColors[key]], []);
    }
    if (!this.width) {
      this.width = this.graphContainer.clientWidth - margin.left - margin.right;
    }
    const xScale = this.isTimeGraph ? d3.scaleTime() : d3.scaleLinear();
    const yScale = d3.scaleLinear();
    const xScaleWithRange = this.reverseXRange ? xScale.range([this.width, 0]) : xScale.range([0, this.width]);
    const yScaleWithRange = this.reverseYRange ? yScale.range([0, this.height]) : yScale.range([this.height, 0]);
    this.xScale = xScaleWithRange;
    this.yScale = yScaleWithRange;
  }

  protected buildSVG(): void {
    const margin = this.margin;
    this.host.html('');
    this.svg = this.host.append('svg').attr('id', this.svgId).attr('width', this.graphContainer.clientWidth)
      .attr('height', this.height + margin.top + margin.bottom).append('g')
      .attr('transform', `translate(${margin.left},${margin.top})`);
  }

  protected populate(): void {}

  protected setXScaleDomain(formattedData?: any): void {}

  protected setYScaleDomain(formattedData?: any): void {}

  /**
   * It draws the svg representation of the x axis. The goal is to set the ticks here, add the axis to the svg element
   * and set the position of the axis.
   * @param {number} ticksCount - optional parameter which sets number of ticks explicitly
   */
  protected drawXAxis(ticksCount?: number): void {
    const axis = d3.axisBottom(this.xScale).tickFormat(this.xAxisTickFormatter).tickPadding(this.tickPadding);
    if (ticksCount) {
      axis.ticks(ticksCount);
    }
    this.xAxis = axis;
    this.svg.append('g').attr('class', 'axis axis-x').attr('transform', `translate(0,${this.height})`).call(this.xAxis);
  }

  /**
   * It draws the svg representation of the y axis. The goal is to set the ticks here, add the axis to the svg element
   * and set the position of the axis.
   * @param {number} ticksCount - optional parameter which sets number of ticks explicitly
   */
  protected drawYAxis(ticksCount?: number): void {
    const axis = d3.axisLeft(this.yScale).tickFormat(this.yAxisTickFormatter).tickPadding(this.tickPadding);
    if (ticksCount) {
      axis.ticks(ticksCount);
    }
    this.yAxis = axis;
    this.svg.append('g').attr('class', 'axis axis-y').call(this.yAxis).append('text');
  };

  /**
   * Function that formats the labels for X axis ticks.
   * Returns simple toString() conversion as default, can be overridden in ancestors.
   * undefined value is returned for ticks to be skipped.
   * @param tick
   * @param {number} index
   * @returns {string|undefined}
   */
  protected xAxisTickFormatter = (tick: any, index: number): string | undefined => {
    return tick.toString();
  };

  /**
   * Function that formats the labels for Y axis ticks.
   * Returns simple toString() conversion as default, can be overridden in ancestors.
   * undefined value is returned for ticks to be skipped.
   * @param tick
   * @param {number} index
   * @returns {string|undefined}
   */
  protected yAxisTickFormatter = (tick: any, index: number): string | undefined => {
    return tick.toString();
  };

  /**
   * The goal is to handle the mouse over event on the rect svg elements so that we can populate the tooltip info object
   * and set the initial position of the tooltip. So we call the corresponding methods.
   * @param d The data for the currently "selected" bar
   * @param {number} index The index of the current element in the selection
   * @param elements The selection of the elements
   */
  protected handleRectMouseOver = (d: {data: any, [key: string]: any}, index: number, elements: any): void => {
    this.setTooltipDataFromChartData(d);
    this.setTooltipPosition();
  };

  /**
   * The goal is to handle the movement of the mouse over the rect svg elements, so that we can set the position of
   * the tooltip by calling the @setTooltipPosition method.
   */
  protected handleRectMouseMove = (): void => {
    this.setTooltipPosition();
  };

  /**
   * The goal is to reset the tooltipInfo object so that the tooltip will be hidden.
   */
  protected handleRectMouseOut = (): void => {
    this.tooltipInfo = {};
  };

  /**
   * The goal is set the tooltip
   * @param d
   */
  protected setTooltipDataFromChartData(d: {data: any, [key: string]: any}): void {
    let {tick, ...data} = d.data;
    let levelColors = this.colors;
    this.tooltipInfo = {
      data: Object.keys(levelColors).filter((key: string): boolean => data[key] > 0).map((key: string): object => Object.assign({}, {
        color: this.colors[key],
        label: this.labels[key],
        value: data[key]
      })),
      title: tick
    };
  }

  /**
   * The goal of this function is to set the tooltip position regarding the d3.mouse event relative to the @graphContainer.
   * Only if we have @tooltipInfo
   */
  protected setTooltipPosition(): void {
    if (this.tooltipInfo.hasOwnProperty('data')) {
      const tooltip = this.tooltip,
        relativeMousePosition = d3.mouse(this.graphContainer),
        absoluteMousePosition = d3.mouse(document.body),
        absoluteMouseLeft = absoluteMousePosition[0],
        top = relativeMousePosition[1] - (tooltip.offsetHeight / 2),
        tooltipWidth = tooltip.offsetWidth,
        windowSize = window.innerWidth;
      let left = relativeMousePosition[0];
      if (absoluteMouseLeft + tooltipWidth > windowSize) {
        left = relativeMousePosition[0] - (tooltipWidth + 25);
      }
      this.tooltipOnTheLeft = left < relativeMousePosition[0];
      this.tooltipPosition = {left, top};
    }
  };

}
