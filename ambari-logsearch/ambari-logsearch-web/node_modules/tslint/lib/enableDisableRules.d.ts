import * as ts from "typescript";
import { IEnableDisablePosition } from "./ruleLoader";
export declare class EnableDisableRulesWalker {
    private sourceFile;
    private enableDisableRuleMap;
    private enabledRules;
    constructor(sourceFile: ts.SourceFile, rules: {
        [name: string]: any;
    });
    getEnableDisableRuleMap(): {
        [rulename: string]: IEnableDisablePosition[];
    };
    private getStartOfLinePosition(position, lineOffset?);
    private switchRuleState(ruleName, isEnabled, start, end?);
    private handleComment(commentText, range);
    private handleTslintLineSwitch(rules, isEnabled, modifier, range);
}
