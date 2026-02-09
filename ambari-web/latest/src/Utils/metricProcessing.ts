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
import { HostMetricsData, HeatmapMetric } from '../types/heatmap';

// Constants aligned with Ember.js WidgetMixin
export const EXPRESSION_REGEX = /\$\{([\w\s\.\,\+\-\*\/\(\)\:\=\[\]]*)\}/g;
export const MATH_EXPRESSION_REGEX = /^[\d\s\+\-\*\/\(\)\.]+$/;
export const VALUE_NAME_REGEX = /[^(\s+\-\*\/\\+\s+)+](\w+\s+\w+)?[\w\.\,\-\:\=\[\]]*/g;

// Number utilities constants
export const BYTES_IN_MB = 1024 * 1024;

/**
 * Extract expressions from widget value
 * Aligned with Ember.js extractExpressions method
 */
export const extractExpressions = (input: any): string[] => {
  const expressions: string[] = [];
  let match;

  if (!input || !input.value) return expressions;

  const pattern = new RegExp(EXPRESSION_REGEX.source, EXPRESSION_REGEX.flags);
  while ((match = pattern.exec(input.value)) !== null) {
    expressions.push(match[1]);
  }
  return expressions;
};

/**
 * Compute expression with metrics data
 * Aligned with Ember.js computeExpression method from WidgetMixin
 */
export const computeExpression = (expressions: string[], metrics: HostMetricsData[]): Record<string, string> => {
  const result: Record<string, string> = {};

  expressions.forEach((_expression) => {
    let validExpression = true;
    let value = "";

    // Replace values with metrics data
    const beforeCompute = _expression.replace(VALUE_NAME_REGEX, (match) => {
      if (isNaN(Number(match))) {
        const metric = metrics.find(m => m.name === match);
        if (metric) {
          return metric.data.toString();
        } else {
          validExpression = false;
          return match;
        }
      } else {
        return match;
      }
    });

    // Check for correct math expression
    if (!(validExpression && MATH_EXPRESSION_REGEX.test(beforeCompute))) {
      validExpression = false;
    }

    result['${' + _expression + '}'] = validExpression ? Number(eval(beforeCompute)).toString() : value;
  });

  return result;
};

/**
 * Compute expression for heatmap widgets (host-to-value mapping)
 * Aligned with Ember.js computeExpression method from HeatmapWidgetView
 */
export const computeHeatmapExpression = (expressions: string[], metrics: HostMetricsData[]): Record<string, string | undefined> => {
  const hostToValueMap: Record<string, string | undefined> = {};
  const hostNames = [...new Set(metrics.map(m => m.hostName))];
  const metricsMap: Record<string, HostMetricsData> = {};


  // Convert metrics to MB when needed and create mapping
  metrics.forEach((_metric) => {
    convertDataWhenMB(_metric);
    // Key issue: We need to use the metric name that matches the expression
    // In Ember.js, the metric name in expression matches the 'name' field from widget definition
    const metricKey = _metric.name + "_" + _metric.hostName;
    metricsMap[metricKey] = _metric;
  });


  hostNames.forEach((_hostName) => {
    expressions.forEach((_expression) => {
      let validExpression = true;

      // Replace values with metrics data
      const beforeCompute = _expression.replace(VALUE_NAME_REGEX, (match) => {
        if (isNaN(Number(match))) {
          const metricKey = match + "_" + _hostName;
          const _metric = metricsMap[metricKey];
          
          if (_metric) {
            return _metric.data.toString();
          } else {
            validExpression = false;
            return match;
          }
        } else {
          return match;
        }
      });


      if (validExpression && MATH_EXPRESSION_REGEX.test(beforeCompute)) {
        const result = Number(eval(beforeCompute)).toString();
        hostToValueMap[_hostName] = result;
      } else if (beforeCompute.includes('undefined')) {
        // Data not available
        hostToValueMap[_hostName] = undefined;
      } else {
        hostToValueMap[_hostName] = undefined;
      }
    });
  });

  return hostToValueMap;
};

/**
 * Convert metric data from MB to bytes if needed
 * Aligned with Ember.js convertDataWhenMB method
 */
