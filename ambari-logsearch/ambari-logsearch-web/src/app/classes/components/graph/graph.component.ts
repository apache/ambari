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

import {
  AfterViewInit, OnChanges, SimpleChanges, ViewChild, ElementRef, Input, Output, EventEmitter, OnInit, OnDestroy
} from '@angular/core';
import * as d3 from 'd3';
import * as d3sc from 'd3-scale-chromatic';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/observable/fromEvent';
import 'rxjs/add/operator/debounceTime';
import {
GraphPositionOptions, GraphMarginOptions, GraphTooltipInfo, LegendItem, GraphEventData, GraphEmittedEvent
} from '@app/classes/graph';
import {HomogeneousObject} from '@app/classes/object';
import {ServiceInjector} from '@app/classes/service-injector';
import {UtilsService} from '@app/services/utils.service';
import {Subscription} from 'rxjs/Subscription';

export class GraphComponent implements AfterViewInit, OnChanges, OnInit, OnDestroy {

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
  height = 150;

  @Input()
  tickPadding = 10;

  @Input()
  colors: HomogeneousObject<string> = {};

  @Input()
  labels: HomogeneousObject<string> = {};

  /**
   * Indicates whether the graph represents dependency on time
   * @type {boolean}
   */
  @Input()
  isTimeGraph = false;

  /**
   * Indicates whether X axis direction is right to left
   * @type {boolean}
   */
  @Input()
  reverseXRange = false;

  /**
   * Indicates whether Y axis direction is top to bottom
   * @type {boolean}
   */
  @Input()
  reverseYRange = false;

  /**
   * Indicates whether X axis ticks with fractional values should be displayed on chart (if any)
   * @type {boolean}
   */
  @Input()
  allowFractionalXTicks = true;

  /**
   * Indicates whether Y axis ticks with fractional values should be displayed on chart (if any)
   * @type {boolean}
   */
  @Input()
  allowFractionalYTicks = true;

  /**
   * Indicated whether Y values equal to 0 should be skipped in tooltip
   * @type {boolean}
   */
  @Input()
  skipZeroValuesInTooltip = true;

  /**
   * Indicates whether X axis event should be emitted with formatted string values that are displayed
   * (instead of raw values)
   * @type {boolean}
   */
  @Input()
  emitFormattedXTick = false;

  /**
   * Indicates whether Y axis event should be emitted with formatted string values that are displayed
   * (instead of raw values)
   * @type {boolean}
   */
  @Input()
  emitFormattedYTick = false;

  @Output()
  xTickContextMenu: EventEmitter<GraphEmittedEvent<MouseEvent>> = new EventEmitter();

  @Output()
  yTickContextMenu: EventEmitter<GraphEmittedEvent<MouseEvent>> = new EventEmitter();

  @ViewChild('graphContainer')
  graphContainerRef: ElementRef;

  @ViewChild('tooltip', {
    read: ElementRef
  })
  tooltipRef: ElementRef;

  private readonly xAxisClassName = 'axis-x';

  private readonly yAxisClassName = 'axis-y';

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
   * This is the computed position of the tooltip relative to the @graphContainer which is the container of the graph.
   * It is set when the mousemoving over the figures in the @handleRectMouseMove method.
   */
  private tooltipPosition: GraphPositionOptions;

  /**
   * This property indicates if the tooltip should be positioned on the left side of the cursor or not.
   * It should be true when the tooltip is out from the window.
   * @type {boolean}
   */
  private tooltipOnTheLeft = false;

  protected subscriptions: Subscription[] = [];

  /**
   * This will return the information about the used levels and the connected colors and labels.
   * The goal is to provide an easy property to the template to display the legend of the levels.
   * @returns {LegendItem[]}
   */
  legendItems: LegendItem[];

  constructor() {
    this.utils = ServiceInjector.injector.get(UtilsService);
  }

  ngOnInit() {
    this.subscriptions.push(
      Observable.fromEvent(window, 'resize').debounceTime(100).subscribe(this.onWindowResize)
    );
    this.setLegendItems();
  }

  ngOnDestroy() {
    this.subscriptions.forEach((subscription: Subscription) => subscription.unsubscribe());
  }

  ngAfterViewInit() {
    this.graphContainer = this.graphContainerRef.nativeElement;
    this.tooltip = this.tooltipRef.nativeElement;
    this.host = d3.select(this.graphContainer);
    this.createGraph();
  }

  ngOnChanges(changes: SimpleChanges) {
    const dataChange = changes.data;
    if (dataChange && dataChange.currentValue && !this.utils.isEmptyObject(dataChange.currentValue)
      && (!dataChange.previousValue || this.utils.isEmptyObject(dataChange.previousValue))
      && this.utils.isEmptyObject(this.labels)) {
      this.setDefaultLabels();
    }
    if (changes.labels || changes.colors) {
      this.setLegendItems();
    }
    this.createGraph();
  }

  onWindowResize = () => {
    this.createGraph();
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
    const data = this.data;
    const keys = Object.keys(data);
    const labels = keys.reduce((keysReduced: HomogeneousObject<string>, dataKey: string): HomogeneousObject<string> => {
        const newKeys = Object.keys(data[dataKey]);
        const newKeysObj = newKeys.reduce((subKeys: HomogeneousObject<string>, key: string): HomogeneousObject<string> => {
            return Object.assign(subKeys, {
              [key]: key
            });
        }, {});
        return Object.assign(keysReduced, newKeysObj);
      }, {});
    this.labels = labels;
    this.setLegendItems();
  }

