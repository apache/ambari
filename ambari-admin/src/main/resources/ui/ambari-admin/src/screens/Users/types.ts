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
interface PrivilegeInfoType {
    permission_label: string;
    permission_name: string;
    principal_name: string;
    principal_type: string;
    privilege_id: number;
    type: string;
    user_name: string;
    [key: string]: string | number;
};

interface PrivilegesType {
    href: string;
    PrivilegeInfo: PrivilegeInfoType;
};

interface UserInfoType {
    href: string;
    Users: {
        active: boolean;
        admin: boolean;
        consecutive_failures: number;
        created: number;
        display_name: string;
        groups: string[];
        ldap_user: boolean;
        local_user_name: string;
        user_name: string;
        user_type: string;
    };
    privileges: PrivilegesType[];
};

interface GroupInfoType {
    href: string;
    Groups: {
        group_name: string;
        group_type: string;
        ldap_group: boolean;
    };
    privileges: {
        href: string;
        PrivilegeInfo: {
            group_name: string;
            privilege_id: number;
        };
    }[];
    members: {
        href: string;
        MemberInfo: {
            group_name: string;
            user_name: string;
        };
    }[];
};

interface GroupNamesType {
    href: string;
    items: {
        href: string;
        Groups: {
            group_name: string;
        };
    }[];
};

interface UserNamesType {
    href: string;
    itemTotal: string;
    items: {
        href: string;
        Users: {
            user_name: string;
        };
    }[];
}

interface UsersListType {
    href: string;
    items: UserInfoType[];
}

interface GroupsListType {
    href: string;
    items: GroupInfoType[];
}

interface SelectOptionType {
    value: string;
    label: string;
}

interface ProcessedRbacDataType {
    [key: string]: { [key: string]: string[] }[];
}

type PermissionLabel = "None" | "Cluster User" | "Cluster Administrator" | "Cluster Operator" | "Service Administrator" | "Service Operator";

export type { UserInfoType, GroupInfoType, GroupNamesType, UserNamesType, UsersListType, GroupsListType, SelectOptionType, PermissionLabel, ProcessedRbacDataType };