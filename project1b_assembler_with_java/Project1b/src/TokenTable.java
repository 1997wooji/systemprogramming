import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * 사용자가 작성한 프로그램 코드를 단어별로 분할 한 후, 의미를 분석하고, 최종 코드로 변환하는 과정을 총괄하는 클래스이다. <br>
 * pass2에서 object code로 변환하는 과정은 혼자 해결할 수 없고 symbolTable과 instTable의 정보가 필요하므로 이를 링크시킨다.<br>
 * section 마다 인스턴스가 하나씩 할당된다.
 *
 */
public class TokenTable {
	public static final int MAX_OPERAND=3;
	
	/* bit 조작의 가독성을 위한 선언 */
	public static final int nFlag=32;
	public static final int iFlag=16;
	public static final int xFlag=8;
	public static final int bFlag=4;
	public static final int pFlag=2;
	public static final int eFlag=1;
	
	/* Token을 다룰 때 필요한 테이블들을 링크시킨다. */
	SymbolTable symTab;
	LiteralTable literalTab;
	InstTable instTab;
	RegisterTable registerTab;
	
	/** 각 line을 의미별로 분할하고 분석하는 공간. */
	ArrayList<Token> tokenList;
	
	/** 각 section별 길이 */
	int programLength;
	
	/*각 section별 modify record를 저장하는 공간*/
	ArrayList<Modify> modifyList;
	
	/**
	 * 초기화하면서 symTable과,literalTable, instTable을 링크시킨다.
	 * @param symTab : 해당 section과 연결되어있는 symbol table
	 * @param literalTab : 해당 section과 연결되어있는 literal table
	 * @param instTab : instruction 명세가 정의된 instTable
	 * @param registerTab : register name-번호가 정의된 register table
	 */
	public TokenTable(SymbolTable symTab, LiteralTable literalTab, InstTable instTab, RegisterTable registerTab) {
		
		this.symTab=symTab;
		this.literalTab=literalTab;
		this.instTab=instTab;
		this.registerTab=registerTab;
		tokenList=new ArrayList<Token>();
		modifyList=new ArrayList<Modify>();
	}

	/**
	 * 일반 문자열을 받아서 Token단위로 분리시켜 tokenList에 추가한다.
	 * @param line : 분리되지 않은 일반 문자열
	 */
	public void putToken(String line) {
		tokenList.add(new Token(line));
	}
	
	/**
	 * tokenList에서 index에 해당하는 Token을 리턴한다.
	 * @param index
	 * @return : index번호에 해당하는 코드를 분석한 Token 클래스
	 */
	public Token getToken(int index) {
		return tokenList.get(index);
	}
	
