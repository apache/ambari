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
import { Modal } from "react-bootstrap";
import DefaultButton from "../../components/DefaultButton";
import { useContext, useEffect, useState } from "react";
import AppContent from "../../context/AppContext";
import { get, isEmpty, set, startCase } from "lodash";
import UserGroupApi from "../../api/userGroupApi";
import Spinner from "../../components/Spinner";
import { ProcessedRbacDataType } from "./types";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faCheck } from "@fortawesome/free-solid-svg-icons";
import Table from "../../components/Table";

type RoleBasedAccessControlProps = {
  isOpen: boolean;
  onClose: () => void;
};

export default function RoleBasedAccessControl({
  isOpen,
  onClose,
}: RoleBasedAccessControlProps) {
  const { rbacData, setRbacData, permissionLabelList, setPermissionLabelList } =
    useContext(AppContent);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const getUserGroupAccess = async () => {
      setLoading(true);
      const data = await UserGroupApi.getPermissions(
        "PermissionInfo%2F*,authorizations%2FAuthorizationInfo%2F*"
      );
      setRbacData(preProcessRbacData(data));
      setLoading(false);
    };
    if (isEmpty(rbacData)) {
      getUserGroupAccess();
    }
  }, []);

  useEffect(() => {
    setPermissionLabelList(sortBasedOnAccess(permissionLabelList));
  }, [rbacData]);

  const columnInRbacTable = [
    {
      header: <div className="w-50"></div>,
      id: "empty",
      width: "30%",
      cell: (info: any) => {
        return Object.keys(get(info, "row.original", {}))?.[0];
      },
    },
    ...permissionLabelList.map((permissionLabel: string) => ({
      header: <div className="w-50">{permissionLabel}</div>,
      id: permissionLabel,
      cell: (info: any) => {
        if (
          get(
            Object.values(get(info, "row.original")),
            `[0][${permissionLabel}]`,
            false
          )
        ) {
          return (
            <div className="d-flex justify-content-center w-50">
              <FontAwesomeIcon icon={faCheck} className="make-green fs-6" />
            </div>
          );
        }
      },
    })),
  ];

  const sortBasedOnAccess = (data: string[]) => {
    let metaData = data.reduce((acc: any, item: string) => {
      acc[item] = 0;
      return acc;
    }, {});
    Object.values(rbacData)
      .flat()
      .forEach((item: any) => {
        const innerDic: { [key: string]: string } = Object.values(
          item
        )?.[0] as { [key: string]: string };
        Object.keys(innerDic).forEach((key: string) => {
          if (innerDic[key]) {
            metaData[key] = metaData[key] + 1;
          }
        });
      });
    return (Object.entries(metaData) as Array<[string, number]>)
      .sort((a, b) => a[1] - b[1])
      .map((entry) => entry[0]);
  };

  const preProcessRbacData = (data: any) => {
    let permissioLabels: string[] = [];
    let permissionLevelList = new Set<string>();
    get(data, "items", []).forEach((item: any) => {
      permissioLabels.push(get(item, "PermissionInfo.permission_label"));
      get(item, "authorizations", []).forEach((authorization: any) => {
        const authorizationId = get(
          authorization,
          "AuthorizationInfo.authorization_id",
          ""
        );
        permissionLevelList.add(authorizationId.split(".")?.[0]);
      });
    });

    setPermissionLabelList(permissioLabels);

    let processedData: ProcessedRbacDataType = {};

    permissionLevelList.forEach((permission: string) => {
      set(processedData, permission, []);
      get(data, "items", []).forEach((item: any) => {
        const authorizations = get(item, "authorizations", []).reduce(
          (acc: any, authorization: any) => {
            const authorizationId = get(
              authorization,
              "AuthorizationInfo.authorization_id",
              ""
            );
            if (authorizationId.split(".")?.[0] === permission) {
              acc.push(
                get(authorization, "AuthorizationInfo.authorization_name", "")
              );
            }
            return acc;
          },
          []
        );
        processedData[permission].push({
          [get(item, "PermissionInfo.permission_label")]: authorizations,
        });
      });
    });

    Object.keys(processedData).forEach((processedDataKey: string) => {
      const combinedList = processedData[processedDataKey].reduce(
        (acc: string[], obj: { [key: string]: string[] }) => {
          const values = Object.values(obj)[0];
          return [...new Set([...acc, ...values])];
        },
        []
      );

      const combinedData = combinedList.reduce(
        (acc: any, obj: string, idx: number) => {
          acc[obj] = combinedList[idx];
          return acc;
        },
        {}
      );

      processedData[processedDataKey].forEach((element) => {
        const role = Object.keys(element)[0];
        const permissions = element[role];

        element[role] = combinedList.reduce((obj: any, key: string) => {
          obj[key] = permissions.includes(key);
          return obj;
        }, {});
      });

      processedData[processedDataKey] = [
        { "": combinedData },
        ...processedData[processedDataKey],
      ];

      processedData[processedDataKey] = Object.keys(
        processedData[processedDataKey][0][""]
      ).map((key: string) => {
        const obj: any = {};
        processedData[processedDataKey].slice(1).forEach((item: any) => {
          const role = Object.keys(item)[0];
          obj[role] = item[role][key];
        });
        return { [key]: obj };
      });
    });

    return processedData;
  };

  return (
    <Modal
      show={isOpen}
      onHide={onClose}
      size="xl"
      className="custom-modal-container"
      data-testid="role-based-access-control-modal"
    >
      <Modal.Header closeButton>
        <Modal.Title>Role Based Access Control</Modal.Title>
      </Modal.Header>
      {loading ? (
        <Spinner />
      ) : (
        <div>
          <Modal.Body className="scrollable">
            {Object.keys(rbacData).map((rbacDataKey: string) => {
              return (
                <div key={rbacDataKey} className="p-2 mb-4">
                  <h6 key={rbacDataKey}>
                    {startCase(rbacDataKey.toLowerCase()) +
                      "-level Permissions"}
                  </h6>
                  <Table
                    columns={columnInRbacTable}
                    data={rbacData[rbacDataKey]}
                    striped={true}
                    hover={true}
                  />
                </div>
              );
            })}
          </Modal.Body>
          <Modal.Footer className="d-flex justify-content-center">
            <DefaultButton onClick={onClose}>CLOSE</DefaultButton>
          </Modal.Footer>
        </div>
      )}
    </Modal>
  );
}
