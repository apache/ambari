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

import React, { useState, useEffect, useRef } from 'react';
import {Button, Modal} from 'react-bootstrap';
import { latestAmbariUrl } from './utils/navigation';

interface InactivityTimeoutProps {
  timeout: number;
}

const InactivityTimeout: React.FC<InactivityTimeoutProps> = ({ timeout }) => {
  const TIME_OUT = timeout;
  const [lastActiveTime, setLastActiveTime] = useState(Date.now());
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [remainTime, setRemainTime] = useState(60);
  const intervalRef = useRef<NodeJS.Timeout | null>(null);

  const keepActive = () => {
    setLastActiveTime(Date.now());
  };

  useEffect(() => {
    const handleMouseMove = keepActive;
    const handleKeyPress = keepActive;
    const handleClick = keepActive;

    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('keypress', handleKeyPress);
    window.addEventListener('click', handleClick);

    intervalRef.current = setInterval(async () => {
      const timeElapsed = Date.now() - lastActiveTime;
      const remainingTime = TIME_OUT - timeElapsed;

      if (remainingTime < 0) {
        window.removeEventListener('mousemove', handleMouseMove);
        window.removeEventListener('keypress', handleKeyPress);
        window.removeEventListener('click', handleClick);
        if (intervalRef.current) clearInterval(intervalRef.current);
        localStorage.clear();
        window.location.replace(latestAmbariUrl("/login"));
      } else if (remainingTime < 60000 && !isModalOpen) {
        setIsModalOpen(true);
      }
    }, 1000);

    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('keypress', handleKeyPress);
      window.removeEventListener('click', handleClick);
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [lastActiveTime, TIME_OUT, isModalOpen]);

  useEffect(() => {
    if (isModalOpen) {
      const countdownInterval = setInterval(() => {
        setRemainTime(prev => {
          if (prev === 1) {
            localStorage.clear();
            window.location.replace(latestAmbariUrl("/login"));
            return 0;
          }
          return prev - 1;
        });
      }, 1000);

      return () => clearInterval(countdownInterval);
    }
  }, [isModalOpen]);

  const handleRemainLoggedIn = () => {
    setIsModalOpen(false);
    setRemainTime(60);
    setLastActiveTime(Date.now());
  };

  const handleLogout = async () => {
    setIsModalOpen(false);
    localStorage.clear();
    window.location.replace(latestAmbariUrl("/login"));
  };

  return (
      <Modal show={isModalOpen} onHide={handleRemainLoggedIn}>
        <Modal.Header className="d-flex justify-content-start">
          <Modal.Title>Automatic Logout</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p>You will be automatically logged out in <strong>{remainTime}</strong> seconds due to inactivity</p>
        </Modal.Body>
        <Modal.Footer>
          <Button className="tab-button" onClick={handleLogout}>
            LOG OUT NOW
          </Button>
          <Button className="btn-launch-cluster-wizard" onClick={handleRemainLoggedIn}>
            REMAIN LOGGED IN
          </Button>
        </Modal.Footer>
      </Modal>
  );
};

export default InactivityTimeout;
