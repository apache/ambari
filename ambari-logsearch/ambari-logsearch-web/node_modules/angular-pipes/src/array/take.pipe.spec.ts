import { TakePipe } from './take.pipe';


describe('TakePipe', () => {
    
    let pipe: TakePipe;
    
    beforeEach(() => {
       pipe = new TakePipe(); 
    });
    
    it('Should return []', () => {
       
       expect(pipe.transform([])).toEqual([]); 
    });
    
    it('Should return [1]', () => {
       
       const value = [1, 2, 3, 4];
       
       expect(pipe.transform(value)).toEqual([1]); 
       expect(value).toEqual([1, 2, 3, 4]); // Check integrity
    });
    
    it ('Should return [1, 2]', () => {
       
       expect(pipe.transform([1, 2, 3, 4], 2)).toEqual([1, 2]); 
    });
    
    it('Should return the value unchanged', () => {
       
       expect(pipe.transform('a')).toEqual('a'); 
    });
    
})