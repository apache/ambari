import { Pipe, PipeTransform } from '@angular/core';
import { isArray } from '../utils/utils';

@Pipe({
  name: 'orderBy'
})
export class OrderByPipe implements PipeTransform {
  
  private static _orderBy (a: any, b: any): number {
    
    if (a instanceof Date && b instanceof Date) {
      return (a < b) ? -1 : (a > b) ? 1 : 0;
    }
    
    const floatA = parseFloat(a);
    const floatB = parseFloat(b);
    
    if (typeof a === 'string' && typeof b === 'string' && (isNaN(floatA) || isNaN(floatB))) {
      const lowerA = a.toLowerCase();
      const lowerB = b.toLowerCase();
      return (lowerA < lowerB) ? -1 : (lowerA > lowerB) ? 1 : 0;
    }
    
    return (floatA < floatB) ? -1 : (floatA > floatB) ? 1 : 0; 
  }
  
  transform (input: any, config: any = '+'): any {
    
    if (!isArray(input)) {
      return input;
    }
    
    const configIsArray = isArray(config);
    
    // If config === 'param' OR ['param'] 
    if (!configIsArray || (configIsArray && config.length === 1)) {
      
      const propertyToCheck: string = configIsArray ? config[0] : config;
      const first = propertyToCheck.substr(0, 1);
      const desc = (first === '-'); // First character is '-'
      
      // Basic array (if only + or - is present)
      if (!propertyToCheck || propertyToCheck === '-' || propertyToCheck === '+') {
        return [...input].sort((a: any, b: any) => {
          const comparator = OrderByPipe._orderBy(a, b);
          return desc ? -comparator : comparator; 
        });
      }            
      else {
        // If contains + or -, substring the property
        const property = (first === '+' || desc) ? propertyToCheck.substr(1) : propertyToCheck;
        
        return [...input].sort((a: any, b: any) => {
          
          const comparator = OrderByPipe._orderBy(a[property], b[property]);
          return desc ? -comparator : comparator; 
        });
        
      }
    }
    else { // Config is an array of property
      
      return [...input].sort((a: any, b: any) => {
        
        for (let i: number = 0; i < config.length; ++i) {
          const first = config[i].substr(0, 1);
          const desc = (first === '-');
          const property = (first === '+' || desc) ? config[i].substr(1) : config[i];
          
          const comparator = OrderByPipe._orderBy(a[property], b[property]);
          const comparison = desc ? -comparator : comparator;
          
          if (comparison !== 0) {
            return comparison;
          }
        }
        
        return 0;
      });
      
    }
  }
}