import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * ����ڰ� �ۼ��� ���α׷� �ڵ带 �ܾ�� ���� �� ��, �ǹ̸� �м��ϰ�, ���� �ڵ�� ��ȯ�ϴ� ������ �Ѱ��ϴ� Ŭ�����̴�. <br>
 * pass2���� object code�� ��ȯ�ϴ� ������ ȥ�� �ذ��� �� ���� symbolTable�� instTable�� ������ �ʿ��ϹǷ� �̸� ��ũ��Ų��.<br>
 * section ���� �ν��Ͻ��� �ϳ��� �Ҵ�ȴ�.
 *
 */
public class TokenTable {
	public static final int MAX_OPERAND=3;
	
	/* bit ������ �������� ���� ���� */
	public static final int nFlag=32;
	public static final int iFlag=16;
	public static final int xFlag=8;
	public static final int bFlag=4;
	public static final int pFlag=2;
	public static final int eFlag=1;
	
	/* Token�� �ٷ� �� �ʿ��� ���̺���� ��ũ��Ų��. */
	SymbolTable symTab;
	LiteralTable literalTab;
	InstTable instTab;
	RegisterTable registerTab;
	
	/** �� line�� �ǹ̺��� �����ϰ� �м��ϴ� ����. */
	ArrayList<Token> tokenList;
	
	/** �� section�� ���� */
	int programLength;
	
	/*�� section�� modify record�� �����ϴ� ����*/
	ArrayList<Modify> modifyList;
	
	/**
	 * �ʱ�ȭ�ϸ鼭 symTable��,literalTable, instTable�� ��ũ��Ų��.
	 * @param symTab : �ش� section�� ����Ǿ��ִ� symbol table
	 * @param literalTab : �ش� section�� ����Ǿ��ִ� literal table
	 * @param instTab : instruction ���� ���ǵ� instTable
	 * @param registerTab : register name-��ȣ�� ���ǵ� register table
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
	 * �Ϲ� ���ڿ��� �޾Ƽ� Token������ �и����� tokenList�� �߰��Ѵ�.
	 * @param line : �и����� ���� �Ϲ� ���ڿ�
	 */
	public void putToken(String line) {
		tokenList.add(new Token(line));
	}
	
	/**
	 * tokenList���� index�� �ش��ϴ� Token�� �����Ѵ�.
	 * @param index
	 * @return : index��ȣ�� �ش��ϴ� �ڵ带 �м��� Token Ŭ����
	 */
	public Token getToken(int index) {
		return tokenList.get(index);
	}
	
