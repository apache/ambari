import { Pipe, PipeTransform } from '@angular/core';
import { isNumberFinite } from '../utils/utils';

@Pipe({
  name: 'random'
})
export class RandomPipe implements PipeTransform {
  
  transform (input: any, min: number = 0, max: number = 1): any {
    
    if (!isNumberFinite(min) || !isNumberFinite(max)) {
      return input;
    }
    
    if (min > max) {
      max = min;
      min = 0;
    }
    
    return Math.random() * (max - min) + min;
  }
}