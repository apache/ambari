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
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faCheck,
  faClockRotateLeft,
  faCog,
  faDownload,
  faMedkit,
  faPlay,
  faRemove,
  faRepeat,
  faStop,
} from "@fortawesome/free-solid-svg-icons";

export const getIcon = (iconName: string, className: string) => {
  className = "mx-2 " + className;

  switch (iconName) {
    case "play":
      return <FontAwesomeIcon icon={faPlay} className={className} />;
    case "stop":
      return <FontAwesomeIcon icon={faStop} className={className} />;
    case "repeat":
      return <FontAwesomeIcon icon={faRepeat} className={className} />;
    case "cog":
      return <FontAwesomeIcon icon={faCog} className={className} />;
    case "medkit":
      return <FontAwesomeIcon icon={faMedkit} className={className} />;
    case "remove":
      return <FontAwesomeIcon icon={faRemove} className={className} />;
    case "check":
      return <FontAwesomeIcon icon={faCheck} className={className} />;
    case "download":
      return <FontAwesomeIcon icon={faDownload} className={className} />;
    case "clockRotateLeft":
      return <FontAwesomeIcon icon={faClockRotateLeft} className={className} />;
    default:
      return null;
  }
};
