import RegisterTable
import LiteralTable
import InstTable
import SymbolTable

'''
각 라인별로 저장된 코드를 단어 단위로 분할한 후  의미를 해석하는 데에 사용되는 변수와 연산을 정의한다. 
의미 해석이 끝나면 pass2에서 object code로 변형되었을 때의 바이트 코드 역시 저장한다.
'''
class Token : 
    '''
    애초에 공유목적 (정적변수) 로 작동할때만 class 아래에 클래스 변수로 놓고 ,
    객체별로 사용될 변수들은 클래스 변수 자리가 아니라 __init__ 에 넣는게 파이썬 스타일이다.
    이것을 몰라서 operand에 모든 operand들이 붙어 나와서 헤맸다. __init__에서 self.를 이용한 것이 인스턴스 변수.
    '''

    '''
    클래스를 초기화 하면서 바로 line의 의미 분석을 수행한다. 
	@param line 문장단위로 저장된 프로그램 코드
    '''
    def __init__(self,line) :
         # 의미 분석 단계에서 사용되는 변수들
        self.location = 0
        self.label = None
        self.operator = None
        self.operand = [] # 일종의 list
        self.comment = None
        self.nixbpe = 0

         # object code 생성 단계에서 사용되는 변수들 
        self.objectCode = 0
        self.byteSize = 0
        self.parsing(line)

    '''
    line의 실질적인 분석을 수행하는 함수. Token의 각 변수에 분석한 결과를 저장한다.
	@param line 문장단위로 저장된 프로그램 코드.
    '''
    def parsing(self, line) : 
        # Token parsing
        arr = line.split('\t')
        # python의 split은 java나 C의 token 분리와 다르게 "/1/2/3"을 split하면 '', '1', '2', '3'으로 앞에 공백도 그대로 들어간다
        self.label = arr[0]
        self.operator = arr[1]

        try : 
            operands = arr[2].split(",") # operand 저장
            for op in operands : 
                self.operand.append(op)
            # 사실 이 때 operand에 comment가 들어가는 경우가 생긴다. (RSUB같이 operand 개수가 0인 경우)
			# 이것에 대해 고민을 많이 했으나,
			# 추후에 TokenTable에서 operand=0인 경우 operand를 들여다보지 않을 것이므로 괜찮다고 결론지었다.
            # comment는 사용하지 않으므로 저장하지 않는다
        except IndexError : 
            pass

        # flag setting을 token parsing 때 해준다.
        if self.operator[0] == '+' : 
            self.setFlag(1,1) # e=1

        if len(self.operand) > 0 and self.operand[0] != '':
            
            if len(self.operand) > 1 and self.operand[1][0] == 'X' :
                # python은 &&를 and로 쓴다
                # python == 값 비교, is 참조 비교(id)
                self.setFlag(8,1)

            if self.operand[0][0] == '@':
                self.setFlag(32,1) # n=1
            elif self.operand[0][0] == '#':
                self.setFlag(16,1) # i=1
            else : 
                self.setFlag(48,1) # operand 있는 경우, @ # 아니면 n=i=1인 direct access
            # b,p에 대한 값은 추후에 object code를 설정할때 pc relative가 가능한지 보고 결정한다.
        else : 
            self.setFlag(48,1)
            # n=i=1 (operand가 없어도 SIC/XE 명령어이므로, 4형식에 operand symbol이 외부 것이어도)
            # SIC/XE이므로 n=i=1이어야 한다. n=i=0일 순 없음.

    '''
    n,i,x,b,p,e flag를 설정한다. 
    @param flag : 원하는 비트 위치
	@param value : 집어넣고자 하는 값. 1또는 0으로 선언한다.
    '''
    def setFlag(self, flag, value) : 
        if value == 1 : 
            # 1인 경우 bit or
            self.nixbpe |= flag
        else : 
            # 0인 경우 0으로 bit and
            self.nixbpe &= (~flag)
    
    '''
     원하는 flag들의 값을 얻어올 수 있다. flag의 조합을 통해 동시에 여러개의 플래그를 얻는 것 역시 가능하다 
	 @param flags : 값을 확인하고자 하는 비트 위치
	 @return : 비트위치에 들어가 있는 값. 플래그별로 각각 32, 16, 8, 4, 2, 1의 값을 리턴할 것임.
    '''
    def getFlag(self, flags) :
        return self.nixbpe & flags # nixbpe & 32, 48 등 bit and값을 리턴

'''
Modify Record를 미리 저장해놓는 class
Modify Record가 하나씩 나올때마다 생성해놓았다가
추후에 section이 끝날 때 실제로 Record를 적는다
'''
class Modify : 
    def __init__(self,location,length,plus,name) : 
        self.location=location # 고칠 곳의 주소
        self.length=length # 고칠 곳의 길이
        self.plus=plus # '+' or '-'
        self.name=name # 더하거나 뺄 symbol 이름
        self.objectCode='M%06X%02X%c%s'%(int(location),int(length),plus,name) # 형식 지정 modify object code
