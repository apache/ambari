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
import { Col, OverlayTrigger, Popover, PopoverBody, PopoverHeader, Row } from 'react-bootstrap';
import { calculateHostPercentages, convertToDisplayValue } from '../../Utils/metricProcessing';

interface HeatmapHostProps {
  host: {
    hostName: string;
    publicHostName: string;
    ip: string;
    hostComponents: string[];
    osType: any;
    rack: string;
  };
  selectedMetric: any;
  hostValue?: string | number;
  units?: string;
  slot?: number;
  slotDefinition?: any;
  onClick?: () => void;
}

const HeatmapHost: React.FC<HeatmapHostProps> = ({
  host,
  slot,
  slotDefinition,
  selectedMetric,
  hostValue,
  units,
  onClick
}) => {
  const hostPercentages = calculateHostPercentages(host);
  
  const backgroundColor = slotDefinition?.cssStyle?.includes('background-color') 
    ? slotDefinition.cssStyle.split(':')[1].trim() 
    : '#f8f9fa';

  const isSpecialSlot = slot !== undefined && slot > 4;
  const backgroundImage = slotDefinition?.cssStyle?.includes('repeating-linear-gradient') 
    ? slotDefinition.cssStyle.split(':').slice(1).join(':') 
    : 'none';

  const PopoverRow: React.FC<{ label: string; value: string }> = ({ label, value }) => (
    <>
      <Row>
        <Col md={6}>
          <strong>{label}</strong>
        </Col>
        <Col md={6}>
          {value}
        </Col>
      </Row>
      <div className='border border-bottom my-2'></div>
    </>
  );

  const detailedPopover = (
    <Popover id={`popover-${host.hostName}`}>
      <PopoverHeader>
        <strong>{host.hostName}</strong>
      </PopoverHeader>
      <PopoverBody>
        <PopoverRow 
          label={selectedMetric.widget_name}
          value={hostValue !== undefined ? convertToDisplayValue(hostValue, units || '') : 'N/A'}
        />

        <PopoverRow label="OS" value={host.osType} />
        <PopoverRow label="IP Address" value={host.ip} />
        <PopoverRow label="Rack" value={host.rack} />

        <PopoverRow 
          label="Disk Usage" 
          value={`${hostPercentages.diskUsagePercent.toFixed(1)}%`}
        />
        <PopoverRow 
          label="CPU Usage" 
          value={`${hostPercentages.cpuUsagePercent.toFixed(1)}%`}
        />
        <PopoverRow 
          label="Memory Usage" 
          value={`${hostPercentages.memoryUsagePercent.toFixed(1)}%`}
        />
        <PopoverRow 
          label="CPU Wait I/O" 
          value={`${hostPercentages.cpuWaitIOPercent.toFixed(1)}%`}
        />
        <PopoverRow 
          label="Components" 
          value={`${host.hostComponents.join(", ")}`}
        />
      </PopoverBody>
    </Popover>
  );

  const hostTileStyle = {
    backgroundColor: backgroundColor,
    backgroundImage: backgroundImage,
    color: backgroundColor === '#666' ? 'white' : 'black',
    height: '40px',
    fontSize: '10px',
    fontWeight: 'bold',
    borderRadius: '4px',
    cursor: 'pointer',
    transition: 'all 0.2s ease'
  };

  const handleMouseEnter = (e: React.MouseEvent<HTMLDivElement>) => {
    e.currentTarget.style.transform = 'scale(1.05)';
    e.currentTarget.style.zIndex = '10';
  };

  const handleMouseLeave = (e: React.MouseEvent<HTMLDivElement>) => {
    e.currentTarget.style.transform = 'scale(1)';
    e.currentTarget.style.zIndex = '1';
  };

  return (
    <OverlayTrigger placement="bottom" overlay={detailedPopover}>
      <div
        className={`heatmap-host d-flex align-items-center justify-content-center text-center cursor-pointer ${isSpecialSlot ? '' : 'border'}`}
        style={hostTileStyle}
        onClick={onClick}
        onMouseEnter={handleMouseEnter}
        onMouseLeave={handleMouseLeave}
      >
      </div>
    </OverlayTrigger>
  );
};

export default HeatmapHost;