export const convertDataWhenMB = (_metric: HostMetricsData): void => {
  if (_metric.metric_path.endsWith('M') && _metric.data != null && isFinite(Number(_metric.data))) {
    _metric.originalData = Number(_metric.data);
    _metric.data = Math.round(Number(_metric.data) * BYTES_IN_MB);
  }
};

/**
 * Create heatmap metric object
 * Aligned with Ember.js MainChartHeatmapMetric
 */
export const createHeatmapMetric = (
  name: string,
  units: string = '',
  maximumValue: number = 100,
  hostNames: string[],
  hostToValueMap: Record<string, string | undefined>
): HeatmapMetric => {
  
  return {
    name,
    units,
    maximumValue,
    minimumValue: 0,
    hostNames,
    hostToValueMap,
    hostToSlotMap: calculateHostToSlotMap(hostToValueMap, hostNames, maximumValue, units),
    slotDefinitions: generateSlotDefinitions(maximumValue, units)
  };
};

/**
 * Calculate host to slot mapping
 * Aligned with Ember.js hostToSlotMap computed property
 */
export const calculateHostToSlotMap = (
  hostToValueMap: Record<string, string | undefined>,
  hostNames: string[],
  maximumValue: number,
  units: string = ''
): Record<string, number> => {
  const hostToSlotMap: Record<string, number> = {};
  const slotDefinitions = generateSlotDefinitions(maximumValue, units);


  if (hostToValueMap && hostNames) {
    hostNames.forEach((hostName) => {
      const slot = calculateSlot(hostToValueMap, hostName, slotDefinitions);
      if (slot > -1) {
        hostToSlotMap[hostName] = slot;
      }
    });
  }

  return hostToSlotMap;
};

/**
 * Calculate slot position for a host
 * Aligned with Ember.js calculateSlot method
 */
export const calculateSlot = (
  hostToValueMap: Record<string, string | undefined>,
  hostName: string,
  slotDefinitions: any[]
): number => {
  const slotWithBoundaries = slotDefinitions.filter(slot => slot.hasBoundaries);
  const invalidDataSlot = slotDefinitions.findIndex(slot => slot.invalidData);
  const notAvailableDataSlot = slotDefinitions.findIndex(slot => slot.notAvailable);
  let slot = slotDefinitions.findIndex(slot => slot.notApplicable);

  if (hostName in hostToValueMap) {
    let value = hostToValueMap[hostName];
    if (value == null) {
      slot = notAvailableDataSlot;
    } else if (isNaN(Number(value)) || !isFinite(Number(value)) || Number(value) < 0) {
      slot = invalidDataSlot;
    } else {
      const numValue = Number(value);
      slotWithBoundaries.forEach((slotDef, slotIndex, array) => {
        if ((numValue >= slotDef.from && numValue <= slotDef.to) ||
            // If value exceeds maximum then it pushed to the last/maximal slot
            (numValue > slotDef.to && slotIndex === array.length - 1)) {
          slot = slotIndex;
        }
      });
    }
  }

  return slot;
};

/**
 * Generate slot definitions for heatmap
 * Aligned with Ember.js slotDefinitions computed property
 */
export const generateSlotDefinitions = (maximumValue: number = 100, units: string = ''): any[] => {
  const numberOfSlots = 5;
  const slotColors = ['#1EB475', '#1FB418', '#E9E23F', '#E9A840', '#EF6162'];
  const minimumValue = 0;
  
  // Convert maximum value to internal units (bytes for MB) for slot calculation
  const maxForSlots = convertNumber(maximumValue, units);
  const delta = (maxForSlots - minimumValue) / numberOfSlots;
  const defs: any[] = [];


  // Generate regular slots
  for (let c = 0; c < numberOfSlots - 1; c++) {
    const slotColor = slotColors[c];
    const start = c * delta;
    const end = (c + 1) * delta;
    defs.push(generateSlot(start, end, units, slotColor));
  }

  // Last slot
  const lastSlotColor = slotColors[numberOfSlots - 1];
  const lastStart = (numberOfSlots - 1) * delta;
  defs.push(generateSlot(lastStart, maxForSlots, units, lastSlotColor));

  // Special slots
  defs.push({
    invalidData: true,
    index: defs.length,
    label: 'Invalid Data',
    cssStyle: getHatchStyle()
  });

  defs.push({
    notAvailable: true,
    index: defs.length,
    label: 'Data Not Available',
    cssStyle: "background-color: #666"
  });

  defs.push({
    notApplicable: true,
    index: defs.length,
    label: 'Not Applicable',
    cssStyle: "background-color:#ccc"
  });

  return defs;
};