	/**
	 * Pass2 과정에서 사용한다.
	 * instruction table, symbol table literal table 등을 참조하여 objectcode를 생성하고, 이를 저장한다.
	 * @param index
	 */
	public void makeObjectCode(int index){
		// END, LTROG 등 몇몇 경우에는 이 함수를 call하지 않는다. pass2()에서 알아서 처리한다.
		// Token의 object code에 생성한 object code를 저장한다.
		
		Token t = tokenList.get(index);
		//int byteSize = 0; //해당 object Code 길이
		int pc=0; //pc값
		int disp=0; //disp 값을 저장
		int opcode=0; //opcode를 저장
		
		if(t.operator.equals("START") || t.operator.equals("CSECT")){
			//Header Record, 시작 주소는 CS program이고, 어디에 올라갈지 모르므로 0일 것이다.
			t.objectCode = String.format("H%-6s%06X%06X", t.label, 0, programLength);
			
		}else if(t.operator.equals("EXTDEF")){
			//D record
			t.objectCode=new String("D");
			int i=0;
			while(i<MAX_OPERAND && t.operand[i]!=null){
				//Array Index Bound Exception을 막기 위해 i<MAX_OPERAND 조건은 꼭 넣어줘야 한다.
				//내보낼 symbol이름과 주소를 추가한다
				t.objectCode += String.format("%s%06X", t.operand[i],symTab.search(t.operand[i]));
				i++;
			}
			
		}else if(t.operator.equals("EXTREF")){
			//R record
			t.objectCode=new String("R");
			int i=0;
			while(i<MAX_OPERAND && t.operand[i]!=null){
				t.objectCode += String.format("%-6s",t.operand[i]); //주소는 모르므로 symbol만 추가한다
				i++;
			}
			
		}else if(t.operator.equals("WORD")){
			// WORD인 경우
			// 상수, 2개의 symbol 뺄셈 연산, 하나의 symbol만 가지는 경우 처리 가능하다.
			// 덧셈 연산은 하지 못한다.
			
			pc = t.location+t.byteSize;

			StringTokenizer str = new StringTokenizer(t.operand[0],"-");
			String searchStr = str.nextToken();
			try{
				disp = Integer.parseInt(searchStr); //상수인 경우
				
			}catch(NumberFormatException e){
				//symbol인 경우
				disp = symTab.search(searchStr);
				
				if(disp < 0){ //없는 symbol의 경우
					//Modify unit추가
					disp = 0;
					modifyList.add(new Modify(t.location, 6, '+', searchStr)); //WORD이므로 WORD 쨰로 수정이 필요
				}
				
				if(str.hasMoreTokens()){
					// "-"로 분리가되는, 뺄셈 연산이 있었을 경우
					searchStr = str.nextToken();
					int subDisp = symTab.search(searchStr);
					
					if(subDisp < 0){ //없는 symbol의 경우
						//Modify unit추가
						subDisp=0;
						modifyList.add(new Modify(t.location, 6, '-', searchStr)); //WORD이므로 WORD 쨰로 수정이 필요
					}
					disp -= subDisp;
					//disp 설정 (만약 이 section에 없는것 - 있는것 일 경우
					//'-있는것'이 disp로 남게된다.
				}
				
				if(disp < 0){
					disp &= (int)Math.pow(2, 24) -1; //음수인 경우 WORD3byte모두에 대해 처리 해야한다
				}
				
				t.objectCode=String.format("%06X", disp);
			}
			
	
			
		}else if(t.operator.equals("BYTE")){
			//BYTE인 경우 (따로 -,+에 대해 처리 해주지 않았다)
			pc = t.location+t.byteSize;
			//C와 X구분
			if(t.operand[0].charAt(0)=='X'){
				//X인 경우
				StringTokenizer str = new StringTokenizer(t.operand[0],"'");
				str.nextToken(); //X' 분리
				t.objectCode = new String(str.nextToken());
			}else{
				//C인 경우, ASCII code로 변환한다
				t.objectCode = new String("");
				StringTokenizer str = new StringTokenizer(t.operand[0],"'");
				str.nextToken(); //C' 분리
				for(int i=0; i < t.byteSize ; i++){
					t.objectCode += String.format("%02X", (int)str.nextToken().charAt(i));
				}
				
			}
			
		}else{
			//Instruction인 경우
			pc = t.location+t.byteSize;
			opcode = instTab.searchOpcode(t.operator);
			
			if(t.getFlag(1)==1){
				//4형식인 경우
				// 사칙 연산에 대한 처리를 해주지 못했다
				opcode <<= 4; //nixbpe 자리를 만들어주기 위해 shift left 4를 한다.
				opcode |= t.nixbpe; //nixbpe bit or
				//모든 nixbpe를 사용하는 경우에서, base relative에 대한 처리를 해주지 못함. pc relative만 사용한다.
				disp = symTab.search(t.operand[0]); //operand의 주소를 찾는다
				
				if(disp<0){
					disp = 0; //없는 symbol인 경우(EXTREF로 사용한 symbol인 경우)
					//modify record를 추가한다
					modifyList.add(new Modify(t.location+1, 5, '+', t.operand[0])); //operand 부분만 수정이 필요하다
				}else{
					disp -= pc;
					opcode |= 2; //pc relative, p=1 설정
				}
				
				if(disp<0){
					// disp가 음수인 경우, int 크기 때문에 20bit로 format을 맞출 수 없다.
					// 원하는 만큼 자르기 위해 bit and를 이용한다.
					disp &= (int)Math.pow(2, 20) - 1;
				}
				
				t.objectCode=String.format("%03X%05X", opcode,disp);
				
			}else if(t.byteSize == 2){
				//2형식인 경우, nixbpe를 사용하지 않는다.
				//register에 대해 계산한다.
				t.objectCode=String.format("%02X%X%X", opcode, registerTab.getNumber(t.operand[0]),registerTab.getNumber(t.operand[1]));

			}else if(instTab.searchNumberOfOperand(t.operator)==1){
				//3형식이고 operand가 있는 경우
				opcode <<= 4; //nixbpe 자리를 만들어주기 위해 shift left 4를 한다.
				opcode |= t.nixbpe; //nixbpe bit or
				
				if(t.getFlag(48)==48){
					//n=i=1인 경우
					
					if(t.operand[0].charAt(0)=='='){
						//literal인 경우
						StringTokenizer str = new StringTokenizer(t.operand[0],"'");
						str.nextToken(); //=C', =X' 분리
						disp=literalTab.search(str.nextToken()); //LITTAB에서 target address를 찾는다
						disp-=pc; //disp를 구한다
						opcode |=2; // p=1 설정
					}else{
						// 일반 access
						// 4형식이 아니므로 operand가 symtab에 있어야 한다. (4형식처럼 없으면 안된다.)
						disp = symTab.search(t.operand[0]);
						disp -= pc;
						opcode |=2; //disp를 구하고 p=1로 설정한다
					}
				}else if(t.getFlag(16)==16){
					// 이 if문이 먼저 나오면 n=i=1인 경우도 들어가므로 후에 해주어야 한다.
					// immediate인 경우
					// # MAXLEN같은 경우는 처리해주지 않았다.
					// 상대 주소가 아닌 진짜 값이므로 p=1처리 해주지 않는다.
					StringTokenizer str = new StringTokenizer(t.operand[0],"#");
					disp = Integer.parseInt(str.nextToken());
				}else {
					//indirect인 경우(1형식인 경우나 n=i=0인 경우도 여기 들어오지만, 처리하지 못한다.)
					StringTokenizer str = new StringTokenizer(t.operand[0],"@");
					disp = symTab.search(str.nextToken());
					disp -= pc;
					opcode |= 2; //상대 주소이므로 p=1로 처리해준다.
				}
				
				if(disp<0){
					//disp가 음수인 경우, 하위 3column만 들어갈 수 있도록 bit and 해준다.
					disp &= (int)Math.pow(2, 12) - 1;
				}
				
				t.objectCode=String.format("%03X%03X", opcode, disp);
				

			}else{
				//3형식이고 operand가 없는 경우
				//1형식인 경우도 이곳에 포함되지만 처리하지 못함
				opcode <<= 4; //nixbpe 자리를 만들어주기 위해 shift left 4를 한다.
				opcode |= t.nixbpe; //nixbpe bit or, p값 설정할 필요 없음
				
				t.objectCode=String.format("%03X%03X",opcode,0);
				
			}

		}
	}
	
