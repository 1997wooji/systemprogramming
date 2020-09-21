package SP20_simulator;

import java.io.File;
import java.io.IOException;

/**
 * 시뮬레이터로서의 작업을 담당한다. VisualSimulator에서 사용자의 요청을 받으면 이에 따라
 * ResourceManager에 접근하여 작업을 수행한다.  
 * 
 * 작성중의 유의사항 : 
 *  1) 새로운 클래스, 새로운 변수, 새로운 함수 선언은 얼마든지 허용됨. 단, 기존의 변수와 함수들을 삭제하거나 완전히 대체하는 것은 지양할 것.
 *  2) 필요에 따라 예외처리, 인터페이스 또는 상속 사용 또한 허용됨.
 *  3) 모든 void 타입의 리턴값은 유저의 필요에 따라 다른 리턴 타입으로 변경 가능.
 *  4) 파일, 또는 콘솔창에 한글을 출력시키지 말 것. (채점상의 이유. 주석에 포함된 한글은 상관 없음)
 * 
 *  + 제공하는 프로그램 구조의 개선방법을 제안하고 싶은 분들은 보고서의 결론 뒷부분에 첨부 바랍니다. 내용에 따라 가산점이 있을 수 있습니다.
 */
public class SicSimulator {
	ResourceManager rMgr;
	InstLuncher instLuncher;
	String curInstruction;
	String curLog;
	int locctr;

	public SicSimulator(ResourceManager resourceManager) {
		// 필요하다면 초기화 과정 추가
		this.rMgr = resourceManager;
		this.instLuncher=new InstLuncher(resourceManager);
		locctr=0; //start address로 초기화해야함
	}

	/**
	 * 레지스터, 메모리 초기화 등 프로그램 load와 관련된 작업 수행.
	 * 단, object code의 메모리 적재 및 해석은 SicLoader에서 수행하도록 한다. 
	 */
	public void load(File program) {
		/* 메모리 초기화, 레지스터 초기화 등*/
		//resourceManager 생성할때 initialize함수를 통해 쓰레기값 없도록 초기화한다.
		rMgr.initializeResource();
	}

	/**
	 * 1개의 instruction이 수행된 모습을 보인다. 
	 * @return 프로그램 끝을 알릴시 -1, 아니면 1을 리턴한다
	 */
	public int oneStep() throws IOException {
		//어차피 명령어대로 실행하는 거기 때문에 data(05,F1등을 명령어로 읽을 일은 없음)
		locctr = rMgr.register[8]; //현재 pc값 = locctr update
		//locctr 업데이트 후 그 다음부터 opcode를 가져옴
		if(locctr<0){
			//종료 해야함
			return -1;
		}
	    rMgr.rsstAddrMemory=locctr;		
		char[] ornopcode = rMgr.getMemory(locctr, 2);//2byte만 가져온다 (opcode, e확인 때문)
		int[] arr;
		
		String opcode = String.format("%02X",ornopcode[0]&252);
		//1111 1100로 ni를 떼고 진짜 opcode만 가져온다
		switch(opcode){
		case "00"://LDA
			addLog("LDA");
			setCurInstruction(rMgr.getMemory(locctr, 3));
			arr=parsing(rMgr.getMemory(locctr, 3)); //3byte가져온다
			rMgr.register[8]+=3; //pc값 변경
			instLuncher.LDA(new Instruction(arr[0],arr[1],arr[2]));
			break;
		case "74"://LDT
			addLog("LDT");
			if((ornopcode[1] & 16) == 16){ //xbpe 0000 으로 여기서 e는 16이다
				setCurInstruction(rMgr.getMemory(locctr, 4));
				arr=parsing(rMgr.getMemory(locctr, 4)); //e==1, 4형식인 경우
				rMgr.register[8]+=4;
			}else{
				setCurInstruction(rMgr.getMemory(locctr, 3));
				arr=parsing(rMgr.getMemory(locctr, 3)); //3형식인 경우
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
		case "B4"://CLEAR(2형식)
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
	 * 남은 모든 instruction이 수행된 모습을 보인다.
	 */
	public void allStep() {
		try{
		while(oneStep()>0);
		rMgr.closeDevice();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	//현재 instruction Log를 visualSimulator에서 볼 수 있도록을 setting한다
	public void addLog(String log) {
		curLog=log;
	}	
	
	//현재 instruction object code를 visualSimulator에서 볼 수 있도록을 setting한다
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
			//3형식
			str+=String.format("%02X", (int)arr[0]);
			str+=String.format("%02X", (int)arr[1]);
			str+=String.format("%02X", (int)arr[2]);
		}
		
		curInstruction=str;
	}
	
	/**
	 * 참조시 편한 Instuction을 만들기위해 parsing한다
	 * int[0] opcode
	 * int[1] nixbpe
	 * int[2] disp
	 * 2형식인 경우 int[1]=reg1, int[2]=reg2
	 * 를 만들어 반환한다
	 */
	public int[] parsing(char[] arr){
		int[] retarr=new int[3];
		if(arr.length==2){
			retarr[0]=arr[0];//B4 12
			retarr[1]=(arr[1]&240)>>4;//1111 0000 reg1, right shift 4해주어야한다
			retarr[2]=(arr[1]&15);//0000 1111 reg2
			
		}else if(arr.length==4){
			retarr[0]=(arr[0]&252);//1111 1100 opcode
			retarr[1]=(arr[0]&3); //ni들어감
			retarr[1] <<=4; //xbpe 들어갈 자리 만듦
			int xbpe = (arr[1] >> 4); //4만큼 right shift 해주어야한다
			retarr[1] |= xbpe;
			retarr[2]=(arr[1]&15); //0000 1111 disp가 먼저 들어감
			retarr[2] <<=8; //1byte는 8이므로 8만큼
			retarr[2] |= arr[2];
			retarr[2] <<=8;
			retarr[2] |= arr[3]; //int는 4byte이므로 괜찮다
		}else{
			//length==3인 경우
			retarr[0]=(arr[0]&252);//1111 1100 opcode
			retarr[1]=(arr[0]&3); //ni들어감
			retarr[1] <<=4; //xbpe 들어갈 자리 만듦
			int xbpe = (arr[1] >> 4); //4만큼 right shift 해주어야한다
			retarr[1] |= xbpe;
			retarr[2]=(arr[1]&15); //0000 1111 disp가 먼저 들어감
			retarr[2] <<=8; //1byte는 8이므로 8만큼
			retarr[2] |= arr[2];
		}
		
		//**F로 시작하면 음수 처리 해줘야함!!!**
		if(((retarr[2]>>11)&1)==1){
			// bit시작이 1로 시작하는 음수일 경우, 앞에 FF를 처리해주어야 한다
			long i1 = Long.parseLong("FFFFFF000",16);
			retarr[2]|=(int)i1; //bit or로 앞부분도 다 FF처리 해주기
			
		}
		
		return retarr;
	}
}
