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

export const convertMapTable = {
  size: {
    b: 1024,
    kb: 1024,
    mb: 1024,
    gb: 1024,
    tb: 1024,
    pb: 1024
  },
  time: {
    milliseconds: 1,
    seconds: 1000,
    minutes: 60,
    hours: 60,
    days: 24
  },
  percent: {
    percent_int: {
      int: 1,
      percent: 1
    },
    percent_float: {
      float: 1,
      percent: 0.01
    }
  },
  dimensionless: {
    int: 1,
    float: 1
  }
};

// Unit label mapping for display
export const unitLabelMap: Record<string, string> = {
  percent: '%',
  int: '',
  float: '',
  b: 'B',
  kb: 'KB',
  mb: 'MB',
  gb: 'GB',
  tb: 'TB',
  pb: 'PB',
  milliseconds: 'ms',
  seconds: 's',
  minutes: 'min',
  hours: 'h',
  days: 'd'
};

// Data type units that should not display units
const DATA_TYPES = ['int', 'float'];

/**
 * Check if a unit is a data type (int/float)
 */
export function isDataType(unit: string): boolean {
  return DATA_TYPES.includes(unit?.toLowerCase());
}

/**
 * Get dimension table map for unit conversion
 */
function getUnitTable(
  unit: string,
  currentDimensionType?: string,
): Record<string, number> {
  if (currentDimensionType) {
    let table: unknown = convertMapTable;
    for (const segment of currentDimensionType.split(".")) {
      if (typeof table !== "object" || table === null || !(segment in table)) {
        table = undefined;
        break;
      }
      table = (table as Record<string, unknown>)[segment];
    }
    if (
      typeof table === "object" &&
      table !== null &&
      Object.values(table).every((value) => typeof value === "number")
    ) {
      return table as Record<string, number>;
    }
  }

  const unitType = Object.keys(convertMapTable).find((item) => {
    const table = convertMapTable[item as keyof typeof convertMapTable];
    return Object.keys(table).includes(unit?.toLowerCase());
  });
  const table = unitType
    ? convertMapTable[unitType as keyof typeof convertMapTable]
    : convertMapTable.dimensionless;
  return Object.values(table).every((value) => typeof value === "number")
    ? (table as Record<string, number>)
    : convertMapTable.dimensionless;
}

/**
 * Convert single value between units
 */
function convertToSingleValue(value: number, fromUnit: string, toUnit: string, currentDimensionType?: string): number {
  const convertTable = getUnitTable(fromUnit, currentDimensionType);
  const units = Object.keys(convertTable);
  const fromUnitIndex = units.indexOf(fromUnit.toLowerCase());
  const toUnitIndex = units.indexOf(toUnit.toLowerCase());
  
  if (fromUnitIndex === -1) {
    console.warn(`Invalid value unit type: ${fromUnit}`);
    return value;
  }
  
  if (toUnitIndex === -1) {
    console.warn(`Invalid desired unit type: ${toUnit}`);
    return value;
  }
  
  if (fromUnitIndex === toUnitIndex) {
    return value;
  }
  
  const range = [fromUnitIndex + 1, toUnitIndex + 1];
  const processedUnits = units.slice(...(range[0] < range[1] ? range : range.slice().reverse()));
  
  const factor = processedUnits.reduce((p, unit) => p * convertTable[unit], 1);
  
  if (range[0] < range[1]) {
    value /= factor;
  } else {
    value *= factor;
  }
  
  return Number.isInteger(value) ? value : parseFloat(value.toFixed(3));
}

/**
 * Main conversion function - converts value between different unit types
 */
