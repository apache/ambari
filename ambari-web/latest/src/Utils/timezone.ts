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

import dayjs from "dayjs";
import utc from "dayjs/plugin/utc";
import timezone from "dayjs/plugin/timezone";

dayjs.extend(utc);
dayjs.extend(timezone);

interface FormattedTimezone {
  groupByKey: string;
  utcOffset: number;
  formattedOffset: string;
  value: string;
  region: string;
  city: string;
}

interface ShownTimezone {
  utcOffset: number;
  label: string;
  value: string;
  zones: FormattedTimezone[];
}

interface TimezoneInfo {
  name: string;
  abbreviation: string;
}

const TIMEZONES: TimezoneInfo[] = [
  // Africa
  { name: "Africa/Abidjan", abbreviation: "GMT" },
  { name: "Africa/Accra", abbreviation: "GMT" },
  { name: "Africa/Algiers", abbreviation: "CET" },
  { name: "Africa/Bissau", abbreviation: "GMT" },
  { name: "Africa/Cairo", abbreviation: "EET" },
  { name: "Africa/Casablanca", abbreviation: "WET" },
  { name: "Africa/Ceuta", abbreviation: "CET" },
  { name: "Africa/Johannesburg", abbreviation: "SAST" },
  { name: "Africa/Lagos", abbreviation: "WAT" },
  { name: "Africa/Maputo", abbreviation: "CAT" },
  { name: "Africa/Monrovia", abbreviation: "GMT" },
  { name: "Africa/Nairobi", abbreviation: "EAT" },
  { name: "Africa/Ndjamena", abbreviation: "WAT" },
  { name: "Africa/Tripoli", abbreviation: "EET" },
  { name: "Africa/Tunis", abbreviation: "CET" },
  { name: "Africa/Windhoek", abbreviation: "CAT" },

  // America
  { name: "America/Adak", abbreviation: "HST" },
  { name: "America/Anchorage", abbreviation: "AKST" },
  { name: "America/Araguaina", abbreviation: "BRT" },
  { name: "America/Argentina/Buenos_Aires", abbreviation: "ART" },
  { name: "America/Asuncion", abbreviation: "PYT" },
  { name: "America/Bahia", abbreviation: "BRT" },
  { name: "America/Barbados", abbreviation: "AST" },
  { name: "America/Belem", abbreviation: "BRT" },
  { name: "America/Belize", abbreviation: "CST" },
  { name: "America/Bogota", abbreviation: "COT" },
  { name: "America/Cancun", abbreviation: "EST" },
  { name: "America/Caracas", abbreviation: "VET" },
  { name: "America/Chicago", abbreviation: "CST" },
  { name: "America/Chihuahua", abbreviation: "MST" },
  { name: "America/Costa_Rica", abbreviation: "CST" },
  { name: "America/Dawson_Creek", abbreviation: "MST" },
  { name: "America/Denver", abbreviation: "MST" },
  { name: "America/Edmonton", abbreviation: "MST" },
  { name: "America/El_Salvador", abbreviation: "CST" },
  { name: "America/Fortaleza", abbreviation: "BRT" },
  { name: "America/Godthab", abbreviation: "WGT" },
  { name: "America/Guatemala", abbreviation: "CST" },
  { name: "America/Guayaquil", abbreviation: "ECT" },
  { name: "America/Halifax", abbreviation: "AST" },
  { name: "America/Havana", abbreviation: "CST" },
  { name: "America/Indianapolis", abbreviation: "EST" },
  { name: "America/Juneau", abbreviation: "AKST" },
  { name: "America/La_Paz", abbreviation: "BOT" },
  { name: "America/Lima", abbreviation: "PET" },
  { name: "America/Los_Angeles", abbreviation: "PST" },
  { name: "America/Managua", abbreviation: "CST" },
  { name: "America/Manaus", abbreviation: "AMT" },
  { name: "America/Matamoros", abbreviation: "CST" },
  { name: "America/Mazatlan", abbreviation: "MST" },
  { name: "America/Mexico_City", abbreviation: "CST" },
  { name: "America/Monterrey", abbreviation: "CST" },
  { name: "America/Montevideo", abbreviation: "UYT" },
  { name: "America/Nassau", abbreviation: "EST" },
  { name: "America/New_York", abbreviation: "EST" },
  { name: "America/Noronha", abbreviation: "FNT" },
  { name: "America/Panama", abbreviation: "EST" },
  { name: "America/Phoenix", abbreviation: "MST" },
  { name: "America/Port_of_Spain", abbreviation: "AST" },
  { name: "America/Port-au-Prince", abbreviation: "EST" },
  { name: "America/Puerto_Rico", abbreviation: "AST" },
  { name: "America/Regina", abbreviation: "CST" },
  { name: "America/Rio_Branco", abbreviation: "ACT" },
  { name: "America/Santiago", abbreviation: "CLT" },
  { name: "America/Santo_Domingo", abbreviation: "AST" },
  { name: "America/Sao_Paulo", abbreviation: "BRT" },
  { name: "America/St_Johns", abbreviation: "NST" },
  { name: "America/Tegucigalpa", abbreviation: "CST" },
  { name: "America/Tijuana", abbreviation: "PST" },
  { name: "America/Toronto", abbreviation: "EST" },
  { name: "America/Vancouver", abbreviation: "PST" },
  { name: "America/Winnipeg", abbreviation: "CST" },

  // Asia
  { name: "Asia/Amman", abbreviation: "EET" },
  { name: "Asia/Baghdad", abbreviation: "AST" },
  { name: "Asia/Bahrain", abbreviation: "AST" },
  { name: "Asia/Bangkok", abbreviation: "ICT" },
  { name: "Asia/Beirut", abbreviation: "EET" },
  { name: "Asia/Calcutta", abbreviation: "IST" },
  { name: "Asia/Colombo", abbreviation: "IST" },
  { name: "Asia/Damascus", abbreviation: "EET" },
  { name: "Asia/Dhaka", abbreviation: "BST" },
  { name: "Asia/Dubai", abbreviation: "GST" },
  { name: "Asia/Hong_Kong", abbreviation: "HKT" },
  { name: "Asia/Irkutsk", abbreviation: "IRKT" },
  { name: "Asia/Jakarta", abbreviation: "WIB" },
  { name: "Asia/Jerusalem", abbreviation: "IST" },
  { name: "Asia/Kabul", abbreviation: "AFT" },
  { name: "Asia/Karachi", abbreviation: "PKT" },
  { name: "Asia/Kathmandu", abbreviation: "NPT" },
  { name: "Asia/Kolkata", abbreviation: "IST" },
  { name: "Asia/Krasnoyarsk", abbreviation: "KRAT" },
  { name: "Asia/Kuala_Lumpur", abbreviation: "MYT" },
  { name: "Asia/Kuwait", abbreviation: "AST" },
  { name: "Asia/Magadan", abbreviation: "MAGT" },
  { name: "Asia/Manila", abbreviation: "PHT" },
  { name: "Asia/Muscat", abbreviation: "GST" },
  { name: "Asia/Nicosia", abbreviation: "EET" },
  { name: "Asia/Qatar", abbreviation: "AST" },
  { name: "Asia/Rangoon", abbreviation: "MMT" },
  { name: "Asia/Riyadh", abbreviation: "AST" },
  { name: "Asia/Seoul", abbreviation: "KST" },
  { name: "Asia/Shanghai", abbreviation: "CST" },
  { name: "Asia/Singapore", abbreviation: "SGT" },
  { name: "Asia/Taipei", abbreviation: "CST" },
  { name: "Asia/Tehran", abbreviation: "IRST" },
  { name: "Asia/Tokyo", abbreviation: "JST" },
  { name: "Asia/Vladivostok", abbreviation: "VLAT" },
  { name: "Asia/Yakutsk", abbreviation: "YAKT" },
  { name: "Asia/Yekaterinburg", abbreviation: "YEKT" },
  { name: "Asia/Yerevan", abbreviation: "AMT" },

  // Atlantic
  { name: "Atlantic/Azores", abbreviation: "AZOT" },
  { name: "Atlantic/Bermuda", abbreviation: "AST" },
  { name: "Atlantic/Canary", abbreviation: "WET" },
  { name: "Atlantic/Cape_Verde", abbreviation: "CVT" },
  { name: "Atlantic/Reykjavik", abbreviation: "GMT" },

  // Australia
  { name: "Australia/Adelaide", abbreviation: "ACST" },
  { name: "Australia/Brisbane", abbreviation: "AEST" },
  { name: "Australia/Darwin", abbreviation: "ACST" },
  { name: "Australia/Hobart", abbreviation: "AEST" },
  { name: "Australia/Melbourne", abbreviation: "AEST" },
  { name: "Australia/Perth", abbreviation: "AWST" },
  { name: "Australia/Sydney", abbreviation: "AEST" },

  // Europe
  { name: "Europe/Amsterdam", abbreviation: "CET" },
  { name: "Europe/Athens", abbreviation: "EET" },
  { name: "Europe/Belgrade", abbreviation: "CET" },
  { name: "Europe/Berlin", abbreviation: "CET" },
  { name: "Europe/Brussels", abbreviation: "CET" },
  { name: "Europe/Bucharest", abbreviation: "EET" },
  { name: "Europe/Budapest", abbreviation: "CET" },
  { name: "Europe/Copenhagen", abbreviation: "CET" },
  { name: "Europe/Dublin", abbreviation: "GMT" },
  { name: "Europe/Helsinki", abbreviation: "EET" },
  { name: "Europe/Istanbul", abbreviation: "TRT" },
  { name: "Europe/Kaliningrad", abbreviation: "EET" },
  { name: "Europe/Kiev", abbreviation: "EET" },
  { name: "Europe/Lisbon", abbreviation: "WET" },
  { name: "Europe/London", abbreviation: "GMT" },
  { name: "Europe/Madrid", abbreviation: "CET" },
  { name: "Europe/Malta", abbreviation: "CET" },
  { name: "Europe/Minsk", abbreviation: "MSK" },
  { name: "Europe/Moscow", abbreviation: "MSK" },
  { name: "Europe/Paris", abbreviation: "CET" },
  { name: "Europe/Prague", abbreviation: "CET" },
  { name: "Europe/Riga", abbreviation: "EET" },
  { name: "Europe/Rome", abbreviation: "CET" },
  { name: "Europe/Sofia", abbreviation: "EET" },
  { name: "Europe/Stockholm", abbreviation: "CET" },
  { name: "Europe/Tallinn", abbreviation: "EET" },
  { name: "Europe/Vienna", abbreviation: "CET" },
  { name: "Europe/Vilnius", abbreviation: "EET" },
  { name: "Europe/Warsaw", abbreviation: "CET" },
  { name: "Europe/Zurich", abbreviation: "CET" },

  // Pacific
  { name: "Pacific/Auckland", abbreviation: "NZST" },
  { name: "Pacific/Fiji", abbreviation: "FJT" },
  { name: "Pacific/Guam", abbreviation: "ChST" },
  { name: "Pacific/Honolulu", abbreviation: "HST" },
  { name: "Pacific/Midway", abbreviation: "SST" },
  { name: "Pacific/Noumea", abbreviation: "NCT" },
  { name: "Pacific/Pago_Pago", abbreviation: "SST" },
  { name: "Pacific/Port_Moresby", abbreviation: "PGT" },
  { name: "Pacific/Tongatapu", abbreviation: "TOT" },
];

