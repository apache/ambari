import { DisplayProcessor } from "../display-processor";
export declare class SuiteNumberingProcessor extends DisplayProcessor {
    private static getParentName(element);
    private suiteHierarchy;
    displaySuite(suite: any, log: String): String;
    private computeNumber(suite);
    private computeHierarchy(suite);
    private computeHierarchyNumber();
}
