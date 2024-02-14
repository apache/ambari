import { DisplayProcessor } from "../display-processor";
export declare class SpecColorsProcessor extends DisplayProcessor {
    displaySuccessfulSpec(spec: any, log: String): String;
    displayFailedSpec(spec: any, log: String): String;
    displayPendingSpec(spec: any, log: String): String;
}