export function convertValue(
  value: number | number[],
  fromUnit: string | string[],
  toUnit: string | string[],
  isObjectOutput: boolean = false,
  currentDimensionType?: string
): number | Array<{value: number, type: string}> {
  const valueIsArray = Array.isArray(value);
  
  if (!valueIsArray) {
    value = +value;
  }
  
  // If units are the same and no object output requested, return as is
  if ((fromUnit === toUnit || !toUnit) && !isObjectOutput) {
    return value as number;
  }
  
  if (isNaN(value as number) && !valueIsArray) {
    return 0;
  }
  
  // Convert toUnit to array if it's a string
  const toUnitArray = Array.isArray(toUnit) ? toUnit : toUnit.split(',');
  
  // Process multi unit format or object output
  if (toUnitArray.length > 1 || isObjectOutput) {
    let remainingValue = value as number;
    return toUnitArray.map(unit => {
      const convertedValue = Math.floor(convertToSingleValue(remainingValue, fromUnit as string, unit, currentDimensionType));
      remainingValue -= convertToSingleValue(convertedValue, unit, fromUnit as string, currentDimensionType);
      return {
        value: convertedValue,
        type: unit
      };
    });
  } else {
    // Single unit format
    if (!valueIsArray) {
      return convertToSingleValue(value as number, fromUnit as string, toUnitArray[0], currentDimensionType);
    } else {
      // Handle array input - sum all values after converting to target unit
      if (Array.isArray(value) && value.length > 0 && typeof value[0] === 'object' && value[0] !== null && 'value' in value[0]) {
        const arrayValue = value as unknown as Array<{value: number, type: string}>;
        return arrayValue
          .map(item => convertToSingleValue(item.value, item.type, toUnitArray[0], currentDimensionType))
          .reduce((sum, val) => sum + val, 0);
      } else {
        // Fallback for simple number arrays or invalid input
        return 0;
      }
    }
  }
}

/**
 * Get unit label for display
 */
export function getUnitLabel(unit: string): string {
  return unitLabelMap[unit?.toLowerCase()] || unit || '';
}

/**
 * Check if unit should be displayed (hide for int/float)
 */
export function shouldDisplayUnit(unit: string): boolean {
  return Boolean(unit) && !isDataType(unit) && unit.toLowerCase() !== 'dimensionless';
}

/**
 * Get display unit label (empty for int/float)
 */
export function getDisplayUnitLabel(unit: string): string {
  return shouldDisplayUnit(unit) ? getUnitLabel(unit) : '';
}

/**
 * Get config unit information from property
 */
export function getConfigUnitInfo(property: any): {
  configUnit: string;
  widgetUnit: string;
  dimensionType: string;
  configType: string;
} {
  // Get config unit from property attributes
  const configUnit = property?.unit || property?.propertyAttributes?.unit || property?.propertyAttributes?.type || 'int';
  
  // Get widget unit from widget definition or fall back to config unit
  const widgetUnit = property?.widget?.units?.[0]?.['unit-name'] || 
                     property?.widget?.units?.[0]?.unit || 
                     configUnit;
  
  // Determine dimension type
  let dimensionType = '';
  if (['int', 'float'].includes(configUnit.toLowerCase()) && widgetUnit.toLowerCase() === 'percent') {
    dimensionType = `percent.percent_${configUnit.toLowerCase()}`;
  } else {
    // Auto-detect dimension type
    dimensionType = Object.keys(convertMapTable).find(type => {
      const table = convertMapTable[type as keyof typeof convertMapTable];
      return Object.keys(table).includes(widgetUnit.toLowerCase());
    }) || 'dimensionless';
  }
  
  return {
    configUnit: configUnit.toLowerCase(),
    widgetUnit: widgetUnit.toLowerCase(),
    dimensionType,
    configType: property?.propertyAttributes?.type || 'int'
  };
}

/**
 * Convert config value to widget value
 */
export function widgetValueByConfigAttributes(
  value: number,
  configUnit: string,
  widgetUnit: string,
  dimensionType?: string
): number {
  if (!value || configUnit === widgetUnit) return value;
  return convertValue(value, configUnit, widgetUnit, false, dimensionType) as number;
}

/**
 * Convert widget value back to config value
 */
export function configValueByWidget(
  value: number,
  widgetUnit: string,
  configUnit: string,
  configType: string,
  dimensionType?: string
): number {
  if (!value || widgetUnit === configUnit) return value;
  const converted = convertValue(value, widgetUnit, configUnit, false, dimensionType) as number;
  return configType === 'int' ? Math.round(converted) : converted;
}

/**
 * Format tick label for sliders (matches Ember formatTickLabel exactly)
 */
