/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the
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

import { faChevronDown, faChevronRight } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { MouseEventHandler } from "react";
import { Link } from "react-router-dom";

export default function SidebarItem({ ele, onClick, isSelected, isOpen = false, hasChildren = false }: {
  ele: any; onClick?: MouseEventHandler<HTMLElement>; isSelected: boolean; isOpen?: boolean; hasChildren?: boolean;
}) {
  const className = `text-decoration-none d-flex justify-content-between ${ele.className || ""} sideitem align-items-center ${isSelected ? "selected-item" : ""}`;
  const style = { ...ele.style, fontSize: 14, cursor: "pointer", padding: "10px 5px 10px 20px", width: "100%" };
  const content = <><span className="d-flex"><span>{ele.icon}</span><span className="ms-2">{ele.name}</span></span>{hasChildren && <FontAwesomeIcon icon={isOpen ? faChevronDown : faChevronRight} className="me-1" />}</>;
  return hasChildren ? <button type="button" className={`${className} border-0 bg-transparent text-start`} style={style} aria-expanded={isOpen} onClick={onClick}>{content}</button>
    : <Link to={ele.path} className={className} style={style} onClick={onClick}>{content}</Link>;
}