  protected setLegendItems(): void {
    if (this.colors && this.labels) {
      this.legendItems = Object.keys(this.labels).map((key: string) => Object.assign({}, {
        label: this.labels[key],
        color: this.colors[key]
      }));
    }
  }

  protected setup(): void {
    const margin = this.margin;
    if (this.utils.isEmptyObject(this.colors)) {
      // set default color scheme for different values if no custom colors specified
      const keys = Object.keys(this.labels);
      const keysCount = keys.length;
      const specterLength = keysCount > 2 ? keysCount : 3; // length of minimal available spectral scheme is 3
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
    this.width = this.graphContainer.clientWidth - margin.left - margin.right;
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

  /**
   * Set the domain values for the x scale regarding the given data.
   * @param formattedData
   */
  protected setXScaleDomain(formattedData?: any): void {}

  /**
   * Set the domain for the y scale regarding the given data.
   * @param formattedData
   */
  protected setYScaleDomain(formattedData?: any): void {}

  /**
   * It draws the svg representation of the x axis. The goal is to set the ticks here, add the axis to the svg element
   * and set the position of the axis.
   * @param {number} ticksCount - optional parameter which sets number of ticks explicitly
   * @param {number} leftOffset
   */
  protected drawXAxis(ticksCount?: number, leftOffset?: number): void {
    const axis = d3.axisBottom(this.xScale).tickFormat(this.xAxisTickFormatter).tickPadding(this.tickPadding);
    if (ticksCount) {
      axis.ticks(ticksCount);
    }
    this.xAxis = axis;
    this.svg.append('g').attr('class', `axis ${this.xAxisClassName}`)
      .attr('transform', `translate(${leftOffset || 0}, ${this.height})`)
      .call(this.xAxis);
    if (this.xTickContextMenu.observers.length) {
      this.svg.selectAll(`.${this.xAxisClassName} .tick`).on('contextmenu', (tickValue: any, index: number): void => {
        const tick = this.emitFormattedXTick ? this.xAxisTickFormatter(tickValue, index) : tickValue,
          nativeEvent = d3.event;
        this.xTickContextMenu.emit({tick, nativeEvent});
        event.preventDefault();
      });
    }
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
    this.svg.append('g').attr('class', `axis ${this.yAxisClassName}`).call(this.yAxis);
    if (this.yTickContextMenu.observers.length) {
      this.svg.selectAll(`.${this.yAxisClassName} .tick`).on('contextmenu', (tickValue: any, index: number): void => {
        const tick = this.emitFormattedYTick ? this.yAxisTickFormatter(tickValue, index) : tickValue,
          nativeEvent = d3.event;
        this.yTickContextMenu.emit({tick, nativeEvent});
        event.preventDefault();
      });
    }
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
    if (this.allowFractionalXTicks) {
      return tick.toString();
    } else {
      return Number.isInteger(tick) ? tick.toFixed(0) : undefined;
    }
  }

  /**
   * Function that formats the labels for Y axis ticks.
   * Returns simple toString() conversion as default, can be overridden in ancestors.
   * undefined value is returned for ticks to be skipped.
   * @param tick
   * @param {number} index
   * @returns {string|undefined}
   */
  protected yAxisTickFormatter = (tick: any, index: number): string | undefined => {
    if (this.allowFractionalYTicks) {
      return tick.toString();
    } else {
      return Number.isInteger(tick) ? tick.toFixed(0) : undefined;
    }
  }

  /**
   * The goal is to handle the mouse over event on the svg elements so that we can populate the tooltip info object
   * and set the initial position of the tooltip. So we call the corresponding methods.
   * @param {GraphEventData} d The data for the currently "selected" figure
   * @param {number} index The index of the current element in the selection
   * @param elements The selection of the elements
   */
  protected handleMouseOver = (d: GraphEventData, index: number, elements: HTMLElement[]): void => {
    this.setTooltipDataFromChartData(d);
    this.setTooltipPosition();
  }

  /**
   * The goal is to handle the movement of the mouse over the svg elements, so that we can set the position of
   * the tooltip by calling the @setTooltipPosition method.
   */
  protected handleMouseMove = (): void => {
    this.setTooltipPosition();
  }

  /**
   * The goal is to reset the tooltipInfo object so that the tooltip will be hidden.
   */
  protected handleMouseOut = (): void => {
    this.tooltipInfo = {};
  }

  /**
   * The goal is set the tooltip
   * @param {GraphEventData} d
   */
  protected setTooltipDataFromChartData(d: GraphEventData): void {
    const {tick, ...data} = d.data,
      levelColors = this.colors;
    let tooltipKeys = Object.keys(levelColors);
    if (this.skipZeroValuesInTooltip) {
      tooltipKeys = tooltipKeys.filter((key: string): boolean => data[key] > 0)
    }
    this.tooltipInfo = {
      data: tooltipKeys.map((key: string): object => Object.assign({}, {
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
