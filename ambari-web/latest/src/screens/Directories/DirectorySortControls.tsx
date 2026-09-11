/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
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
import { faArrowDown, faArrowUp } from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";

type SortOption<T extends string> = {
  label: string;
  value: T;
};

type DirectorySortControlsProps<T extends string> = {
  direction: "asc" | "desc";
  onDirectionChange: () => void;
  onSortChange: (sort: T) => void;
  options: SortOption<T>[];
  sort: T;
};

export default function DirectorySortControls<T extends string>({
  direction,
  onDirectionChange,
  onSortChange,
  options,
  sort,
}: DirectorySortControlsProps<T>) {
  const { t } = useTranslation();
  const directionLabel = direction === "asc"
    ? t("directory.ascending")
    : t("directory.descending");
  return (
    <div className="directory-mobile-sort d-md-none mb-3">
      <div>
        <label className="form-label" htmlFor="directory-mobile-sort">{t("directory.sortBy")}</label>
        <select
          className="form-select"
          id="directory-mobile-sort"
          onChange={(event) => onSortChange(event.target.value as T)}
          value={sort}
        >
          {options.map((option) => (
            <option key={option.value} value={option.value}>{option.label}</option>
          ))}
        </select>
      </div>
      <button
        aria-label={t("directory.changeSortDirection", { direction: directionLabel })}
        className="btn btn-outline-secondary directory-sort-direction"
        onClick={onDirectionChange}
        title={directionLabel}
        type="button"
      >
        <FontAwesomeIcon icon={direction === "asc" ? faArrowUp : faArrowDown} />
      </button>
    </div>
  );
}
