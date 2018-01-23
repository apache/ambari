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

export interface GraphPositionOptions {
  top: number;
  left: number;
}

export interface GraphMarginOptions extends GraphPositionOptions {
  right: number;
  bottom: number;
}

export interface GraphTooltipInfo {
  data: object[];
  title: string | number;
}

export interface LegendItem {
  label: string;
  color: string;
}

export interface GraphScaleItem {
  tick: number;
  [key: string]: number;
}

export interface ChartTimeGap {
  value: number;
  unit: string;
  label: string;
}

export interface GraphEventData extends Array<number> {
  data: GraphScaleItem;
}

export type GraphLinePoint = GraphScaleItem & {
  color: string;
}

export interface GraphLineData {
  points: GraphScaleItem[];
  key: string;
}

export interface GraphEmittedEvent<EventType> {
  tick: any;
  nativeEvent: EventType;
}
