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
import { useContext, useEffect, useState } from "react";
import { Button, Col, Form, InputGroup, Row } from "react-bootstrap";
import ViewsInformationApi from "../../api/viewsApiInfo";
import { cloneDeep, isEmpty } from "lodash";
import ReactSelect from "react-select";
import { useHistory, useParams } from "react-router-dom";
import { get } from "lodash";
import Spinner from "../../components/Spinner";
import AppContent from "../../context/AppContext";
import toast from "react-hot-toast";

type ParamsType = {
  view_name: string;
  version: string;
  instance_name: string;
};

type OptionsType = {
  label: string;
  value: any;
};

type FormDataType = {
  name: string;
  view: string;
  instance: string;
  shortUrl: string;
  [key: string]: string;
};

export default function CreateShortUrl() {
  const [loading, setLoading] = useState(false);
  const history = useHistory();

  const [viewOptions, setViewOptions] = useState<OptionsType[]>([]);
  const [selectedView, setSelectedView] = useState<OptionsType>();
  const [instanceOptions, setInstanceOptions] = useState<OptionsType[]>([]);
  const [selectedInstance, setSelectedInstance] = useState<OptionsType | null>(
    null
  );
  const [submittedOnce, setSubmittedonce] = useState(false);

  const params = useParams<ParamsType>();

  const [formData, setFormData] = useState<FormDataType>({
    name: "",
    view: "",
    instance: "",
    shortUrl: "",
  });

  const [nameValid, setNameValid] = useState(false);
  const [urlValid, setUrlValid] = useState(false);

  const {
    setSelectedOption
  } = useContext(AppContent);



  const handleValueChange = (key: string, value: string) => {
    setSelectedOption("Views");
    const formDataCopy = cloneDeep(formData);
    formDataCopy[key] = value;
    if (formDataCopy.name) {
      setNameValid(true);
    } else {
      setNameValid(false);
    }

    if (formDataCopy.shortUrl) {
      const isValidPattern = (str: string): boolean =>
        /^[a-z0-9-_]+$/.test(str);
      const minLengthCheck = formDataCopy.shortUrl.length >= 2;
      const maxLengthCheck = formDataCopy.shortUrl.length <= 25;
      setUrlValid(isValidPattern(formDataCopy.shortUrl) && minLengthCheck && maxLengthCheck);
    } else {
      setUrlValid(false);
    }

    setFormData(formDataCopy);
  };

  useEffect(() => {
    async function getViewsList() {
      setLoading(true);
      const data: any = await ViewsInformationApi.viewsListAPI();
      const viewOptionsLocal = [];
      for (const viewOption of data.items) {
        for (const viewOptionVersion of viewOption.versions) {
          viewOptionsLocal.push({
            label: `${get(viewOption, "ViewInfo.view_name")} (${get(
              viewOptionVersion,
              "ViewVersionInfo.version"
            )})`,
            value: get(viewOptionVersion, "instances", []),
          });
        }
      }

      const currentViewNameMapping = params.view_name;
      const currentViewVersionMapping = params.version;
      const currentViewInstanceMapping = params.instance_name;

      const selectedViewMapping = viewOptionsLocal.find(
        (view: any) =>
          view.label ===
          `${currentViewNameMapping} (${currentViewVersionMapping})`
      );
      setSelectedView(selectedViewMapping);

      const selectedViewInstanceMapping = selectedViewMapping?.value.find(
        (instance: any) =>
          instance.ViewInstanceInfo.instance_name === currentViewInstanceMapping
      );
      setSelectedInstance({
        label: get(
          selectedViewInstanceMapping,
          "ViewInstanceInfo.instance_name"
        ),
        value: get(
          selectedViewInstanceMapping,
          "ViewInstanceInfo.instance_name"
        ),
      });

      if (selectedViewMapping)
        handleValueChange("view", selectedViewMapping?.label);
      handleValueChange(
        "instance",
        selectedViewInstanceMapping?.ViewInstanceInfo?.instance_name
      );

      setViewOptions(viewOptionsLocal);

      setLoading(false);
    }
    getViewsList();
  }, []);

  useEffect(() => {
    if (!isEmpty(selectedView)) {
      //   setSelectedInstance({});
      //Check if selected instance does not exist in value options of the selected view
      const selectedInstanceExists = selectedView.value.find(
        (instance: any) =>
          instance.ViewInstanceInfo.instance_name === selectedInstance?.value
      );
      if (!selectedInstanceExists) {
        setSelectedInstance(null);
      }
      const instancesList = selectedView.value;
      setInstanceOptions(
        instancesList.map((instance: any) => {
          return {
            label: instance.ViewInstanceInfo.instance_name,
            value: instance.ViewInstanceInfo.instance_name,
          };
        })
      );

      if (selectedInstance)
        handleValueChange("instance", selectedInstance?.label);
    }
  }, [selectedView]);

  const setValues = async () => {
    const data: any = {
      ViewUrlInfo: {
        url_name: formData.name,
        url_suffix: formData.shortUrl,
        view_instance_version:
          formData.view !== "" ? formData.view.split(" ")[0] : params.version,
        view_instance_name: formData.instance,
        view_instance_common_name:
          formData.view !== "" ? formData.view.split(" ")[0] : params.view_name,
      },
    };

    if (nameValid && urlValid) {
      try{
        await ViewsInformationApi.createShortUrl(formData.name, data);
        toast.success("URL created successfully");
        history.push("/views");
      }catch{
        toast.error("Error creating URL");
      }
     
    }
  };

  if(loading) {
    return <Spinner/>
  }

  return (
    <div className="mx-2 my-2">
      <div className="border-bottom">
        <h4>Create New URL</h4>
      </div>
      <div>
        <Form>
          <Form.Group as={Row} className="mt-3">
            <Form.Label column sm="2">
              Name
            </Form.Label>
            <Col sm="10">
              <Form.Control
                type="text"
                placeholder="Name"
                required
                isValid={submittedOnce && nameValid}
                isInvalid={submittedOnce && !nameValid}
                onChange={(e) => handleValueChange("name", e.target.value)}
                data-testid = "name-input"
              ></Form.Control>
              <Form.Control.Feedback type="invalid">
                This field is required
              </Form.Control.Feedback>
            </Col>
          </Form.Group>
          <Form.Group as={Row} className="mt-3">
            <Form.Label column sm="2">
              View
            </Form.Label>
            <Col sm="10">
              <ReactSelect
                options={viewOptions}
                value={selectedView}
                onChange={(value: any) => {
                  setSelectedView(value);
                  handleValueChange("view", value?.label);
                }}
                className={selectedView ? "" : "is-invalid"}
                aria-label="view-select"
              />
               {!selectedView && <div className="invalid-feedback">This is required</div>}
            </Col>
          </Form.Group>

          <Form.Group as={Row} className="mt-3">
            <Form.Label column sm="2">
              Instance
            </Form.Label>
            <Col sm="10">
              <ReactSelect
                options={instanceOptions}
                value={selectedInstance}
                required
                onChange={(value) => {
                  setSelectedInstance(value);
                  handleValueChange("instance", value ? value.label : "");
                }}
                className={selectedInstance ? "" : "is-invalid"}
                aria-label="instance-select"
                
              />
              {!selectedInstance && <div className="invalid-feedback">This is required</div>}
            </Col>
          </Form.Group>
          <Form.Group as={Row} className="mt-3">
            <Form.Label column sm="2">
              Short URL
            </Form.Label>
            <Col sm="10">
              <InputGroup className="align-items-center">
                <InputGroup.Text id="inputGroupPrepend">
                  /main/view/{selectedView?.label.split(" ")[0]}
                </InputGroup.Text>
                <Form.Control
                  type="text"
                  placeholder="Short URL"
                  aria-describedby="inputGroupPrepend"
                  className="w-50"
                  required
                  isValid={submittedOnce && urlValid}
                  isInvalid={submittedOnce && !urlValid}
                  onChange={(e) =>
                    handleValueChange("shortUrl", e.target.value)
                  }
                  data-testid = "shorturl-input"
                />
                <Form.Control.Feedback type="invalid">
                  {[
                    !formData.shortUrl && "This field is required",
                    formData.shortUrl &&
                      formData.shortUrl.length < 2 &&
                      "Length should be at least 2 characters",
                    formData.shortUrl &&
                      formData.shortUrl.length > 25 &&
                      "Length should not exceed 25 characters",
                    formData.shortUrl &&
                      !/^[a-z0-9-_]+$/.test(formData.shortUrl) &&
                      "Only lowercase alphanumeric characters, hyphens, and underscores are allowed",
                  ]
                    .filter(Boolean)
                    .join(", ")}
                </Form.Control.Feedback>
              </InputGroup>
            </Col>
          </Form.Group>
        </Form>
      </div>
      <div className="d-flex justify-content-end mt-3">
        <Button
          size="sm"
          className="ms-1"
          variant="success"
          onClick={() => {
            setSubmittedonce(true);
            if (nameValid && urlValid && selectedInstance && selectedView) {
              setValues();
            }
          }}
        >
          SAVE
        </Button>
      </div>
    </div>
  );
}
