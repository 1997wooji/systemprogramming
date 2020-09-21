package SP20_simulator;

import java.io.File;
import java.io.IOException;

/**
 * �ùķ����ͷμ��� �۾��� ����Ѵ�. VisualSimulator���� ������� ��û�� ������ �̿� ����
 * ResourceManager�� �����Ͽ� �۾��� �����Ѵ�.  
 * 
 * �ۼ����� ���ǻ��� : 
 *  1) ���ο� Ŭ����, ���ο� ����, ���ο� �Լ� ������ �󸶵��� ����. ��, ������ ������ �Լ����� �����ϰų� ������ ��ü�ϴ� ���� ������ ��.
 *  2) �ʿ信 ���� ����ó��, �������̽� �Ǵ� ��� ��� ���� ����.
 *  3) ��� void Ÿ���� ���ϰ��� ������ �ʿ信 ���� �ٸ� ���� Ÿ������ ���� ����.
 *  4) ����, �Ǵ� �ܼ�â�� �ѱ��� ��½�Ű�� �� ��. (ä������ ����. �ּ��� ���Ե� �ѱ��� ��� ����)
 * 
 *  + �����ϴ� ���α׷� ������ ��������� �����ϰ� ���� �е��� ������ ��� �޺κп� ÷�� �ٶ��ϴ�. ���뿡 ���� �������� ���� �� �ֽ��ϴ�.
 */
public class SicSimulator {
	ResourceManager rMgr;
	InstLuncher instLuncher;
	String curInstruction;
	String curLog;
	int locctr;

	public SicSimulator(ResourceManager resourceManager) {
		// �ʿ��ϴٸ� �ʱ�ȭ ���� �߰�
		this.rMgr = resourceManager;
		this.instLuncher=new InstLuncher(resourceManager);
		locctr=0; //start address�� �ʱ�ȭ�ؾ���
	}

	/**
	 * ��������, �޸� �ʱ�ȭ �� ���α׷� load�� ���õ� �۾� ����.
	 * ��, object code�� �޸� ���� �� �ؼ��� SicLoader���� �����ϵ��� �Ѵ�. 
	 */
	public void load(File program) {
		/* �޸� �ʱ�ȭ, �������� �ʱ�ȭ ��*/
		//resourceManager �����Ҷ� initialize�Լ��� ���� �����Ⱚ ������ �ʱ�ȭ�Ѵ�.
		rMgr.initializeResource();
	}

