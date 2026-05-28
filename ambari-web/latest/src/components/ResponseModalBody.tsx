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

import CopyButton from './CopyButton';

type ResponseModalBodyProps = {
    responseText: string;
};

export default function ResponseModalBody({ responseText }: ResponseModalBodyProps) {
    return (
        <div>
            {/* Copy Button */}
            <div className="d-flex justify-content-end mb-2">
                <CopyButton textToCopy={responseText} />
            </div>
            {/* Response Text */}
            <div className="pre-wrap">{responseText}</div>
        </div>
    );
}