export function formatTickLabel(tick: number, unit: string, separator: string = ''): string {
  if (!shouldDisplayUnit(unit)) {
    return tick.toString();
  }
  
  let valueLabel = tick;
  let unitLabel = getUnitLabel(unit);
  
  // Handle size units with auto-scaling (matches Ember logic exactly)
  const sizeUnits = ['B', 'KB', 'MB', 'GB', 'TB'];
  const unitLabelIndex = sizeUnits.indexOf(unitLabel);
  
  if (unitLabelIndex > -1) {
    let scaledTick = tick;
    let scaledUnitIndex = unitLabelIndex;
    

    while (scaledTick >= 1024 && scaledUnitIndex < sizeUnits.length - 1) {
      scaledTick /= 1024;
      scaledUnitIndex++;
    }
    
    unitLabel = sizeUnits[scaledUnitIndex];
    valueLabel = parseFloat(scaledTick.toFixed(3));
  }
  
  return valueLabel + separator + unitLabel;
}

/**
 * Round number to 3 decimal places
 */
export function extraRound(value: number): number {
  return parseFloat(value.toFixed(3));
}

/**
 * Parse time interval from config value to appropriate time units (days, hours, minutes, seconds)
 */
export function parseTimeInterval(value: number, configUnit: string): {
  days: number;
  hours: number;
  minutes: number;
  seconds: number;
  milliseconds: number;
} {
  // Convert to milliseconds first
  const milliseconds = convertValue(value, configUnit, 'milliseconds') as number;
  
  const totalSeconds = Math.floor(milliseconds / 1000);
  const days = Math.floor(totalSeconds / (24 * 60 * 60));
  const hours = Math.floor((totalSeconds % (24 * 60 * 60)) / (60 * 60));
  const minutes = Math.floor((totalSeconds % (60 * 60)) / 60);
  const seconds = totalSeconds % 60;
  const remainingMilliseconds = Math.round(milliseconds - totalSeconds * 1000);
  
  return {
    days,
    hours,
    minutes,
    seconds,
    milliseconds: remainingMilliseconds,
  };
}

/**
 * Compose time interval from time components back to config unit
 */
export function composeTimeInterval(
  days: number,
  hours: number,
  minutes: number,
  seconds: number,
  configUnit: string,
  milliseconds = 0,
): number {
  const totalMilliseconds =
    ((days * 24 * 60 * 60) + (hours * 60 * 60) + (minutes * 60) + seconds) *
      1000 +
    milliseconds;
  return convertValue(totalMilliseconds, 'milliseconds', configUnit) as number;
}

export const TIME_INTERVAL_UNITS = [
  'days',
  'hours',
  'minutes',
  'seconds',
  'milliseconds',
] as const;

export type TimeIntervalUnit = (typeof TIME_INTERVAL_UNITS)[number];

const timeIntervalMilliseconds: Record<TimeIntervalUnit, number> = {
  days: 24 * 60 * 60 * 1000,
  hours: 60 * 60 * 1000,
  minutes: 60 * 1000,
  seconds: 1000,
  milliseconds: 1,
};

export const timeIntervalPartMaximum: Record<TimeIntervalUnit, number> = {
  days: 365,
  hours: 23,
  minutes: 59,
  seconds: 59,
  milliseconds: 999,
};

export function normalizeTimeIntervalUnits(value: unknown): TimeIntervalUnit[] {
  const units = String(value ?? '')
    .split(',')
    .map((unit) => unit.trim().toLowerCase())
    .filter((unit): unit is TimeIntervalUnit =>
      TIME_INTERVAL_UNITS.includes(unit as TimeIntervalUnit),
    );
  return units.filter((unit, index) => units.indexOf(unit) === index);
}

function timeValueToMilliseconds(value: number, unit: string): number {
  const factor = timeIntervalMilliseconds[unit.toLowerCase() as TimeIntervalUnit];
  return factor === undefined ? Number.NaN : value * factor;
}

export function decomposeTimeInterval(
  value: number,
  configUnit: string,
  units: TimeIntervalUnit[],
): Record<TimeIntervalUnit, number> {
  let remaining = timeValueToMilliseconds(value, configUnit);
  const result = Object.fromEntries(
    TIME_INTERVAL_UNITS.map((unit) => [unit, 0]),
  ) as Record<TimeIntervalUnit, number>;
  if (!Number.isFinite(remaining)) return result;

  units.forEach((unit) => {
    const factor = timeIntervalMilliseconds[unit];
    const part = Math.floor(remaining / factor);
    result[unit] = part;
    remaining -= part * factor;
  });
  return result;
}