/**
 * Generate individual slot
 * Aligned with Ember.js generateSlot method
 */
export const generateSlot = (min: number, max: number, units: string, slotColor: string): any => {
  // For display labels, convert back to original units
  const minForLabel = units === 'MB' ? min / BYTES_IN_MB : min;
  const maxForLabel = units === 'MB' ? max / BYTES_IN_MB : max;
  
  const fromLabel = formatLegendLabel(minForLabel, units);
  const toLabel = formatLegendLabel(maxForLabel, units);


  return {
    hasBoundaries: true,
    from: min,  // Keep internal values (bytes for MB)
    to: max,    // Keep internal values (bytes for MB)
    label: fromLabel + " - " + toLabel,
    cssStyle: "background-color:" + slotColor
  };
};

/**
 * Format legend label
 * Aligned with Ember.js formatLegendLabel method
 */
export const formatLegendLabel = (num: number, units: string): string => {
  const fraction = num % 1;
  if (num >= 100) {
    num = Math.round(num);
  } else if (num >= 10 && fraction > 0) {
    num = parseFloat(num.toFixed(1));
  } else if (fraction > 0) {
    num = parseFloat(num.toFixed(2));
  }
  
  if (units === 'ms') {
    return timingFormat(num);
  }
  return num + units;
};

/**
 * Convert milliseconds to human-readable format
 * Aligned with Ember.js timingFormat function
 */
export const timingFormat = (time: number): string => {
  if (time == null || isNaN(time)) {
    return '0s';
  }

  time = parseInt(time.toString());
  const fullTime = time;
  let duration = '';

  if (time === 0) {
    return '0s';
  }

  const oneSecMs = 1000;
  const oneMinMs = 60000;
  const oneHourMs = 3600000;
  const oneDayMs = 86400000;

  const extractTimeUnit = (time: number, unitValue: number, unitSuffix: string): [string, number] => {
    let result = '';
    if (time >= unitValue) {
      result = Math.floor(time / unitValue) + `${unitSuffix} `;
      time -= Math.floor(time / unitValue) * unitValue;
    }
    return [result, time];
  };

  let days, hours, minutes, seconds;
  [days, time] = extractTimeUnit(time, oneDayMs, 'd');
  [hours, time] = extractTimeUnit(time, oneHourMs, 'h');
  [minutes, time] = extractTimeUnit(time, oneMinMs, 'm');
  duration += days + hours + minutes;
  
  if (fullTime < oneDayMs) {
    [seconds, time] = extractTimeUnit(time, oneSecMs, 's');
    duration += seconds;
    if (fullTime < oneSecMs) {
      duration += '1s';
    }
  }

  return duration.trim();
};

/**
 * Convert number based on units
 * Aligned with Ember.js convertNumber method
 */
export const convertNumber = (number: number, units: string): number => {
  if (units === 'MB') {
    return Math.round(number * BYTES_IN_MB);
  }
  return number;
};

/**
 * Get hatch style for invalid data
 * Aligned with Ember.js getHatchStyle method
 */
export const getHatchStyle = (): string => {
  // Simplified version - could be enhanced with browser detection
  return "background-image:repeating-linear-gradient(-45deg, #666, #666 6px, #fff 6px, #fff 7px)";
};

/**
 * Convert internal value back to display value with proper units
 * Reverse of convertNumber function
 */
export const convertToDisplayValue = (value: string | number, units: string): string => {
  const numValue = Number(value);
  
  if (units === 'MB' && numValue > 1000000) {
    // Convert bytes back to MB for display
    const mbValue = numValue / BYTES_IN_MB;
    return formatLegendLabel(mbValue, units);
  }
  
  // For percentage and other units, format as-is
  return formatLegendLabel(numValue, units);
};

