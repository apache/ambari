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

import {Component, OnInit, AfterViewInit, OnChanges, Input, ViewChild, ElementRef} from '@angular/core';
import * as d3 from 'd3';
import * as moment from 'moment-timezone';
import {AppSettingsService} from '@app/services/storage/app-settings.service';

@Component({
  selector: 'time-histogram',
  templateUrl: './time-histogram.component.html',
  styleUrls: ['./time-histogram.component.less']
})
export class TimeHistogramComponent implements OnInit, AfterViewInit, OnChanges {

  constructor(private appSettings: AppSettingsService) {
    this.appSettings.getParameter('timeZone').subscribe(value => {
      this.timeZone = value;
      this.createHistogram();
    });
  }

  ngOnInit() {
    Object.assign(this.options, this.defaultOptions, this.customOptions);
  }

  ngAfterViewInit() {
    this.htmlElement = this.element.nativeElement;
    this.host = d3.select(this.htmlElement);
  }

  ngOnChanges() {
    this.createHistogram();
  }

  @ViewChild('container')
  element: ElementRef;

  @Input()
  customOptions: any;

  @Input()
  data: any;

  private readonly defaultOptions = {
    margin: {
      top: 20,
      right: 20,
      bottom: 40,
      left: 50
    },
    height: 200,
    tickPadding: 10,
    columnWidth: 20
  };

  private options: any = {};

  private timeZone: string;

  private host;

  private svg;

  private width;

  private xScale;

  private yScale;

  private colorScale;

  private xAxis;

  private yAxis;

  private htmlElement: HTMLElement;

  histogram: any;

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
      colors = keys.reduce((array, key) => [...array, keysWithColors[key]], []);
    this.width = this.htmlElement.clientWidth - margin.left - margin.right;
    this.xScale = d3.scaleTime().range([0, this.width]);
    this.yScale = d3.scaleLinear().range([this.options.height, 0]);
    this.colorScale = d3.scaleOrdinal(colors);
  }

  private buildSVG(): void {
    const margin = this.options.margin;
    this.host.html('');
    this.svg = this.host.append('svg').attr('width', this.width + margin.left + margin.right)
      .attr('height', this.options.height + margin.top + margin.bottom).append('g')
      .attr('transform', `translate(${margin.left},${margin.top})`);
  }

  private drawXAxis(): void {
    this.xAxis = d3.axisBottom(this.xScale)
      .tickFormat(tick => moment(tick).tz(this.timeZone).format('MM/DD HH:mm'))
      .tickPadding(this.options.tickPadding);
    this.svg.append('g').attr('class', 'axis').attr('transform', `translate(0,${this.options.height})`).call(this.xAxis);
  }

  private drawYAxis(): void {
    this.yAxis = d3.axisLeft(this.yScale).tickFormat((tick: number) => {
      if (Number.isInteger(tick)) {
        return tick.toFixed(0);
      } else {
        return;
      }
    }).tickPadding(this.options.tickPadding);
    this.svg.append('g').attr('class', 'axis').call(this.yAxis).append('text');
  }

  private populate(): void {
    const keys = Object.keys(this.options.keysWithColors),
      data = this.data,
      timeStamps = Object.keys(data),
      formattedData = timeStamps.map(timeStamp => Object.assign({
        timeStamp: timeStamp
      }, data[timeStamp])),
      layers = (d3.stack().keys(keys)(formattedData)),
      columnWidth = this.options.columnWidth;
    this.xScale.domain(d3.extent(formattedData, item => item.timeStamp));
    this.yScale.domain([0, d3.max(formattedData, item => keys.reduce((sum, key) => sum + item[key], 0))]);
    this.drawXAxis();
    this.drawYAxis();
    const layer = this.svg.selectAll().data(d3.transpose<any>(layers)).enter().append('g');
    layer.selectAll().data(item => item).enter().append('rect')
      .attr('x', item => this.xScale(item.data.timeStamp) - columnWidth / 2).attr('y', item => this.yScale(item[1]))
      .attr('height', item => this.yScale(item[0]) - this.yScale(item[1])).attr('width', columnWidth.toString())
      .style('fill', (item, index) => this.colorScale(index));
  }

}
