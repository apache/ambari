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

import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Modal, Button, ProgressBar } from 'react-bootstrap';
import { useHistory } from 'react-router-dom';
import './SessionTimeout.scss';

interface SessionTimeoutProps {
  timeoutMinutes?: number;
  warningMinutes?: number;
  onLogout: () => void;
  onSessionExtended?: () => void;
}

const SessionTimeout: React.FC<SessionTimeoutProps> = ({
  timeoutMinutes = 30,
  warningMinutes = 5,
  onLogout,
  onSessionExtended
}) => {
  const [showWarning, setShowWarning] = useState(false);
  const [remainingTime, setRemainingTime] = useState(0);
  const [progress, setProgress] = useState(100);
  const timeoutRef = useRef<NodeJS.Timeout | null>(null);
  const warningRef = useRef<NodeJS.Timeout | null>(null);
  const countdownRef = useRef<NodeJS.Timeout | null>(null);
  const history = useHistory();
  
  // Convert minutes to milliseconds
  const timeoutMs = timeoutMinutes * 60 * 1000;
  const warningMs = warningMinutes * 60 * 1000;
  
  // Reset all timers and start a new session timeout
  const resetTimeout = useCallback(() => {
    // Clear existing timers
    if (timeoutRef.current) clearTimeout(timeoutRef.current);
    if (warningRef.current) clearTimeout(warningRef.current);
    if (countdownRef.current) clearInterval(countdownRef.current);
    
    // Hide warning modal if it's showing
    setShowWarning(false);
    
    // Set timeout for showing warning
    warningRef.current = setTimeout(() => {
      setShowWarning(true);
      setRemainingTime(warningMs / 1000);
      setProgress(100);
      
      // Start countdown for remaining time
      countdownRef.current = setInterval(() => {
        setRemainingTime(prev => {
          if (prev <= 1) {
            if (countdownRef.current) clearInterval(countdownRef.current);
            return 0;
          }
          
          // Update progress bar
          const newProgress = ((prev - 1) / (warningMs / 1000)) * 100;
          setProgress(newProgress);
          
          return prev - 1;
        });
      }, 1000);
    }, timeoutMs - warningMs);
    
    // Set timeout for session expiration
    timeoutRef.current = setTimeout(() => {
      handleLogout();
    }, timeoutMs);
    
    // Call the session extended callback if provided
    if (onSessionExtended) {
      onSessionExtended();
    }
  }, [timeoutMs, warningMs, onSessionExtended]);
  
  // Handle user activity to reset the timeout
  const handleUserActivity = useCallback(() => {
    // Only reset if the user is authenticated
    const user = localStorage.getItem('ambari-user');
    if (user) {
      resetTimeout();
    }
  }, [resetTimeout]);
  
  // Handle logout
  const handleLogout = useCallback(() => {
    // Clear all timers
    if (timeoutRef.current) clearTimeout(timeoutRef.current);
    if (warningRef.current) clearTimeout(warningRef.current);
    if (countdownRef.current) clearInterval(countdownRef.current);
    
    // Hide warning modal
    setShowWarning(false);
    
    // Call the logout callback
    onLogout();
    
    // Redirect to login page
    history.push('/login');
  }, [onLogout, history]);
  
  // Handle continue session
  const handleContinueSession = () => {
    resetTimeout();
  };
  
  // Format remaining time as MM:SS
  const formatTime = (seconds: number): string => {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };
  
  useEffect(() => {
    // Set up event listeners for user activity
    const activityEvents = [
      'mousedown', 'mousemove', 'keydown',
      'scroll', 'touchstart', 'click'
    ];
    
    // Add event listeners
    activityEvents.forEach(event => {
      window.addEventListener(event, handleUserActivity);
    });
    
    // Initial timeout setup
    resetTimeout();
    
    // Cleanup function
    return () => {
      // Remove event listeners
      activityEvents.forEach(event => {
        window.removeEventListener(event, handleUserActivity);
      });
      
      // Clear timers
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
      if (warningRef.current) clearTimeout(warningRef.current);
      if (countdownRef.current) clearInterval(countdownRef.current);
    };
  }, [handleUserActivity, resetTimeout]);
  
  return (
    <Modal 
      show={showWarning} 
      onHide={handleContinueSession}
      backdrop="static"
      keyboard={false}
      centered
      className="session-timeout-modal"
    >
      <Modal.Header>
        <Modal.Title>Session Timeout Warning</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <p>
          Your session will expire in <strong>{formatTime(remainingTime)}</strong> due to inactivity.
        </p>
        <p>
          You will be automatically logged out when the timer expires.
        </p>
        <ProgressBar 
          now={progress} 
          variant={progress > 50 ? 'success' : progress > 20 ? 'warning' : 'danger'} 
          className="timeout-progress"
        />
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={handleLogout}>
          Logout Now
        </Button>
        <Button variant="primary" onClick={handleContinueSession}>
          Continue Session
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default SessionTimeout;