export const timezoneNames: string[] = TIMEZONES.map((tz) => tz.name);

export const timezoneAbbreviations: Record<string, string> = TIMEZONES.reduce(
  (acc, tz) => {
    acc[tz.name] = tz.abbreviation;
    return acc;
  },
  {} as Record<string, string>
);

export const timezoneData: TimezoneInfo[] = TIMEZONES;

export const getTimezoneAbbreviation = (timezoneName: string): string => {
  const timezone = TIMEZONES.find((tz) => tz.name === timezoneName);
  return timezone?.abbreviation || "UTC";
};

export const getTimezoneByAbbreviation = (
  abbreviation: string
): string | undefined => {
  const timezone = TIMEZONES.find((tz) => tz.abbreviation === abbreviation);
  return timezone?.name;
};

export const getTimezonesByRegion = (region: string): TimezoneInfo[] => {
  return TIMEZONES.filter((tz) => tz.name.startsWith(region + "/"));
};

export const getAllTimezoneNames = (): string[] => {
  let timezones = timezoneNames;
  // Try to use Intl.supportedValuesOf if available in the browser
  try {
    // @ts-ignore - TypeScript might not recognize this newer API
    if (typeof Intl.supportedValuesOf === "function") {
      // @ts-ignore
      const supportedTimezones = Intl.supportedValuesOf("timeZone");
      if (Array.isArray(supportedTimezones) && supportedTimezones.length > 0) {
        // Use the browser's supported timezones if available
        // @ts-ignore
        timezones = supportedTimezones;
      }
    }
  } catch (e) {
    console.warn(
      "Intl.supportedValuesOf not available, using fallback timezone list"
    );
  }
  return timezones.filter((timeZoneName: string) => {
    return (
      timeZoneName.indexOf("Etc/") !== 0 &&
      timeZoneName !== timeZoneName.toUpperCase()
    );
  });
};