	/** 
	 * index번호에 해당하는 object code를 리턴한다.
	 * @param index
	 * @return : object code
	 */
	public String getObjectCode(int index) {
		return tokenList.get(index).objectCode;
	}
	
}

/**
 * 각 라인별로 저장된 코드를 단어 단위로 분할한 후  의미를 해석하는 데에 사용되는 변수와 연산을 정의한다. 
 * 의미 해석이 끝나면 pass2에서 object code로 변형되었을 때의 바이트 코드 역시 저장한다.
 */
class Token{
	//의미 분석 단계에서 사용되는 변수들
	int location;
	String label;
	String operator;
	String[] operand;
	String comment;
	char nixbpe;

	// object code 생성 단계에서 사용되는 변수들 
	String objectCode;
	int byteSize;
	
	/**
	 * 클래스를 초기화 하면서 바로 line의 의미 분석을 수행한다. 
	 * @param line 문장단위로 저장된 프로그램 코드
	 */
	public Token(String line) {
		//initialize 추가
		parsing(line);
	}
	
	/**
	 * line의 실질적인 분석을 수행하는 함수. Token의 각 변수에 분석한 결과를 저장한다.
	 * @param line 문장단위로 저장된 프로그램 코드.
	 */
	public void parsing(String line) {
		/** Token Parsing */
		StringTokenizer str = new StringTokenizer(line,"\t");
		StringTokenizer operandStr;
		String operands;
		
		if(line.charAt(0)!='\t'){ //label이 있는 경우
			label = new String(str.nextToken().toString());
		}
		
		operator = new String(str.nextToken().toString());
		
		if(str.hasMoreTokens()){
			
			int i=0;
			
			operand=new String[TokenTable.MAX_OPERAND]; //MAX_OPERAND개 생성, 자동으로 null 들어감
			operands=new String(str.nextToken().toString());
			operandStr=new StringTokenizer(operands, ",");
			
			while(operandStr.hasMoreTokens() && i < TokenTable.MAX_OPERAND){
				operand[i]=new String(operandStr.nextToken().toString());
				i++;
			}
			// 사실 이 때 operand에 comment가 들어가는 경우가 생긴다. (RSUB같이 operand 개수가 0인 경우)
			// 이것에 대해 고민을 많이 했으나,
			// 추후에 TokenTable에서 operand=0인 경우 operand를 들여다보지 않을 것이므로 괜찮다고 결론지었다.
			
			/**flag setting을 token parsing 때 해준다.*/
			if(operator.charAt(0)=='+'){
				this.setFlag(1, 1); //e=1
			}
			
			if(operand[1]!=null && operand[1].charAt(0)=='X'){
				this.setFlag(8, 1);
			}
			//nixbpe
			if(operand[0]!=null){
				
				if(operand[0].charAt(0)=='@'){
					this.setFlag(32, 1); //n=1
					
				}else if(operand[0].charAt(0)=='#'){
					this.setFlag(16, 1); //i=1
					
				}else{
					this.setFlag(48, 1); //n=i=1
				}
				
				//b,p에 대한 값은 추후에 object code를 설정할때 pc relative가 가능한지 보고 결정한다.
			}
			
		}
		
		if(str.hasMoreTokens()){
			comment = new String(str.nextToken().toString());
		}
		
		
	}
	
