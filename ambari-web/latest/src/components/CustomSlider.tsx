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

import { useEffect, useRef, useState } from "react";
import Slider from "rc-slider";
import "rc-slider/assets/index.css";
import { extraRound, formatTickLabel } from "../Utils/unitConversionUtils";

export type SliderMarker = {
  kind: "current" | "default" | "recommended";
  value: number;
  onSelect?: () => void;
};

interface CustomSliderProps {
  min: number;
  max: number;
  step: number;
  value: number;
  unit?: string;
  onChange: (value: number) => void;
  onChangeComplete?: (value: number) => void;
  disabled?: boolean;
  marks?: Record<number, string>;
  markers?: SliderMarker[];
}

const CustomSlider: React.FC<CustomSliderProps> = ({
  min,
  max,
  step,
  value,
  unit = "",
  onChange,
  onChangeComplete,
  disabled = false,
  marks,
  markers = [],
}) => {
  const [tooltipPosition, setTooltipPosition] = useState(0);
  const sliderRef = useRef<HTMLDivElement>(null);
  const displayValue = extraRound(value);

  // Update tooltip position when value changes
  useEffect(() => {
    if (sliderRef.current) {
      let percentage =
        max === min ? 50 : ((displayValue - min) / (max - min)) * 100;

      if (percentage < 1) percentage = 1;
      if (percentage > 99) percentage = 99;

      setTooltipPosition(percentage);
    }
  }, [displayValue, min, max]);

  const markerPosition = (markerValue: number) =>
    max === min ? 50 : ((markerValue - min) / (max - min)) * 100;
  const markerLabel = (marker: SliderMarker) =>
    `${marker.kind[0].toUpperCase()}${marker.kind.slice(1)} value: ${formatTickLabel(
      marker.value,
      unit,
      unit ? " " : "",
    )}`;

  return (
    <div className="custom-slider-container" ref={sliderRef}>
      <output
        aria-label="Current slider value"
        className="custom-slider-tooltip"
        style={{
          left: `${tooltipPosition}%`,
        }}
      >
        {formatTickLabel(displayValue, unit, "")}
      </output>

      <Slider
        min={min}
        max={max}
        step={step}
        marks={marks}
        value={displayValue}
        onChange={(nextValue) => onChange(nextValue as number)}
        onChangeComplete={(nextValue) =>
          onChangeComplete?.(nextValue as number)
        }
        disabled={disabled}
      />
      <div className="custom-slider-markers" aria-label="Slider value markers">
        {markers
          .filter((marker) => marker.value >= min && marker.value <= max)
          .map((marker) => {
            const label = markerLabel(marker);
            const markerStyle = {
              left: `${markerPosition(marker.value)}%`,
            };
            return marker.onSelect ? (
              <button
                key={`${marker.kind}-${marker.value}`}
                type="button"
                className={`custom-slider-marker custom-slider-marker-${marker.kind}`}
                style={markerStyle}
                aria-label={`Use ${marker.kind} value: ${formatTickLabel(
                  marker.value,
                  unit,
                  unit ? " " : "",
                )}`}
                title={label}
                disabled={disabled}
                onClick={marker.onSelect}
              />
            ) : (
              <span
                key={`${marker.kind}-${marker.value}`}
                className={`custom-slider-marker custom-slider-marker-${marker.kind}`}
                style={markerStyle}
                aria-label={label}
                title={label}
              />
            );
          })}
      </div>
    </div>
  );
};

export default CustomSlider;