'''
사용자가 작성한 프로그램 코드를 단어별로 분할 한 후, 의미를 분석하고, 최종 코드로 변환하는 과정을 총괄하는 클래스이다. <br>
pass2에서 object code로 변환하는 과정은 혼자 해결할 수 없고 symbolTable과 instTable의 정보가 필요하므로 이를 링크시킨다.<br>
section 마다 인스턴스가 하나씩 할당된다.
'''
class TokenTable : 
    # final static
    # python은 final, const같은 상수 선언이 없다.
    # 대신 대문자로 이름을 설정하면 값을 바꿀 때 주의를 보낸다
    MAX_OPERAND=3
    # bit 조작의 가독성을 위한 선언
    nFlag=32
    iFlag=16
    xFlag=8
    bFlag=4
    pFlag=2
    eFlag=1

    '''
    초기화하면서 symTable과,literalTable, instTable을 링크시킨다.
    @param symTab : 해당 section과 연결되어있는 symbol table
    @param literalTab : 해당 section과 연결되어있는 literal table
    @param instTab : instruction 명세가 정의된 instTable
    @param registerTab : register name-번호가 정의된 register table
    '''
    def __init__(self,symTab,literalTab,instTab,registerTab) : 
        # Token을 다룰 때 필요한 테이블들을 링크시킨다.
        self.symTab = symTab
        self.literalTab = literalTab
        self.instTab = instTab
        self.registerTab = registerTab
        self.tokenList = [] # 각 line을 의미별로 분할하고 분석하는 공간.
        self.programLength = None  # 각 section별 길이
        self.modifyList = []  # 각 section별 modify record를 저장하는 공간

    '''
    일반 문자열을 받아서 Token단위로 분리시켜 tokenList에 추가한다.
	@param line : 분리되지 않은 일반 문자열
    '''
    def putToken(self,line) : 
        self.tokenList.append(Token(line))

    '''
    tokenList에서 index에 해당하는 Token을 리턴한다.
	@param index
	@return : index번호에 해당하는 코드를 분석한 Token 클래스
    '''
    def getToken(self,index) : 
        return self.tokenList[index]

    '''
    Pass2 과정에서 사용한다.
	instruction table, symbol table literal table 등을 참조하여 objectcode를 생성하고, 이를 저장한다.
	@param index
    '''
    def makeObjectCode(self,index) : 
        # END, LTROG 등 몇몇 경우에는 이 함수를 call하지 않는다. pass2()에서 알아서 처리한다.
		# Token의 object code에 생성한 object code를 저장한다.

        t = self.tokenList[index] # python에서는 자료형 지정하지 않는다
        pc = 0 # pc 값
        disp = 0 # disp 값
        opcode = 0 # opcode 값을 저장

        if t.operator == 'START' or t.operator == 'CSECT' : 
            #Header Record, 시작 주소는 CS program이고, 어디에 올라갈지 모르므로 0일 것이다.
            t.objectCode = 'H%-6s%06X%06X'%(t.label,0,int(self.programLength))
        elif t.operator == 'EXTDEF' : 
            # D record
            t.objectCode = 'D'
            for i in range(len(t.operand)) :
                t.objectCode += '%s%06X'%(t.operand[i], self.symTab.search(t.operand[i]))
        elif t.operator == 'EXTREF' : 
            # R record
            t.objectCode = 'R'
            for i in range(len(t.operand)) : 
                t.objectCode += '%-6s'%(t.operand[i])
        elif t.operator == 'WORD' : 
            # WORD인 경우
			# 상수, 2개의 symbol 뺄셈 연산, 하나의 symbol만 가지는 경우 처리 가능하다.
			# 덧셈 연산은 하지 못한다.
            pc = t.location + t.byteSize
            arr = t.operand[0].split('-')

            # Exception으로 처리한다
            try : 
                disp = int(arr[0]) # 상수인 경우
            except ValueError : 
                # symbol인 경우
                disp = self.symTab.search(arr[0])

                if disp < 0 : # 없는 symbol인 경우
                    disp = 0
                    # Modify unit추가
                    self.modifyList.append(Modify(t.location,6,'+',arr[0]))
                
                if len(arr) > 1 : # 연산자 - 가 있는 경우
                    subDisp = self.symTab.search(arr[1])
                    if subDisp < 0 : # 없는 symbol의 경우
                        subDisp = 0
                        # Modify unit추가
                        self.modifyList.append(Modify(t.location,6,'-',arr[1]))
                
                disp -= subDisp;
                # disp 설정 (만약 이 section에 없는것 - 있는것 일 경우)
                # '-있는것'이 disp로 남게된다.

            if disp < 0 : 
                # 음수인 경우 WORD3byte모두에 대해 처리 해야한다
                disp &= pow(2,24) -1
            t.objectCode = '%06X'%(disp)
        
        elif t.operator == 'BYTE' : 
            # BYTE인 경우 (따로 -,+에 대해 처리 해주지 않았다)
            pc = t.location + t.byteSize
            # C와 X구분
            if t.operand[0][0] == 'X' : 
                # X인 경우
                arr = t.operand[0].split("'") 
                t.objectCode = arr[1] # //X' 분리
            else : 
                # C인 경우, ASCII code로 변환한다
                t.objectCode = ''
                arr = t.operand[0].split("'")
                for i in len(arr[1]) : # arr1 (C'' 떼내고 남은 byte 한 char씩)
                    t.objectCode += '%02X'%(arr[1][i])
        
        else : 
            # Instruction인 경우
            pc = t.location + t.byteSize
            opcode = self.instTab.searchOpcode(t.operator)

            if t.getFlag(1) == 1 : 
                # 4형식인 경우
                # 사칙 연산에 대한 처리를 해주지 못했다
                opcode <<= 4# nixbpe 자리를 만들어주기 위해 shift left 4를 한다.
                opcode |= t.nixbpe # nixbpe bit or
                # 모든 nixbpe를 사용하는 경우에서, base relative에 대한 처리를 해주지 못함. pc relative만 사용한다.
                disp = self.symTab.search(t.operand[0]) # operand의 주소를 찾는다

                if disp < 0 : # 없는 symbol인 경우(EXTREF로 사용한 symbol인 경우)
                    disp = 0
                    self.modifyList.append(Modify(t.location+1,5,'+',t.operand[0])) # modify record를 추가한다 operand 부분만 수정이 필요하다
                else :
                    disp -= pc
                    opcode |= 2 # pc relative, p=1 설정

                if disp < 0 : 
                    # disp가 음수인 경우, int 크기 때문에 20bit로 format을 맞출 수 없다.
                    # 원하는 만큼 자르기 위해 bit and를 이용한다.
                    disp &= pow(2,20) -1
                
                t.objectCode = '%03X%05X'%(opcode,disp)
            
            elif t.byteSize ==2 : 
                # 2형식인 경우, nixbpe를 사용하지 않는다.
                # register에 대해 계산한다.
                t.objectCode = '%02X%X' % (opcode,self.registerTab.getNumber(t.operand[0]))
                if len(t.operand) > 1 : # index Error 방지
                    t.objectCode += '%X' % (self.registerTab.getNumber(t.operand[1]))
                else :
                    t.objectCode += '0'
            
            elif self.instTab.searchNumberOfOperand(t.operator) == 1 : 
                # 3형식이고 operand가 있는 경우
                opcode <<=4 # nixbpe 자리를 만들어주기 위해 shift left 4를 한다.
                opcode |= t.nixbpe # nixbpe bit or

                if t.getFlag(48) == 48 : 
                    #n=i=1인 경우
                    if t.operand[0][0] =='=' : 
                        # literal인 경우
                        arr = t.operand[0].split("'")
                        disp = self.literalTab.search(arr[1]) # LITTAB에서 target address를 찾는다
                        disp -= pc # disp를 구한다
                        opcode |=2  #p=1 설정
                    else : 
                        # 일반 access
                        # 4형식이 아니므로 operand가 symtab에 있어야 한다. (4형식처럼 없으면 안된다.)
                        disp = self.symTab.search(t.operand[0])
                        disp -= pc
                        opcode |= 2 #disp를 구하고 p=1로 설정한다
                elif t.getFlag(16) == 16 : 
                    # immediate인 경우
                    # '# MAXLEN'같은 경우는 처리해주지 않았다.
                    # 상대 주소가 아닌 진짜 값이므로 p=1처리 해주지 않는다.
                   disp = int(t.operand[0][1:]) # '#'는 떼고
                else : 
                    # indirect인 경우(1형식인 경우나 n=i=0인 경우도 여기 들어오지만, 처리하지 못한다.)
                    disp = self.symTab.search(t.operand[0][1:]) # '@'를 떼고
                    disp -=pc
                    opcode |= 2 # 상대 주소이므로 p=1로 처리해준다.

                if disp < 0 :
                    # disp가 음수인 경우, 하위 3column만 들어갈 수 있도록 bit and 해준다.
                    disp &= pow(2,12) -1
                
                t.objectCode = '%03X%03X' % (int(opcode), int(disp))
            
            else : 
                # 3형식이고 operand가 없는 경우
				# 1형식인 경우도 이곳에 포함되지만 처리하지 못함
                opcode <<= 4 # nixbpe 자리를 만들어주기 위해 shift left 4를 한다.
                opcode |= t.nixbpe # nixbpe bit or, p값 설정할 필요 없음

                t.objectCode = '%03X%03X' % (opcode,0) #뒤를 0으로 채운다



    '''
    index번호에 해당하는 object code를 리턴한다.
	@param index
	@return : object code
    '''
    def getObjectCode(self,index) : 
        return self.tokenList[index].objectCode
    




