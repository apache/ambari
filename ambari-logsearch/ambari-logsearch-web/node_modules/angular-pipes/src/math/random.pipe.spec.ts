import { RandomPipe } from './random.pipe';
        


describe('RandomPipe', () => {
    
    let pipe: RandomPipe;
    
    beforeEach(() => {
       pipe = new RandomPipe(); 
    });
    
    it ('Should return a random value between 5 and 10', () => {
       
       const result = pipe.transform(0, 5, 10);
       
       expect(result).toBeGreaterThan(5); 
       expect(result).toBeLessThan(10); 
    });
    
    it ('Should return a random value between 0 and 1', () => {
       
       const result = pipe.transform(0, 0, 1);
       
       expect(result).toBeGreaterThan(0); 
       expect(result).toBeLessThan(1); 
    });
    
    it ('Should return a random value between 1298 and 1300', () => {
       
       const result = pipe.transform(0, 1298, 1300);
       
       expect(result).toBeGreaterThan(1298); 
       expect(result).toBeLessThan(1300); 
    });
    
    it ('Should return a random value between 0 and 10 #2', () => {
       
       const result = pipe.transform(0, 10);
       
       expect(result).toBeGreaterThan(0); 
       expect(result).toBeLessThan(10); 
    });

    
    it ('Should return the value unchanged', () => {
       
       expect((<any>pipe).transform(0, 'a', 1)).toEqual(0); 
    });
    
    
});