	/**
	 * 1���� instruction�� ����� ����� ���δ�. 
	 * @return ���α׷� ���� �˸��� -1, �ƴϸ� 1�� �����Ѵ�
	 */
	public int oneStep() throws IOException {
		//������ ��ɾ��� �����ϴ� �ű� ������ data(05,F1���� ��ɾ�� ���� ���� ����)
		locctr = rMgr.register[8]; //���� pc�� = locctr update
		//locctr ������Ʈ �� �� �������� opcode�� ������
		if(locctr<0){
			//���� �ؾ���
			return -1;
		}
	    rMgr.rsstAddrMemory=locctr;		
		char[] ornopcode = rMgr.getMemory(locctr, 2);//2byte�� �����´� (opcode, eȮ�� ����)
		int[] arr;
		
		String opcode = String.format("%02X",ornopcode[0]&252);
		//1111 1100�� ni�� ���� ��¥ opcode�� �����´�
		switch(opcode){
		case "00"://LDA
			addLog("LDA");
			setCurInstruction(rMgr.getMemory(locctr, 3));
			arr=parsing(rMgr.getMemory(locctr, 3)); //3byte�����´�
			rMgr.register[8]+=3; //pc�� ����
			instLuncher.LDA(new Instruction(arr[0],arr[1],arr[2]));
			break;
		case "74"://LDT
			addLog("LDT");
			if((ornopcode[1] & 16) == 16){ //xbpe 0000 ���� ���⼭ e�� 16�̴�
				setCurInstruction(rMgr.getMemory(locctr, 4));
				arr=parsing(rMgr.getMemory(locctr, 4)); //e==1, 4������ ���
				rMgr.register[8]+=4;
			}else{
				setCurInstruction(rMgr.getMemory(locctr, 3));
				arr=parsing(rMgr.getMemory(locctr, 3)); //3������ ���
				rMgr.register[8]+=3;
			}
			instLuncher.LDT(new Instruction(arr[0],arr[1],arr[2]));
			break;
		case "50"://LDCH
			addLog("LDCH");
			if((ornopcode[1] & 16) == 16){
				setCurInstruction(rMgr.getMemory(locctr, 4));
				arr=parsing(rMgr.getMemory(locctr, 4));
				rMgr.register[8]+=4;
			}else{
				setCurInstruction(rMgr.getMemory(locctr, 3));
				arr=parsing(rMgr.getMemory(locctr, 3));
				rMgr.register[8]+=3;
			}
			instLuncher.LDCH(new Instruction(arr[0],arr[1],arr[2]));
			break;
		case "0C"://STA
			addLog("STA");
			setCurInstruction(rMgr.getMemory(locctr, 3));
			arr=parsing(rMgr.getMemory(locctr, 3));
			rMgr.register[8]+=3;
			instLuncher.STA(new Instruction(arr[0],arr[1],arr[2]));
			break;
		case "14"://STL
			addLog("STL");
			setCurInstruction(rMgr.getMemory(locctr, 3));
			arr=parsing(rMgr.getMemory(locctr, 3));
			rMgr.register[8]+=3;
			instLuncher.STL(new Instruction(arr[0],arr[1],arr[2]));
			break;
		case "10"://STX
			addLog("STX");
			if((ornopcode[1] & 16) == 16){
				setCurInstruction(rMgr.getMemory(locctr, 4));
				arr=parsing(rMgr.getMemory(locctr, 4));
				rMgr.register[8]+=4;
			}else{
				setCurInstruction(rMgr.getMemory(locctr, 3));
				arr=parsing(rMgr.getMemory(locctr, 3));
				rMgr.register[8]+=3;
			}
			instLuncher.STX(new Instruction(arr[0],arr[1],arr[2]));
			break;
		case "54"://STCH
			addLog("STCH");
			if((ornopcode[1] & 16) == 16){
				setCurInstruction(rMgr.getMemory(locctr, 4));
				arr=parsing(rMgr.getMemory(locctr, 4));
				rMgr.register[8]+=4;
			}else{
				setCurInstruction(rMgr.getMemory(locctr, 3));
				arr=parsing(rMgr.getMemory(locctr, 3));
				rMgr.register[8]+=3;
			}
			instLuncher.STCH(new Instruction(arr[0],arr[1],arr[2]));
			break;
		case "48"://JSUB
			addLog("JSUB");
			if((ornopcode[1] & 16) == 16){
				setCurInstruction(rMgr.getMemory(locctr, 4));
				arr=parsing(rMgr.getMemory(locctr, 4));
				rMgr.register[8]+=4;
			}else{
				setCurInstruction(rMgr.getMemory(locctr, 3));
				arr=parsing(rMgr.getMemory(locctr, 3));
				rMgr.register[8]+=3;
			}
			instLuncher.JSUB(new Instruction(arr[0],arr[1],arr[2]));
			break;
		case "30"://JEQ
			addLog("JEQ");
			setCurInstruction(rMgr.getMemory(locctr, 3));
			arr=parsing(rMgr.getMemory(locctr, 3));
			rMgr.register[8]+=3;
			instLuncher.JEQ(new Instruction(arr[0],arr[1],arr[2]));
			break;
		case "38"://JLT
			addLog("JLT");
			setCurInstruction(rMgr.getMemory(locctr, 3));
			arr=parsing(rMgr.getMemory(locctr, 3));
			rMgr.register[8]+=3;
			instLuncher.JLT(new Instruction(arr[0],arr[1],arr[2]));
			break;
		case "3C"://J
			addLog("J");
			setCurInstruction(rMgr.getMemory(locctr, 3));
			arr=parsing(rMgr.getMemory(locctr, 3));
			rMgr.register[8]+=3;
			instLuncher.J(new Instruction(arr[0],arr[1],arr[2]));
			break;
		case "B4"://CLEAR(2����)
			addLog("CLEAR");
			setCurInstruction(rMgr.getMemory(locctr, 2));
			arr=parsing(rMgr.getMemory(locctr, 2));
			rMgr.register[8]+=2;
			instLuncher.CLEAR(new Instruction(arr[0],arr[1],arr[2],true));
			break;
		case "28"://COMP
			addLog("COMP");
			setCurInstruction(rMgr.getMemory(locctr, 3));
			arr=parsing(rMgr.getMemory(locctr, 3));
			rMgr.register[8]+=3;
			instLuncher.COMP(new Instruction(arr[0],arr[1],arr[2]));
			break;
		case "A0"://COMPR
			addLog("COMPR");
			setCurInstruction(rMgr.getMemory(locctr, 2));
			arr=parsing(rMgr.getMemory(locctr, 2));
			rMgr.register[8]+=2;
			instLuncher.COMPR(new Instruction(arr[0],arr[1],arr[2],true));
			break;
		case "B8"://TIXR
			addLog("TIXR");
			setCurInstruction(rMgr.getMemory(locctr, 2));
			arr=parsing(rMgr.getMemory(locctr, 2));
			rMgr.register[8]+=2;
			instLuncher.TIXR(new Instruction(arr[0],arr[1],arr[2],true));
			break;
		case "E0"://TD
			addLog("TD");
			setCurInstruction(rMgr.getMemory(locctr, 3));
			arr=parsing(rMgr.getMemory(locctr, 3));
			rMgr.register[8]+=3;
			instLuncher.TD(new Instruction(arr[0],arr[1],arr[2]));
			break;
		case "D8"://RD
			addLog("RD");
			setCurInstruction(rMgr.getMemory(locctr, 3));
			arr=parsing(rMgr.getMemory(locctr, 3));
			rMgr.register[8]+=3;
			instLuncher.RD(new Instruction(arr[0],arr[1],arr[2]));
			break;
		case "DC"://WD
			addLog("WD");
			setCurInstruction(rMgr.getMemory(locctr, 3));
			arr=parsing(rMgr.getMemory(locctr, 3));
			rMgr.register[8]+=3;
			instLuncher.WD(new Instruction(arr[0],arr[1],arr[2]));
			break;
		case "4C"://RSUB
			addLog("RSUB");
			setCurInstruction(rMgr.getMemory(locctr, 3));
			arr=parsing(rMgr.getMemory(locctr, 3));
			rMgr.register[8]+=3;
			instLuncher.RSUB(new Instruction(arr[0],arr[1],arr[2]));
			break;
		}
		
		return 1;
	}
	
