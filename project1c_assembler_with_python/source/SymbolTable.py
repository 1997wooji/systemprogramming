'''
symbol과 관련된 데이터와 연산을 소유한다.
section 별로 하나씩 인스턴스를 할당한다.
'''
class SymbolTable : 

    def __init__(self) : 
        self.symbolList = []  # 이거 hashmap으로 했어도 괜찮았지 않을까?
        self.locationList = [] # list도 nonetype error를 막기 위해

    '''
	새로운 Symbol을 table에 추가한다.
	@param symbol : 새로 추가되는 symbol의 label
	@param location : 해당 symbol이 가지는 주소값
	주의 : 만약 중복된 symbol이 putSymbol을 통해서 입력된다면 이는 프로그램 코드에 문제가 있음을 나타낸다. 
	매칭되는 주소값의 변경은 modifySymbol()을 통해서 이루어져야 한다.
	'''
    def putSymbol(self, symbol, location) : 
        if (symbol in self.symbolList) == False : # symbol이 없을때만
            self.symbolList.append(symbol)
            self.locationList.append(location)

    '''
	기존에 존재하는 symbol 값에 대해서 가리키는 주소값을 변경한다.
	@param symbol : 변경을 원하는 symbol의 label
     @param newLocation : 새로 바꾸고자 하는 주소값
    '''
    def modifySymbol(self, symbol, newLocation) :
        if (symbol in self.symbolList) == False : # symbol이 없으면 돌아간다
            return 

        index = self.symbolList.index(symbol)
        self.locationList[index] = newLocation

    '''
    인자로 전달된 symbol이 어떤 주소를 지칭하는지 알려준다. 
	@param symbol : 검색을 원하는 symbol의 label
	@return symbol이 가지고 있는 주소값. 해당 symbol이 없을 경우 -1 리턴
    '''
    def search(self, symbol) : 
        if symbol in self.symbolList : 
            index = self.symbolList.index(symbol)
            return self.locationList[index]
        else : 
            return -1

