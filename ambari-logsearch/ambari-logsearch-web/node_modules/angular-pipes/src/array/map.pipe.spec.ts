import { MapPipe } from './map.pipe';


describe('MapPipe', () => {
    
    let pipe: MapPipe;
    
    beforeEach(() => {
       pipe = new MapPipe(); 
    });
    
    it('Should return the modified array', () => {
       
       const array = [0, 1, 2, 3];
       const fn = function (item: any) {
           return ++item;
       };
       
       expect(pipe.transform(array, fn)).toEqual([1, 2, 3, 4]); 
       expect(array).toEqual([0, 1, 2, 3]); // Check integrity
    });
    
    it('Should return the original array', () => {
       
       // undefined to avoid typescript error
       expect(pipe.transform([1,2], undefined)).toEqual([1, 2]); 
    });
    
    it('Should return the value unchanged', () => {
       
       expect(pipe.transform('a', null)).toEqual('a'); 
    });
    
})