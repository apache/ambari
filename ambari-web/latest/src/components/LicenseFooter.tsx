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

interface LicenseFooterProps {
  isSidebarCollapsed?: boolean;
  hasSidebar?: boolean;
}

const LicenseFooter: React.FC<LicenseFooterProps> = ({ 
  isSidebarCollapsed = false, 
  hasSidebar = true 
}) => {
  const getLeftPosition = () => {
    if (!hasSidebar) return '0'; // Full width for installer/wizard
    return isSidebarCollapsed ? '60px' : '230px'; // Account for sidebar width
  };

  return (
    <footer 
      className="license-footer py-2 mt-auto"
      style={{
        borderTop: hasSidebar?'1px solid #dee2e6':"none",
        background:hasSidebar?"white":"transparent",
        fontSize: '12px',
        color: '#6c757d',
        position: 'fixed',
        bottom: 0,
        left: getLeftPosition(),
        right: 0,
        zIndex: 1000
      }}
    >
      <div className="container-fluid footer-links text-start">
        <a 
          data-qa="license-link"
          href="http://www.apache.org/licenses/LICENSE-2.0" 
          target="_blank" 
          rel="noopener noreferrer"
          className="text-decoration-none text-info fs-12"
        >
          Licensed under the Apache License, Version 2.0
        </a>
        .<br />
        <a 
          data-qa="third-party-link"
          href="/licenses/NOTICE.txt" 
          target="_blank" 
          rel="noopener noreferrer"
          className="text-decoration-none text-info fs-12"
        >
          See third-party tools/resources that Ambari uses and their respective authors
        </a>
      </div>
    </footer>
  );
};

export default LicenseFooter;
