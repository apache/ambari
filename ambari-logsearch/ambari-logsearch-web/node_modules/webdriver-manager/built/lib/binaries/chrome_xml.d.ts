import { BinaryUrl } from './binary';
import { XmlConfigSource } from './config_source';
export declare class ChromeXml extends XmlConfigSource {
    constructor();
    getUrl(version: string): Promise<BinaryUrl>;
    /**
     * Get a list of chrome drivers paths available for the configuration OS type and architecture.
     */
    getVersionList(): Promise<string[]>;
    /**
     * Helper method, gets the ostype and gets the name used by the XML
     */
    getOsTypeName(): string;
    /**
     * Gets the latest item from the XML.
     */
    private getLatestChromeDriverVersion();
    /**
     * Gets a specific item from the XML.
     */
    private getSpecificChromeDriverVersion(inputVersion);
}
