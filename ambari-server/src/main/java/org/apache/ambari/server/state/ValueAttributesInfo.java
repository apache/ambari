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

package org.apache.ambari.server.state;

import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@XmlAccessorType(XmlAccessType.FIELD)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class ValueAttributesInfo {
  private String type;
  private String maximum;
  private String minimum;
  private String unit;
  private String delete;
  private Boolean visible;
  private Boolean overridable;
  private String copy;

  @XmlElement(name = "empty-value-valid")
  @JsonProperty("empty_value_valid")
  private Boolean emptyValueValid;

  @XmlElement(name = "ui-only-property")
  @JsonProperty("ui_only_property")
  private Boolean uiOnlyProperty;

  @XmlElement(name = "read-only")
  @JsonProperty("read_only")
  private Boolean readOnly;

  @XmlElement(name = "editable-only-at-install")
  @JsonProperty("editable_only_at_install")
  private Boolean editableOnlyAtInstall;

  @XmlElement(name = "show-property-name")
  @JsonProperty("show_property_name")
  private Boolean showPropertyName;

  @XmlElement(name = "increment-step")
  @JsonProperty("increment_step")
  private String incrementStep;

  @XmlElementWrapper(name = "entries")
  @XmlElements(@XmlElement(name = "entry"))
  private Collection<ValueEntryInfo> entries;

  @XmlElement(name = "hidden")
  private String hidden;

  @XmlElement(name = "entries_editable")
  private Boolean entriesEditable;

  @XmlElement(name = "selection-cardinality")
  @JsonProperty("selection_cardinality")
  private String selectionCardinality;

  @XmlElement(name = "property-file-name")
  @JsonProperty("property-file-name")
  private String propertyFileName;

  @XmlElement(name = "property-file-type")
  @JsonProperty("property-file-type")
  private String propertyFileType;

  public ValueAttributesInfo() {

  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getMaximum() {
    return maximum;
  }

  public void setMaximum(String maximum) {
    this.maximum = maximum;
  }

  public String getMinimum() {
    return minimum;
  }

  public void setMinimum(String minimum) {
    this.minimum = minimum;
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  public Collection<ValueEntryInfo> getEntries() {
    return entries;
  }

  public void setEntries(Collection<ValueEntryInfo> entries) {
    this.entries = entries;
  }

  public String getHidden() {
    return hidden;
  }

  public void setHidden(String hidden) {
    this.hidden = hidden;
  }

  public Boolean getEntriesEditable() {
    return entriesEditable;
  }

  public void setEntriesEditable(Boolean entriesEditable) {
    this.entriesEditable = entriesEditable;
  }

  public String getSelectionCardinality() {
    return selectionCardinality;
  }

  public void setSelectionCardinality(String selectionCardinality) {
    this.selectionCardinality = selectionCardinality;
  }

  public String getPropertyFileName() {
    return propertyFileName;
  }

  public void setPropertyFileName(String propertyFileName) {
    this.propertyFileName = propertyFileName;
  }

  public String getPropertyFileType() {
    return propertyFileType;
  }

  public void setPropertyFileType(String propertyFileType) {
    this.propertyFileType = propertyFileType;
  }

  public String getIncrementStep() {
    return incrementStep;
  }

  public void setIncrementStep(String incrementStep) {
    this.incrementStep = incrementStep;
  }

  public String getDelete() {
    return delete;
  }

  public void setDelete(String delete) {
    this.delete = delete;
  }

  public Boolean getEmptyValueValid() {
    return emptyValueValid;
  }

  public void setEmptyValueValid(Boolean isEmptyValueValid) {
    this.emptyValueValid = isEmptyValueValid;
  }

  public Boolean getVisible() {
    return visible;
  }

  public void setVisible(Boolean isVisible) {
    this.visible = isVisible;
  }

  public Boolean getReadOnly() {
    return readOnly;
  }

  public void setReadOnly(Boolean isReadOnly) {
    this.readOnly = isReadOnly;
  }

  public Boolean getEditableOnlyAtInstall() {
    return editableOnlyAtInstall;
  }

  public void setEditableOnlyAtInstall(Boolean isEditableOnlyAtInstall) {
    this.editableOnlyAtInstall = isEditableOnlyAtInstall;
  }

  public Boolean getOverridable() {
    return overridable;
  }

  public void setOverridable(Boolean isOverridable) {
    this.overridable = isOverridable;
  }

  public Boolean getShowPropertyName() {
    return showPropertyName;
  }

  public void setShowPropertyName(Boolean isPropertyNameVisible) {
    this.showPropertyName = isPropertyNameVisible;
  }

  public Boolean getUiOnlyProperty() {
    return uiOnlyProperty;
  }

  public void setUiOnlyProperty(Boolean isUiOnlyProperty) {
    this.uiOnlyProperty = isUiOnlyProperty;
  }

  public String getCopy() {
    return copy;
  }

  public void setCopy(String copy) {
    this.copy = copy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ValueAttributesInfo that = (ValueAttributesInfo) o;

    if (entries != null ? !entries.equals(that.entries) : that.entries != null) return false;
    if (entriesEditable != null ? !entriesEditable.equals(that.entriesEditable) : that.entriesEditable != null)
      return false;
    if (emptyValueValid != null ? !emptyValueValid.equals(that.emptyValueValid) : that.emptyValueValid != null)
      return false;
    if (visible != null ? !visible.equals(that.visible) : that.visible != null)
      return false;
    if (readOnly != null ? !readOnly.equals(that.readOnly) : that.readOnly != null)
      return false;
    if (editableOnlyAtInstall != null ? !editableOnlyAtInstall.equals(that.editableOnlyAtInstall) : that.editableOnlyAtInstall != null)
      return false;
    if (overridable != null ? !overridable.equals(that.overridable) : that.overridable != null)
      return false;
    if (hidden != null ? !hidden.equals(that.overridable) : that.hidden != null)
      return false;
    if (showPropertyName != null ? !showPropertyName.equals(that.showPropertyName) : that.showPropertyName != null)
      return false;
    if (uiOnlyProperty != null ? !uiOnlyProperty.equals(that.uiOnlyProperty) : that.uiOnlyProperty != null)
      return false;
    if (copy != null ? !copy.equals(that.copy) : that.copy != null)
      return false;
    if (maximum != null ? !maximum.equals(that.maximum) : that.maximum != null) return false;
    if (minimum != null ? !minimum.equals(that.minimum) : that.minimum != null) return false;
    if (selectionCardinality != null ? !selectionCardinality.equals(that.selectionCardinality) : that.selectionCardinality != null)
      return false;
    if (propertyFileName != null ? !propertyFileName.equals(that.propertyFileName) : that.propertyFileName != null)
      return false;
    if (propertyFileType != null ? !propertyFileType.equals(that.propertyFileType) : that.propertyFileType != null)
      return false;
    if (type != null ? !type.equals(that.type) : that.type != null) return false;
    if (unit != null ? !unit.equals(that.unit) : that.unit != null) return false;
    if (delete != null ? !delete.equals(that.delete) : that.delete != null) return false;
    if (incrementStep != null ? !incrementStep.equals(that.incrementStep) : that.incrementStep != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = type != null ? type.hashCode() : 0;
    result = 31 * result + (hidden != null ? hidden.hashCode() : 0);
    result = 31 * result + (maximum != null ? maximum.hashCode() : 0);
    result = 31 * result + (minimum != null ? minimum.hashCode() : 0);
    result = 31 * result + (unit != null ? unit.hashCode() : 0);
    result = 31 * result + (delete != null ? delete.hashCode() : 0);
    result = 31 * result + (entries != null ? entries.hashCode() : 0);
    result = 31 * result + (entriesEditable != null ? entriesEditable.hashCode() : 0);
    result = 31 * result + (selectionCardinality != null ? selectionCardinality.hashCode() : 0);
    result = 31 * result + (propertyFileName != null ? propertyFileName.hashCode() : 0);
    result = 31 * result + (propertyFileType != null ? propertyFileType.hashCode() : 0);
    result = 31 * result + (incrementStep != null ? incrementStep.hashCode() : 0);
    result = 31 * result + (emptyValueValid != null ? emptyValueValid.hashCode() : 0);
    result = 31 * result + (visible != null ? visible.hashCode() : 0);
    result = 31 * result + (readOnly != null ? readOnly.hashCode() : 0);
    result = 31 * result + (editableOnlyAtInstall != null ? editableOnlyAtInstall.hashCode() : 0);
    result = 31 * result + (overridable != null ? overridable.hashCode() : 0);
    result = 31 * result + (showPropertyName != null ? showPropertyName.hashCode() : 0);
    result = 31 * result + (uiOnlyProperty != null ? uiOnlyProperty.hashCode() : 0);
    result = 31 * result + (copy != null ? copy.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ValueAttributesInfo{" +
      "entries=" + entries +
      ", type='" + type + '\'' +
      ", maximum='" + maximum + '\'' +
      ", minimum='" + minimum + '\'' +
      ", unit='" + unit + '\'' +
      ", delete='" + delete + '\'' +
      ", emptyValueValid='" + emptyValueValid + '\'' +
      ", visible='" + visible + '\'' +
      ", readOnly='" + readOnly + '\'' +
      ", editableOnlyAtInstall='" + editableOnlyAtInstall + '\'' +
      ", overridable='" + overridable + '\'' +
      ", showPropertyName='" + showPropertyName + '\'' +
      ", uiOnlyProperty='" + uiOnlyProperty + '\'' +
      ", incrementStep='" + incrementStep + '\'' +
      ", entriesEditable=" + entriesEditable +
      ", selectionCardinality='" + selectionCardinality + '\'' +
      ", propertyFileName='" + propertyFileName + '\'' +
      ", propertyFileType='" + propertyFileType + '\'' +
      ", copy='" + copy + '\'' +
      '}';
  }
}

