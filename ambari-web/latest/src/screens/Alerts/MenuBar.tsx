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

import React, { useState, useRef, useEffect } from 'react';
import { Row, Col, Button } from 'react-bootstrap';
import { AlertDefinition, AlertGroupItem } from './types';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faFilter, faXmarkCircle } from '@fortawesome/free-solid-svg-icons';
import { ALERT_SEARCH_CATEGORIES } from './constants';
import { getCategoryValues, getTimeRangeValue } from './alertUtils';
import '../../styles/MenuBar.scss';
import { ActionsButton } from './ActionsButton';

interface SearchFilter {
    category: string;
    value: string;
}

interface MenuBarProps {
    title: string;
    alertGroups: AlertGroupItem[];
    alertCounts: Record<string, number>;
    onSearch: (filters: SearchFilter[]) => void;
    alertDefinitions?: AlertDefinition[];
    onModalStateChange?: (isOpen: boolean) => void;
    hasAnyAlertPermissions?: boolean;
}

const MenuBar: React.FC<MenuBarProps> = ({ title, alertGroups, onSearch,
                                             alertDefinitions, onModalStateChange, hasAnyAlertPermissions = true
    }) => {
    const [showSearch, setShowSearch] = useState(false);
    const [selectedCategory, setSelectedCategory] = useState('');
    const [inputValue, setInputValue] = useState('');
    const [searchFilters, setSearchFilters] = useState<SearchFilter[]>([]);
    const [suggestions, setSuggestions] = useState<string[]>([]);
    const [showCategories, setShowCategories] = useState(false);
    const searchRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (searchRef.current && !searchRef.current.contains(event.target as Node)) {
                setShowCategories(false);
                setSuggestions([]);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const toggleSearch = () => {
        setShowSearch(!showSearch);
    };

    const handleInputFocus = () => {
        if (!selectedCategory) {
            setShowCategories(true);
        }
    };

    const handleInputChange = (value: string) => {
        setInputValue(value);
        if (selectedCategory) {
            const categoryValues = getCategoryValues(selectedCategory, alertGroups || []);
            console.log('Category values:', categoryValues);
            setSuggestions(
                categoryValues.filter(item =>
                    item.toLowerCase().includes(value.toLowerCase())
                )
            );
        }
    };

    const handleCategorySelect = (category: string) => {
        console.log('Selected category:', category);
        setSelectedCategory(category);
        setShowCategories(false);
        setInputValue('');
        setSuggestions(getCategoryValues(category, alertGroups || []));
        console.log('Available suggestions:', getCategoryValues(category, alertGroups || []));
    };

    const handleSuggestionSelect = (suggestion: string) => {
        if (selectedCategory) {
            let filterValue = suggestion;
            if (selectedCategory === 'Last Status Changed') {
                const date = getTimeRangeValue(suggestion);
                if (date) {
                    filterValue = suggestion;
                }
            }
            const newFilter = { category: selectedCategory, value: filterValue };
            const newFilters = [...searchFilters, newFilter];
            setSearchFilters(newFilters);
            setSelectedCategory('');
            setInputValue('');
            setSuggestions([]);
            onSearch(newFilters);
        }
    };

    const removeFilter = (index: number) => {
        const newFilters = searchFilters.filter((_, i) => i !== index);
        setSearchFilters(newFilters);
        onSearch(newFilters);
    };

    const clearAllFilters = () => {
        setSearchFilters([]);
        onSearch([]);
    };

    return (
        <div>
            <div className="row d-flex justify-content-between align-items-center mb-4">
                <div className="col-sm-6">
                    <h2 className="mb-0">{title}</h2>
                </div>
                <div className="col-sm-6 d-flex justify-content-end">
                    <Button className="btn btn-default me-2" onClick={toggleSearch}>
                        <FontAwesomeIcon icon={faFilter} />
                    </Button>
                    {/* Only show Actions button if user has any alert management permissions */}
                    {hasAnyAlertPermissions && (
                        <ActionsButton
                            alertGroups={alertGroups}
                            allAlertDefinitions={alertDefinitions}
                            onModalStateChange={onModalStateChange}
                        />
                    )}
                </div>
            </div>
            {showSearch && (
                <Row className="mb-4">
                    <Col xs={12}>
                        <div className="VS-search" ref={searchRef}>
                            <div className="VS-search-inner">
                                {searchFilters.map((filter, index) => (
                                    <div key={index} className="search-facet">
                                        <div className="category">{filter.category}:</div>
                                        <div className="search-facet-input">
                                            <span>{filter.value}</span>
                                        </div>
                                        <div
                                            className="search-facet-remove"
                                            onClick={() => removeFilter(index)}
                                        >
                                            ×
                                        </div>
                                    </div>
                                ))}
                                <div className="search-input-wrapper">
                                    <input
                                        type="text"
                                        className="search-input"
                                        value={inputValue}
                                        onChange={(e) => handleInputChange(e.target.value)}
                                        onFocus={handleInputFocus}
                                        placeholder={selectedCategory ? `Enter ${selectedCategory}...` : 'Search...'}
                                    />
                                    {showCategories && !selectedCategory && (
                                        <div className="dropdown-menu show">
                                            {ALERT_SEARCH_CATEGORIES.map(category => (
                                                <div
                                                    key={category}
                                                    className="dropdown-item"
                                                    onClick={() => handleCategorySelect(category)}
                                                >
                                                    {category}
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                    {selectedCategory && suggestions.length > 0 && (
                                        <div className="dropdown-menu show">
                                            {suggestions.map(suggestion => (
                                                <div
                                                    key={suggestion}
                                                    className="dropdown-item"
                                                    onClick={() => handleSuggestionSelect(suggestion)}
                                                >
                                                    {suggestion}
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>
                                {searchFilters.length > 0 && (
                                    <Button
                                        variant='transparent'
                                        className="ms-2 text-light"
                                        onClick={clearAllFilters}
                                    >
                                        <FontAwesomeIcon icon={faXmarkCircle} />
                                    </Button>
                                )}
                            </div>
                        </div>
                    </Col>
                </Row>
            )}
        </div>
    );
};

export default MenuBar;
