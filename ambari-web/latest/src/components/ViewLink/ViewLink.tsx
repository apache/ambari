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
import { Link } from 'react-router-dom';
import { generateRegularViewUrl } from '../../Utils/viewUtils';

interface ViewLinkProps {
    viewName: string;
    viewVersion: string;
    instanceName: string;
    viewPath?: string;
    className?: string;
    children?: React.ReactNode;
}

/**
 * A component that renders a link to any Ambari view
 */
const ViewLink: React.FC<ViewLinkProps> = ({
                                               viewName,
                                               viewVersion,
                                               instanceName,
                                               viewPath = '',
                                               className = '',
                                               children
                                           }) => {
    const viewUrl = generateRegularViewUrl(viewName, viewVersion, instanceName, viewPath);

    return (
        <Link to={viewUrl} className={className}>
            {children || `${viewName} View`}
        </Link>
    );
};

export default ViewLink;