	/**
	 * Pass2 �������� ����Ѵ�.
	 * instruction table, symbol table literal table ���� �����Ͽ� objectcode�� �����ϰ�, �̸� �����Ѵ�.
	 * @param index
	 */
	public void makeObjectCode(int index){
		// END, LTROG �� ��� ��쿡�� �� �Լ��� call���� �ʴ´�. pass2()���� �˾Ƽ� ó���Ѵ�.
		// Token�� object code�� ������ object code�� �����Ѵ�.
		
		Token t = tokenList.get(index);
		//int byteSize = 0; //�ش� object Code ����
		int pc=0; //pc��
		int disp=0; //disp ���� ����
		int opcode=0; //opcode�� ����
		
		if(t.operator.equals("START") || t.operator.equals("CSECT")){
			//Header Record, ���� �ּҴ� CS program�̰�, ��� �ö��� �𸣹Ƿ� 0�� ���̴�.
			t.objectCode = String.format("H%-6s%06X%06X", t.label, 0, programLength);
			
		}else if(t.operator.equals("EXTDEF")){
			//D record
			t.objectCode=new String("D");
			int i=0;
			while(i<MAX_OPERAND && t.operand[i]!=null){
				//Array Index Bound Exception�� ���� ���� i<MAX_OPERAND ������ �� �־���� �Ѵ�.
				//������ symbol�̸��� �ּҸ� �߰��Ѵ�
				t.objectCode += String.format("%s%06X", t.operand[i],symTab.search(t.operand[i]));
				i++;
			}
			
		}else if(t.operator.equals("EXTREF")){
			//R record
			t.objectCode=new String("R");
			int i=0;
			while(i<MAX_OPERAND && t.operand[i]!=null){
				t.objectCode += String.format("%-6s",t.operand[i]); //�ּҴ� �𸣹Ƿ� symbol�� �߰��Ѵ�
				i++;
			}
			
		}else if(t.operator.equals("WORD")){
			// WORD�� ���
			// ���, 2���� symbol ���� ����, �ϳ��� symbol�� ������ ��� ó�� �����ϴ�.
			// ���� ������ ���� ���Ѵ�.
			
			pc = t.location+t.byteSize;

			StringTokenizer str = new StringTokenizer(t.operand[0],"-");
			String searchStr = str.nextToken();
			try{
				disp = Integer.parseInt(searchStr); //����� ���
				
			}catch(NumberFormatException e){
				//symbol�� ���
				disp = symTab.search(searchStr);
				
				if(disp < 0){ //���� symbol�� ���
					//Modify unit�߰�
					disp = 0;
					modifyList.add(new Modify(t.location, 6, '+', searchStr)); //WORD�̹Ƿ� WORD ���� ������ �ʿ�
				}
				
				if(str.hasMoreTokens()){
					// "-"�� �и����Ǵ�, ���� ������ �־��� ���
					searchStr = str.nextToken();
					int subDisp = symTab.search(searchStr);
					
					if(subDisp < 0){ //���� symbol�� ���
						//Modify unit�߰�
						subDisp=0;
						modifyList.add(new Modify(t.location, 6, '-', searchStr)); //WORD�̹Ƿ� WORD ���� ������ �ʿ�
					}
					disp -= subDisp;
					//disp ���� (���� �� section�� ���°� - �ִ°� �� ���
					//'-�ִ°�'�� disp�� ���Եȴ�.
				}
				
				if(disp < 0){
					disp &= (int)Math.pow(2, 24) -1; //������ ��� WORD3byte��ο� ���� ó�� �ؾ��Ѵ�
				}
				
				t.objectCode=String.format("%06X", disp);
			}
			
	
			
		}else if(t.operator.equals("BYTE")){
			//BYTE�� ��� (���� -,+�� ���� ó�� ������ �ʾҴ�)
			pc = t.location+t.byteSize;
			//C�� X����
			if(t.operand[0].charAt(0)=='X'){
				//X�� ���
				StringTokenizer str = new StringTokenizer(t.operand[0],"'");
				str.nextToken(); //X' �и�
				t.objectCode = new String(str.nextToken());
			}else{
				//C�� ���, ASCII code�� ��ȯ�Ѵ�
				t.objectCode = new String("");
				StringTokenizer str = new StringTokenizer(t.operand[0],"'");
				str.nextToken(); //C' �и�
				for(int i=0; i < t.byteSize ; i++){
					t.objectCode += String.format("%02X", (int)str.nextToken().charAt(i));
				}
				
			}
			
		}else{
			//Instruction�� ���
			pc = t.location+t.byteSize;
			opcode = instTab.searchOpcode(t.operator);
			
			if(t.getFlag(1)==1){
				//4������ ���
				// ��Ģ ���꿡 ���� ó���� ������ ���ߴ�
				opcode <<= 4; //nixbpe �ڸ��� ������ֱ� ���� shift left 4�� �Ѵ�.
				opcode |= t.nixbpe; //nixbpe bit or
				//��� nixbpe�� ����ϴ� ��쿡��, base relative�� ���� ó���� ������ ����. pc relative�� ����Ѵ�.
				disp = symTab.search(t.operand[0]); //operand�� �ּҸ� ã�´�
				
				if(disp<0){
					disp = 0; //���� symbol�� ���(EXTREF�� ����� symbol�� ���)
					//modify record�� �߰��Ѵ�
					modifyList.add(new Modify(t.location+1, 5, '+', t.operand[0])); //operand �κи� ������ �ʿ��ϴ�
				}else{
					disp -= pc;
					opcode |= 2; //pc relative, p=1 ����
				}
				
				if(disp<0){
					// disp�� ������ ���, int ũ�� ������ 20bit�� format�� ���� �� ����.
					// ���ϴ� ��ŭ �ڸ��� ���� bit and�� �̿��Ѵ�.
					disp &= (int)Math.pow(2, 20) - 1;
				}
				
				t.objectCode=String.format("%03X%05X", opcode,disp);
				
			}else if(t.byteSize == 2){
				//2������ ���, nixbpe�� ������� �ʴ´�.
				//register�� ���� ����Ѵ�.
				t.objectCode=String.format("%02X%X%X", opcode, registerTab.getNumber(t.operand[0]),registerTab.getNumber(t.operand[1]));

			}else if(instTab.searchNumberOfOperand(t.operator)==1){
				//3�����̰� operand�� �ִ� ���
				opcode <<= 4; //nixbpe �ڸ��� ������ֱ� ���� shift left 4�� �Ѵ�.
				opcode |= t.nixbpe; //nixbpe bit or
				
				if(t.getFlag(48)==48){
					//n=i=1�� ���
					
					if(t.operand[0].charAt(0)=='='){
						//literal�� ���
						StringTokenizer str = new StringTokenizer(t.operand[0],"'");
						str.nextToken(); //=C', =X' �и�
						disp=literalTab.search(str.nextToken()); //LITTAB���� target address�� ã�´�
						disp-=pc; //disp�� ���Ѵ�
						opcode |=2; // p=1 ����
					}else{
						// �Ϲ� access
						// 4������ �ƴϹǷ� operand�� symtab�� �־�� �Ѵ�. (4����ó�� ������ �ȵȴ�.)
						disp = symTab.search(t.operand[0]);
						disp -= pc;
						opcode |=2; //disp�� ���ϰ� p=1�� �����Ѵ�
					}
				}else if(t.getFlag(16)==16){
					// �� if���� ���� ������ n=i=1�� ��쵵 ���Ƿ� �Ŀ� ���־�� �Ѵ�.
					// immediate�� ���
					// # MAXLEN���� ���� ó�������� �ʾҴ�.
					// ��� �ּҰ� �ƴ� ��¥ ���̹Ƿ� p=1ó�� ������ �ʴ´�.
					StringTokenizer str = new StringTokenizer(t.operand[0],"#");
					disp = Integer.parseInt(str.nextToken());
				}else {
					//indirect�� ���(1������ ��쳪 n=i=0�� ��쵵 ���� ��������, ó������ ���Ѵ�.)
					StringTokenizer str = new StringTokenizer(t.operand[0],"@");
					disp = symTab.search(str.nextToken());
					disp -= pc;
					opcode |= 2; //��� �ּ��̹Ƿ� p=1�� ó�����ش�.
				}
				
				if(disp<0){
					//disp�� ������ ���, ���� 3column�� �� �� �ֵ��� bit and ���ش�.
					disp &= (int)Math.pow(2, 12) - 1;
				}
				
				t.objectCode=String.format("%03X%03X", opcode, disp);
				

			}else{
				//3�����̰� operand�� ���� ���
				//1������ ��쵵 �̰��� ���Ե����� ó������ ����
				opcode <<= 4; //nixbpe �ڸ��� ������ֱ� ���� shift left 4�� �Ѵ�.
				opcode |= t.nixbpe; //nixbpe bit or, p�� ������ �ʿ� ����
				
				t.objectCode=String.format("%03X%03X",opcode,0);
				
			}

		}
	}
	
