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
import { Card, CardHeader, CardBody, Row, Col } from 'react-bootstrap';
import Spinner from '../Spinner';
import HeatmapHost from './HeatmapHost';

interface HeatmapRackProps {
  rack: {
    name: string;
    rackId: string;
    hosts: any[];
    isLoaded: boolean;
    index: number;
  };
  hostToSlotMap: Record<string, number>;
  hostToValueMap: Record<string, string | undefined>;
  slotDefinitions: any[];
  units?: string;
  onHostClick?: (host: any) => void;
  selectedMetric: any;
}

const HeatmapRack: React.FC<HeatmapRackProps> = ({
  rack,
  hostToSlotMap,
  hostToValueMap,
  slotDefinitions,
  units,
  onHostClick,
  selectedMetric
}) => {
  return (
    <Card className="h-100">
      <CardHeader>
        <h3 className="mb-0">{rack.name}</h3>
      </CardHeader>
      <CardBody className="p-2">
        {rack.isLoaded ? (
          <Row className="g-1">
            {rack.hosts.map((host) => (
              <Col key={host.hostName} md={Math.floor(12 / rack.hosts.length)}>
                <HeatmapHost
                  host={host}
                  slot={hostToSlotMap[host.hostName]}
                  slotDefinition={slotDefinitions[hostToSlotMap[host.hostName]]}
                  hostValue={hostToValueMap[host.hostName]}
                  units={units}
                  onClick={() => onHostClick?.(host)}
                  selectedMetric={selectedMetric}
                />
              </Col>
            ))}
          </Row>
        ) : (
          <div className="text-center py-3">
            <Spinner />
          </div>
        )}
      </CardBody>
    </Card>
  );
};

export default HeatmapRack;
