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

import { DropdownItem } from "react-bootstrap";
import { ServiceActionEnums } from "../../../../enums/ServiceActionEnums";
import { useEffect, useState } from "react";
import ValidateEnablement from "./ValidateEnablement";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faSitemap } from "@fortawesome/free-solid-svg-icons";

function EnableHighAvailibilityNameNode({
  isMappingOnly,
}: {
  isMappingOnly?: boolean;
}) {
  const [shouldStartEnableFlow, setShouldStartEnableFlow] = useState(false);
  const { componentName } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  useEffect(() => {
    if (
      location.pathname.includes("highAvailability") &&
      componentName === "NameNode"
    ) {
      setShouldStartEnableFlow(true);
    }
  }, []);

  return (
    <>
      {shouldStartEnableFlow ? <ValidateEnablement /> : null}
      {!isMappingOnly ? (
        <DropdownItem
          onClick={() => {
            navigate(`/main/services/highAvailability/NameNode/enable/step1`);
          }}
        >
          <FontAwesomeIcon className="text-secondary me-2" icon={faSitemap} />
          {ServiceActionEnums.enableHighAvailibility}
        </DropdownItem>
      ) : null}
    </>
  );
}

export default EnableHighAvailibilityNameNode;