const groupPropertyValues = (collection: any, key: string) => {
  const group: { [key: string]: any[] } = {};
  collection.forEach((item: any) => {
    const value = item[key];
    if (!group[value]) {
      group[value] = [item];
    } else {
      group[value].push(item);
    }
  });
  return group;
};

export const groupTimezones = (zones: FormattedTimezone[]): ShownTimezone[] => {
  const groupedByOffset = groupPropertyValues(zones, "groupByKey");
  const newZones: ShownTimezone[] = [];

  Object.keys(groupedByOffset).forEach((offset) => {
    const groupedByRegion = groupPropertyValues(
      groupedByOffset[offset],
      "region"
    );

    Object.keys(groupedByRegion).forEach((region) => {
      const cities = groupedByRegion[region]
        .map((zone) => zone.city)
        .filter((city) => city !== "" && city !== city.toUpperCase())
        .filter((city, index, self) => self.indexOf(city) === index) // unique cities
        .join(", ");

      const firstZone = groupedByRegion[region][0];
      const formattedOffset = firstZone.formattedOffset;
      const utcOffset = firstZone.utcOffset;
      const value = `${firstZone.groupByKey}|${region}`;
      const abbr = getTimezoneAbbreviation(firstZone.value);

      newZones.push({
        utcOffset: utcOffset,
        label: `(UTC${formattedOffset} ${abbr}) ${region}${
          cities ? " / " + cities : ""
        }`,
        value: value,
        zones: groupedByRegion[region],
      });
    });
  });

  return newZones.sort((a, b) => a.utcOffset - b.utcOffset);
};