/**
 * Calculate host percentage metrics
 * Aligned with Ember.js host percentage calculations
 */
export const calculateHostPercentages = (host: any) => {
  return {
    // Disk Usage % = ((Total - Free) / Total) * 100
    diskUsagePercent: host.diskTotal > 0 
      ? Math.round(((host.diskTotal - host.diskFree) / host.diskTotal) * 100 * 100) / 100
      : 0,
    
    // Memory Usage % = ((Total - Free) / Total) * 100
    // Note: Ember.js sometimes includes cached memory, but basic formula is without cached
    memoryUsagePercent: host.memTotal > 0 
      ? Math.round(((host.memTotal - host.memFree) / host.memTotal) * 100 * 100) / 100
      : 0,
    
    // CPU Usage % = cpu_user + cpu_system (already in percentage)
    cpuUsagePercent: Math.round((host.cpuUser + host.cpuSystem) * 100) / 100,
    
    // Individual metrics for reference
    diskFreePercent: host.diskTotal > 0 
      ? Math.round((host.diskFree / host.diskTotal) * 100 * 100) / 100
      : 0,
    memoryFreePercent: host.memTotal > 0 
      ? Math.round((host.memFree / host.memTotal) * 100 * 100) / 100
      : 0,
    cpuWaitIOPercent: Math.round(host.cpuWio * 100) / 100 || 0
  };
};

/**
 * Create host percentage heatmap widgets (aligned with Ember.js SYSTEM_HEATMAPS)
 */
export const createHostPercentageWidgets = () => {
  return [
    {
      id: 'host_disk_used',
      widget_name: 'Host Disk Usage %',
      widget_type: 'HEATMAP',
      scope: 'CLUSTER',
      author: 'ui2',
      metrics: [
        { name: 'disk_free', metric_path: 'metrics/disk/disk_free', service_name: 'STACK' },
        { name: 'disk_total', metric_path: 'metrics/disk/disk_total', service_name: 'STACK' }
      ],
      values: [{ name: 'Host Disk Usage %', value: '${((disk_total-disk_free)/disk_total)*100}' }],
      properties: { display_unit: '%', max_limit: '100' }
    },
    {
      id: 'host_memory_used',
      widget_name: 'Host Memory Usage %',
      widget_type: 'HEATMAP',
      scope: 'CLUSTER',
      author: 'ui2',
      metrics: [
        { name: 'mem_total', metric_path: 'metrics/memory/mem_total', service_name: 'STACK' },
        { name: 'mem_free', metric_path: 'metrics/memory/mem_free', service_name: 'STACK' }
      ],
      values: [{ name: 'Host Memory Usage %', value: '${((mem_total-mem_free)/mem_total)*100}' }],
      properties: { display_unit: '%', max_limit: '100' }
    },
    {
      id: 'host_cpu_usage',
      widget_name: 'Host CPU Usage %',
      widget_type: 'HEATMAP',
      scope: 'CLUSTER',
      author: 'ui2',
      metrics: [
        { name: 'cpu_user', metric_path: 'metrics/cpu/cpu_user', service_name: 'STACK' },
        { name: 'cpu_system', metric_path: 'metrics/cpu/cpu_system', service_name: 'STACK' }
      ],
      values: [{ name: 'Host CPU Usage %', value: '${cpu_user+cpu_system}' }],
      properties: { display_unit: '%', max_limit: '100' }
    },
    {
      id: 'host_cpu_wait_io',
      widget_name: 'Host CPU Wait I/O %',
      widget_type: 'HEATMAP',
      scope: 'CLUSTER',
      author: 'ui2',
      metrics: [
        { name: 'cpu_wio', metric_path: 'metrics/cpu/cpu_wio', service_name: 'STACK' }
      ],
      values: [{ name: 'Host CPU Wait I/O %', value: '${cpu_wio}' }],
      properties: { display_unit: '%', max_limit: '100' }
    }
  ];
};