	/**
	 * ���� ��� instruction�� ����� ����� ���δ�.
	 */
	public void allStep() {
		try{
		while(oneStep()>0);
		rMgr.closeDevice();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	//���� instruction Log�� visualSimulator���� �� �� �ֵ����� setting�Ѵ�
	public void addLog(String log) {
		curLog=log;
	}	
	
	//���� instruction object code�� visualSimulator���� �� �� �ֵ����� setting�Ѵ�
	public void setCurInstruction(char[] arr){
		String str="";
		if(arr.length==2){
			str+=String.format("%02X", (int)arr[0]);
			str+=String.format("%02X", (int)arr[1]);
		}else if(arr.length==4){
			str+=String.format("%02X", (int)arr[0]);
			str+=String.format("%02X", (int)arr[1]);
			str+=String.format("%02X", (int)arr[2]);
			str+=String.format("%02X", (int)arr[3]);
		}else{
			//3����
			str+=String.format("%02X", (int)arr[0]);
			str+=String.format("%02X", (int)arr[1]);
			str+=String.format("%02X", (int)arr[2]);
		}
		
		curInstruction=str;
	}
	
	/**
	 * ������ ���� Instuction�� ��������� parsing�Ѵ�
	 * int[0] opcode
	 * int[1] nixbpe
	 * int[2] disp
	 * 2������ ��� int[1]=reg1, int[2]=reg2
	 * �� ����� ��ȯ�Ѵ�
	 */
	public int[] parsing(char[] arr){
		int[] retarr=new int[3];
		if(arr.length==2){
			retarr[0]=arr[0];//B4 12
			retarr[1]=(arr[1]&240)>>4;//1111 0000 reg1, right shift 4���־���Ѵ�
			retarr[2]=(arr[1]&15);//0000 1111 reg2
			
		}else if(arr.length==4){
			retarr[0]=(arr[0]&252);//1111 1100 opcode
			retarr[1]=(arr[0]&3); //ni��
			retarr[1] <<=4; //xbpe �� �ڸ� ����
			int xbpe = (arr[1] >> 4); //4��ŭ right shift ���־���Ѵ�
			retarr[1] |= xbpe;
			retarr[2]=(arr[1]&15); //0000 1111 disp�� ���� ��
			retarr[2] <<=8; //1byte�� 8�̹Ƿ� 8��ŭ
			retarr[2] |= arr[2];
			retarr[2] <<=8;
			retarr[2] |= arr[3]; //int�� 4byte�̹Ƿ� ������
		}else{
			//length==3�� ���
			retarr[0]=(arr[0]&252);//1111 1100 opcode
			retarr[1]=(arr[0]&3); //ni��
			retarr[1] <<=4; //xbpe �� �ڸ� ����
			int xbpe = (arr[1] >> 4); //4��ŭ right shift ���־���Ѵ�
			retarr[1] |= xbpe;
			retarr[2]=(arr[1]&15); //0000 1111 disp�� ���� ��
			retarr[2] <<=8; //1byte�� 8�̹Ƿ� 8��ŭ
			retarr[2] |= arr[2];
		}
		
		//**F�� �����ϸ� ���� ó�� �������!!!**
		if(((retarr[2]>>11)&1)==1){
			// bit������ 1�� �����ϴ� ������ ���, �տ� FF�� ó�����־�� �Ѵ�
			long i1 = Long.parseLong("FFFFFF000",16);
			retarr[2]|=(int)i1; //bit or�� �պκе� �� FFó�� ���ֱ�
			
		}
		
		return retarr;
	}
}