export const parseTimezones = (): ShownTimezone[] => {
  const currentYear = new Date().getFullYear();
  const jan = new Date(currentYear, 0, 1);
  const jul = new Date(currentYear, 6, 1);

  const zones = getAllTimezoneNames()
    .map((timeZoneName) => {
      const zone = dayjs().tz(timeZoneName);

      if (!zone.isValid()) {
        return null;
      }

      const offset = zone.format("Z");
      const regionCity = timeZoneName.split("/");
      const region = regionCity[0];
      const city = regionCity.length === 2 ? regionCity[1] : "";

      const janOffset = dayjs(jan).tz(timeZoneName).utcOffset();
      const julOffset = dayjs(jul).tz(timeZoneName).utcOffset();

      return {
        groupByKey: `${janOffset}${julOffset}`,
        utcOffset: zone.utcOffset(),
        formattedOffset: offset,
        value: timeZoneName,
        region: region,
        city: city.replace(/_/g, " "),
      };
    })
    .filter((zone): zone is FormattedTimezone => zone !== null)
    .sort((zoneA, zoneB) => {
      if (zoneA.utcOffset === zoneB.utcOffset) {
        if (zoneA.value === zoneB.value) {
          return 0;
        }
        return zoneA.value < zoneB.value ? -1 : 1;
      } else {
        return zoneA.utcOffset < zoneB.utcOffset ? -1 : 1;
      }
    });

  return groupTimezones(zones);
};

export const getTimezones = (): ShownTimezone[] => {
  return parseTimezones();
};

export const getTimezonesMappedByValue = (): Record<string, ShownTimezone> => {
  const ret: Record<string, ShownTimezone> = {};
  parseTimezones().forEach((tz) => {
    ret[tz.value] = tz;
  });
  return ret;
};

export const detectUserTimezone = (region?: string): string => {
  region = (region || "").toLowerCase();
  const currentYear = new Date().getFullYear();
  const jan = new Date(currentYear, 0, 1);
  const jul = new Date(currentYear, 6, 1);
  const janOffset = -jan.getTimezoneOffset();
  const julOffset = -jul.getTimezoneOffset();

  const validZones: string[] = [];

  const timeZones = parseTimezones();
  for (let i = 0; i < timeZones.length; i++) {
    const zones = timeZones[i].zones;
    for (let j = 0; j < zones.length; j++) {
      const tzJanOffset = dayjs(jan).tz(zones[j].value).utcOffset();
      const tzJulOffset = dayjs(jul).tz(zones[j].value).utcOffset();

      if (tzJanOffset === janOffset && tzJulOffset === julOffset) {
        validZones.push(timeZones[i].value);
        break;
      }
    }
  }

  if (validZones.length) {
    if (region) {
      for (let i = 0; i < validZones.length; i++) {
        if (validZones[i].toLowerCase().indexOf(region) !== -1) {
          return validZones[i];
        }
      }
      return validZones[0];
    }
    return validZones[0];
  }
  return "";
};
