import { AbstractRule } from "./language/rule/abstractRule";
import { IRule } from "./language/rule/rule";
export interface IEnableDisablePosition {
    isEnabled: boolean;
    position: number;
}
export declare function loadRules(ruleConfiguration: {
    [name: string]: any;
}, enableDisableRuleMap: {
    [rulename: string]: IEnableDisablePosition[];
}, rulesDirectories?: string | string[], isJs?: boolean): IRule[];
export declare function findRule(name: string, rulesDirectories?: string | string[]): typeof AbstractRule | null;
