import { InitialPipe } from './initial.pipe';


describe('InitialPipe', () => {
    
    let pipe: InitialPipe;
    
    beforeEach(() => {
       pipe = new InitialPipe(); 
    });
    
    it('Should return []', () => {
       
       expect(pipe.transform([])).toEqual([]); 
    });
    
    it('Should return [1]', () => {
       
       const value = [1, 2];
       
       expect(pipe.transform(value)).toEqual([1]); 
       expect(value).toEqual([1, 2]); // Check integrity
    });
    
    it ('Should return [1, 2]', () => {
       
       expect(pipe.transform([1, 2, 3])).toEqual([1,2]); 
    });
    
    it('Should return the value unchanged', () => {
       
       expect(pipe.transform('a')).toEqual('a'); 
    });
    
})