	/** 
	 * index��ȣ�� �ش��ϴ� object code�� �����Ѵ�.
	 * @param index
	 * @return : object code
	 */
	public String getObjectCode(int index) {
		return tokenList.get(index).objectCode;
	}
	
}

/**
 * �� ���κ��� ����� �ڵ带 �ܾ� ������ ������ ��  �ǹ̸� �ؼ��ϴ� ���� ���Ǵ� ������ ������ �����Ѵ�. 
 * �ǹ� �ؼ��� ������ pass2���� object code�� �����Ǿ��� ���� ����Ʈ �ڵ� ���� �����Ѵ�.
 */
class Token{
	//�ǹ� �м� �ܰ迡�� ���Ǵ� ������
	int location;
	String label;
	String operator;
	String[] operand;
	String comment;
	char nixbpe;

	// object code ���� �ܰ迡�� ���Ǵ� ������ 
	String objectCode;
	int byteSize;
	
	/**
	 * Ŭ������ �ʱ�ȭ �ϸ鼭 �ٷ� line�� �ǹ� �м��� �����Ѵ�. 
	 * @param line ��������� ����� ���α׷� �ڵ�
	 */
	public Token(String line) {
		//initialize �߰�
		parsing(line);
	}
	
	/**
	 * line�� �������� �м��� �����ϴ� �Լ�. Token�� �� ������ �м��� ����� �����Ѵ�.
	 * @param line ��������� ����� ���α׷� �ڵ�.
	 */
	public void parsing(String line) {
		/** Token Parsing */
		StringTokenizer str = new StringTokenizer(line,"\t");
		StringTokenizer operandStr;
		String operands;
		
		if(line.charAt(0)!='\t'){ //label�� �ִ� ���
			label = new String(str.nextToken().toString());
		}
		
		operator = new String(str.nextToken().toString());
		
		if(str.hasMoreTokens()){
			
			int i=0;
			
			operand=new String[TokenTable.MAX_OPERAND]; //MAX_OPERAND�� ����, �ڵ����� null ��
			operands=new String(str.nextToken().toString());
			operandStr=new StringTokenizer(operands, ",");
			
			while(operandStr.hasMoreTokens() && i < TokenTable.MAX_OPERAND){
				operand[i]=new String(operandStr.nextToken().toString());
				i++;
			}
			// ��� �� �� operand�� comment�� ���� ��찡 �����. (RSUB���� operand ������ 0�� ���)
			// �̰Ϳ� ���� ����� ���� ������,
			// ���Ŀ� TokenTable���� operand=0�� ��� operand�� �鿩�ٺ��� ���� ���̹Ƿ� �����ٰ� ���������.
			
			/**flag setting�� token parsing �� ���ش�.*/
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
				
				//b,p�� ���� ���� ���Ŀ� object code�� �����Ҷ� pc relative�� �������� ���� �����Ѵ�.
			}
			
		}
		
