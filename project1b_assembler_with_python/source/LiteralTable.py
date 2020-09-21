'''
literal과 관련된 데이터와 연산을 소유한다.
section 별로 하나씩 인스턴스를 할당한다.
'''
class LiteralTable : 

    def __init__(self) : 
        self.literalList = []
        self.locationList = []
        self.charOrHexList = [] # 해당 literal이 char인지 hex인지 저장한다 

    '''
	새로운 Literal을 table에 추가한다.
	@param literal : 새로 추가되는 literal의 label
	@param location : 해당 literal이 가지는 주소값
	구분을 위해 X인 경우 location은 -1, C인 경우 location은 -2로 넣는다*
	charOrHex도 마찬가지로 구분을 위해 X는 -1, C는 -2로 넣는다
	주의 : 만약 중복된 literal이 putLiteral을 통해서 입력된다면 이는 프로그램 코드에 문제가 있음을 나타낸다. 
	매칭되는 주소값의 변경은 modifyLiteral()을 통해서 이루어져야 한다.
	'''
    def putLiteral(self, literal, location) : 
        if (literal in self.literalList) == False : # 이미 해당 literal이 추가되어 있다면 추가할 필요가 없음 (순서 때문에 괄호 필수)
            self.literalList.append(literal)
            self.locationList.append(location)
            self.charOrHexList.append(location)

    '''
	기존에 존재하는 literal 값에 대해서 가리키는 주소값을 변경한다.
	@param literal : 변경을 원하는 literal의 label
	@param newLocation : 새로 바꾸고자 하는 주소값
    '''
    def modifyLiteral(self, literal, newLocation) :
        if (literal in self.literalList) == False : # symbol이 없으면 돌아간다
            return 

        index = self.literalList.index(literal)
        self.locationList[index] = newLocation

    '''
	인자로 전달된 literal이 어떤 주소를 지칭하는지 알려준다. 
	@param literal : 검색을 원하는 literal의 label
	@return literal이 가지고 있는 주소값. 해당 literal이 없을 경우 -1 리턴
    '''
    def search(self, literal) : 
        if literal in self.literalList : 
            index = self.literalList.index(literal)
            return self.locationList[index]
        else : 
            return -1

    '''
	해당 섹션의 모든 리터럴들에 주소를 할당한다
	@param locctr : literal에 주소를 추가하기 위해 locctr을 인자로 갖고온다
	@return 모든 literal에 주소 할당이 끝난 후 pass1에서 locctr을
	update 해주기 위해 누적된 locctr을 return한다
    '''
    def addAddLiteral(self, locctr) : 
        if self.locationList[0] > 0 : 
            return locctr
           	# 만약 이미 주소가 할당되었다면 돌아가기
			# LTORG, 혹은 END를 만나면 한번에 그 section의 모든 literal을 할당한다.
			# 따라서 이미 하나라도 주소 할당이 되었다면, 모두 주소가 할당된 것이므로 돌아간다.
        index = 0
        for literal in self.literalList : 
            if self.charOrHexList[index] == -1 : 
                # X인 경우
                byteSize = len(literal)/2 # 문자열 함수
            else : # C인 경우
                byteSize = len(literal) 
            
            self.modifyLiteral(literal,locctr) # 해당 literal 주소 수정
            locctr += byteSize
            index += 1 # python은 증감 연산자가 없음
        return locctr # locctr을 반환하여 assember에서의 locctr을  업데이트한다.