export function composeTimeIntervalParts(
  parts: Partial<Record<TimeIntervalUnit, number>>,
  configUnit: string,
): number {
  const configFactor =
    timeIntervalMilliseconds[configUnit.toLowerCase() as TimeIntervalUnit];
  if (configFactor === undefined) return Number.NaN;
  const milliseconds = TIME_INTERVAL_UNITS.reduce(
    (total, unit) =>
      total + (Number(parts[unit]) || 0) * timeIntervalMilliseconds[unit],
    0,
  );
  return milliseconds / configFactor;
}

export function getTimeIntervalStep(
  increment: unknown,
  configUnit: string,
  displayedUnit: TimeIntervalUnit,
): number {
  const numericIncrement = Number(increment);
  const sourceIncrement =
    Number.isFinite(numericIncrement) && numericIncrement > 0
      ? numericIncrement
      : 1;
  return (
    timeValueToMilliseconds(sourceIncrement, configUnit) /
    timeIntervalMilliseconds[displayedUnit]
  );
}

export function getTimeIntervalCompatibility(
  value: unknown,
  configUnit: string,
  units: TimeIntervalUnit[],
  attributes: Record<string, unknown>,
): { compatible: boolean; reason: string } {
  const numericValue = Number(value);
  if (
    value === '' ||
    !Number.isFinite(numericValue) ||
    !Number.isInteger(numericValue)
  ) {
    return { compatible: false, reason: 'Enter a whole-number time interval.' };
  }
  if (!units.length || !Number.isFinite(timeValueToMilliseconds(1, configUnit))) {
    return { compatible: false, reason: 'The configured time units are invalid.' };
  }

  const minimum = Number(attributes.minimum);
  if (attributes.minimum !== undefined && Number.isFinite(minimum) && numericValue < minimum) {
    return { compatible: false, reason: 'The value is below the configured minimum.' };
  }
  const maximum = Number(attributes.maximum);
  if (attributes.maximum !== undefined && Number.isFinite(maximum) && numericValue > maximum) {
    return { compatible: false, reason: 'The value exceeds the configured maximum.' };
  }

  const increment = Number(attributes.increment_step);
  if (attributes.increment_step !== undefined) {
    if (!Number.isFinite(increment) || increment <= 0) {
      return { compatible: false, reason: 'The configured increment is invalid.' };
    }
    const remainder = Math.abs(numericValue % increment);
    const tolerance = Number.EPSILON * Math.max(1, Math.abs(numericValue));
    if (remainder > tolerance && Math.abs(increment - remainder) > tolerance) {
      return {
        compatible: false,
        reason: 'The value is not aligned with the configured increment.',
      };
    }
  }

  const milliseconds = timeValueToMilliseconds(numericValue, configUnit);
  const smallestFactor = timeIntervalMilliseconds[units[units.length - 1]];
  if (!Number.isInteger(milliseconds / smallestFactor)) {
    return {
      compatible: false,
      reason: 'The configured fields cannot represent this value exactly.',
    };
  }

  const parts = decomposeTimeInterval(numericValue, configUnit, units);
  const overflowingPart = units.slice(1).find(
    (unit) => parts[unit] > timeIntervalPartMaximum[unit],
  );
  if (overflowingPart) {
    return {
      compatible: false,
      reason: `The ${overflowingPart} field exceeds its supported range.`,
    };
  }
  return { compatible: true, reason: '' };
}

/**
 * Get parse function based on unit type
 */
export function getParseFunction(unitType: string): (value: string) => number {
  return unitType === 'int' ? parseInt : parseFloat;
}

/**
 * Check if widget unit is higher precision than config unit
 */
export function isWidgetUnitHigher(configUnit: string, widgetUnit: string): boolean {
  const units = ['b', 'kb', 'mb', 'gb', 'tb', 'pb'];
  const configIndex = units.indexOf(configUnit.toLowerCase());
  const widgetIndex = units.indexOf(widgetUnit.toLowerCase());
  
  if (configIndex === -1 || widgetIndex === -1) return false;
  return widgetIndex > configIndex;
}
