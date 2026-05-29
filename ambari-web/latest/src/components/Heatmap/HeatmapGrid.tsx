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

import React from 'react';
import { Row } from 'react-bootstrap';
import HeatmapRack from './HeatmapRack';

interface HeatmapGridProps {
  racks: any[];
  hostToSlotMap: Record<string, number>;
  hostToValueMap: Record<string, string | undefined>;
  slotDefinitions: any[];
  units?: string;
  onHostClick?: (host: any) => void;
  selectedMetric: any;
}

const HeatmapGrid: React.FC<HeatmapGridProps> = ({
  racks,
  hostToSlotMap,
  hostToValueMap,
  slotDefinitions,
  units,
  onHostClick,
  selectedMetric
}) => {
  const getRackClass = (rackCount: number): string => {
    if (rackCount < 2) {
      return "col-12";
    }
    if (rackCount === 2) {
      return "col-md-6";
    }
    return "col-md-4";
  };

  return (
    <Row className="g-3">
      {racks.map((rack) => (
        <div key={rack.rackId} className={getRackClass(racks.length)}>
          <HeatmapRack
            rack={rack}
            hostToSlotMap={hostToSlotMap}
            hostToValueMap={hostToValueMap}
            slotDefinitions={slotDefinitions}
            units={units}
            onHostClick={onHostClick}
            selectedMetric={selectedMetric}
          />
        </div>
      ))}
    </Row>
  );
};

export default HeatmapGrid;
