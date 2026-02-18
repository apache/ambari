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

import React, { useContext } from 'react';
import { Navigate } from 'react-router-dom';
import { AppContext } from '../store/context';

interface UpgradeGuardProps {
  children: React.ReactNode;
  redirectTo?: string;
}

/**
 * UpgradeGuard component that redirects users away from admin pages during upgrades
 * Based on Ember.js logic that blocks access to admin pages when upgrade is in progress
 * The permission checks are already handled in UserContext.havePermissions()
 */
export const UpgradeGuard: React.FC<UpgradeGuardProps> = ({ 
  children, 
  redirectTo = '/main/dashboard' 
}) => {
  const { upgradeIsRunning, upgradeSuspended } = useContext(AppContext);

  // Check if upgrade is blocking operations (running but not suspended)
  // The permission system in UserContext already handles who can access what during upgrades
  const isUpgradeBlocking = upgradeIsRunning && !upgradeSuspended;

  // If upgrade is blocking, redirect to dashboard
  if (isUpgradeBlocking) {
    return <Navigate to={redirectTo} replace />;
  }

  return <>{children}</>;
};

export default UpgradeGuard;