		if(str.hasMoreTokens()){
			comment = new String(str.nextToken().toString());
		}
		
		
	}
	
	/** 
	 * n,i,x,b,p,e flag�� �����Ѵ�. 
	 * 
	 * ��� �� : setFlag(nFlag, 1); 
	 *   �Ǵ�     setFlag(TokenTable.nFlag, 1);
	 * 
	 * @param flag : ���ϴ� ��Ʈ ��ġ
	 * @param value : ����ְ��� �ϴ� ��. 1�Ǵ� 0���� �����Ѵ�.
	 */
	public void setFlag(int flag, int value) {
		if(value==1){
			nixbpe |= flag; //bit or			
		}else{
			//0�� ���
			nixbpe &= (~flag);
		}
	}
	
	/**
	 * ���ϴ� flag���� ���� ���� �� �ִ�. flag�� ������ ���� ���ÿ� �������� �÷��׸� ��� �� ���� �����ϴ� 
	 * 
	 * ��� �� : getFlag(nFlag)
	 *   �Ǵ�     getFlag(nFlag|iFlag)
	 * 
	 * @param flags : ���� Ȯ���ϰ��� �ϴ� ��Ʈ ��ġ
	 * @return : ��Ʈ��ġ�� �� �ִ� ��. �÷��׺��� ���� 32, 16, 8, 4, 2, 1�� ���� ������ ����.
	 */
	public int getFlag(int flags) {
		return nixbpe & flags; //nixbpe & 32, 48 �� bit and���� ����
	}
}

/**
 * Modify Record�� �̸� �����س��� class
 * Modify Record�� �ϳ��� ���ö����� �����س��Ҵٰ�
 * ���Ŀ� section�� ���� �� ������ Record�� ���´�
 */
class Modify{
	int location; // ��ĥ ���� �ּ�
	int length; // ��ĥ ���� ����
	char plus; // '+' or '-'
	String name; // ���ϰų� �� symbol �̸�
	String objectCode; //modify object code
	
	public Modify(int location, int length, char plus, String name){
		this.location=location;
		this.length=length;
		this.plus=plus;
		this.name=name;
		this.objectCode = String.format("M%06X%02X%c%s", location, length, plus, name);
	}
}