	/** 
	 * n,i,x,b,p,e flag를 설정한다. 
	 * 
	 * 사용 예 : setFlag(nFlag, 1); 
	 *   또는     setFlag(TokenTable.nFlag, 1);
	 * 
	 * @param flag : 원하는 비트 위치
	 * @param value : 집어넣고자 하는 값. 1또는 0으로 선언한다.
	 */
	public void setFlag(int flag, int value) {
		if(value==1){
			nixbpe |= flag; //bit or			
		}else{
			//0인 경우
			nixbpe &= (~flag);
		}
	}
	
	/**
	 * 원하는 flag들의 값을 얻어올 수 있다. flag의 조합을 통해 동시에 여러개의 플래그를 얻는 것 역시 가능하다 
	 * 
	 * 사용 예 : getFlag(nFlag)
	 *   또는     getFlag(nFlag|iFlag)
	 * 
	 * @param flags : 값을 확인하고자 하는 비트 위치
	 * @return : 비트위치에 들어가 있는 값. 플래그별로 각각 32, 16, 8, 4, 2, 1의 값을 리턴할 것임.
	 */
	public int getFlag(int flags) {
		return nixbpe & flags; //nixbpe & 32, 48 등 bit and값을 리턴
	}
}

/**
 * Modify Record를 미리 저장해놓는 class
 * Modify Record가 하나씩 나올때마다 생성해놓았다가
 * 추후에 section이 끝날 때 실제로 Record를 적는다
 */
class Modify{
	int location; // 고칠 곳의 주소
	int length; // 고칠 곳의 길이
	char plus; // '+' or '-'
	String name; // 더하거나 뺄 symbol 이름
	String objectCode; //modify object code
	
	public Modify(int location, int length, char plus, String name){
		this.location=location;
		this.length=length;
		this.plus=plus;
		this.name=name;
		this.objectCode = String.format("M%06X%02X%c%s", location, length, plus, name);
	}
}