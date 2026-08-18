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

import React, { useState, useEffect } from 'react';
import { AlertNotification } from '../screens/Alerts/types';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faTimes, faCheck, faPencilAlt } from '@fortawesome/free-solid-svg-icons';
import './EditableList.css';

interface EditableListProps {
  items: AlertNotification[];
  resources: AlertNotification[];
  name: string;
  isCaseSensitive?: boolean;
  onItemsChange: (items: AlertNotification[]) => void;
}

const EditableList: React.FC<EditableListProps> = ({
  items,
  resources,
  isCaseSensitive = false,
  onItemsChange
}) => {
  const [editMode, setEditMode] = useState(false);
  const [input, setInput] = useState('');
  const [typeahead, setTypeahead] = useState<AlertNotification[]>([]);
  const [selectedTypeahead, setSelectedTypeahead] = useState(0);
  const [itemsOriginal, setItemsOriginal] = useState<AlertNotification[]>([]);

  // Update items original when items change
  useEffect(() => {
    setItemsOriginal([...items]);
  }, [items]);

  // Update typeahead when input changes
  useEffect(() => {
    if (input) {
      const inputValue = isCaseSensitive ? input : input.toLowerCase();
      const availableItems = resources.filter(resource => {
        const nameToCompare = isCaseSensitive ? resource.AlertTarget.name : resource.AlertTarget.name.toLowerCase();
        const alreadyExists = items.some(item => item.AlertTarget.name === resource.AlertTarget.name);
        const matchesInput = nameToCompare.indexOf(inputValue) >= 0;
        return matchesInput && !alreadyExists;
      });
      setTypeahead(availableItems);
      setSelectedTypeahead(0);
    } else {
      setTypeahead([]);
      setSelectedTypeahead(0);
    }
  }, [input, items, resources, isCaseSensitive]);

  const enableEditMode = () => {
    setInput('');
    setEditMode(true);
  };

  const onPrimary = (event?: React.MouseEvent) => {
    setEditMode(false);
    setInput('');
    if (event) {
      event.stopPropagation();
    }
  };

  const onSecondary = () => {
    // Restore all items
    onItemsChange(itemsOriginal);
    setInput('');
    setEditMode(false);
  };

  const removeFromItems = (itemToRemove: AlertNotification) => {
    const updatedItems = items.filter(item => item.AlertTarget.id !== itemToRemove.AlertTarget.id);
    onItemsChange(updatedItems);
    setInput('');
  };

  const addItem = (itemToAdd: AlertNotification) => {
    const updatedItems = [...items, itemToAdd];
    setInput('');
    setTypeahead([]);
    setSelectedTypeahead(0);
    onItemsChange(updatedItems);
  };

  const handleInputKeyDown = (event: React.KeyboardEvent) => {
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      setSelectedTypeahead(Math.min(selectedTypeahead + 1, typeahead.length - 1));
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      setSelectedTypeahead(Math.max(selectedTypeahead - 1, 0));
    } else if (event.key === 'Enter') {
      event.preventDefault();
      if (typeahead.length > 0 && typeahead[selectedTypeahead]) {
        addItem(typeahead[selectedTypeahead]);
      }
    } else if (event.key === 'Escape') {
      onSecondary();
    }
  };

  return (
    <div className={`editable-list-container well ${editMode ? 'edit-mode' : ''}`}>
      <div className="items-box" onClick={!editMode ? enableEditMode : undefined}>
        <ul className="items-list">
          {items.map((item, index) => (
            <li key={item.AlertTarget.id || index} className={`item ${item.AlertTarget.global ? 'global' : 'deletable'}`}>
              <span>
                <a href="#" onClick={(e) => e.preventDefault()}>
                  {item.AlertTarget.name}
                  <small style={{ color: '#999', marginLeft: '5px' }}>
                    ({item.AlertTarget.global ? 'Global' : 'Custom'})
                  </small>
                </a>
                {!item.AlertTarget.global && (
                  <button 
                    type="button" 
                    className="close"
                    onClick={(e) => {
                      e.stopPropagation();
                      removeFromItems(item);
                    }}
                  >
                    <span>&times;</span>
                  </button>
                )}
              </span>
            </li>
          ))}
          {editMode && (
            <li className="item add-item-input">
              <input
                type="text"
                className="form-control"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={handleInputKeyDown}
                placeholder="New"
                autoFocus
              />
              {typeahead.length > 0 && (
                <div className="typeahead-box">
                  <ul>
                    {typeahead.map((item, index) => (
                      <li 
                        key={item.AlertTarget.id || index}
                        className={index === selectedTypeahead ? 'selected' : ''}
                        onClick={() => addItem(item)}
                      >
                        {item.AlertTarget.name}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </li>
          )}
          {!editMode && items.length === 0 && (
            <li className="item add-item">Add</li>
          )}
        </ul>
      </div>

      {editMode ? (
        <div className="actions-panel">
          <button className="btn btn-default btn-xs" onClick={onSecondary}>
            <FontAwesomeIcon icon={faTimes} />
          </button>
          <button className="btn btn-primary btn-xs" onClick={onPrimary}>
            <FontAwesomeIcon icon={faCheck} />
          </button>
        </div>
      ) : (
        <div className="pencil-box" onClick={enableEditMode}>
          <FontAwesomeIcon icon={faPencilAlt} />
        </div>
      )}
    </div>
  );
};

export default EditableList;
