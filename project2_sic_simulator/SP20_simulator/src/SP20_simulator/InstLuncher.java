package SP20_simulator;

import java.io.IOException;

/* instruction에 따라 동작을 수행하는 메소드를 정의하는 클래스
*/
public class InstLuncher {
    ResourceManager rMgr;
    

    public InstLuncher(ResourceManager resourceManager) {
        this.rMgr = resourceManager;
    }
    
    // instruction 별로 동작을 수행하는 메소드를 정의
    // A0 X1 L2 B3 S4 T5 F6 PC8 SW9
    //n=i=1인 경우 무조건 p=1이므로 다른 처리는 하지 않았다.
    //copy에서만 돌아가도록 짰다
    
    public void LDA(Instruction inst){
    	if((inst.nixbpe&48)==48){
    		//n=i=1
    		//target address 설정
    		int ta = inst.disp + rMgr.register[8]; //disp+pc=TA
    		rMgr.rstargetAddr=ta;
    		//3byte(word) A reg에 저장
    		char[] arr = rMgr.getMemory(ta, 3);
    		int value = rMgr.byteToInt(arr);
    		rMgr.register[0]=value;
    		
    	}else if((inst.nixbpe&16)==16){
    		//i=1, target address 설정 필요 X
    		rMgr.register[0]=inst.disp;
    	}
    }
    
    
    public void LDT(Instruction inst){
    	int ta;
    	if((inst.nixbpe & 1)==1){
	    	//4형식
			//target address 설정
			ta = inst.disp; //ta가 절대값임(absolute, direct)
    	}else{
    		//3형식
    		ta = inst.disp + rMgr.register[8]; //disp+pc=TA
    	}
    	//3byte(word) T reg에 저장
    	rMgr.rstargetAddr=ta;
		char[] arr = rMgr.getMemory(ta, 3);
		int value = rMgr.byteToInt(arr);
		rMgr.register[5]=value;

    }
    
    public void LDCH(Instruction inst){
    	int ta;
    	if((inst.nixbpe & 1)==1){
	    	//4형식
			//target address 설정
			ta = inst.disp; //ta가 절대값임(absolute, direct)
    	}else{
    		//3형식
    		ta = inst.disp + rMgr.register[8]; //disp+pc=TA
    	}
    	//1byte 읽어 A reg에 저장
    	if((inst.nixbpe & 8)==8){
    		//X가 있는 경우
    		ta += rMgr.register[1];//X reg값을 TA에 더한다(index)
    	}
    	rMgr.rstargetAddr=ta;
		char[] arr = rMgr.getMemory(ta, 1);
		int value = rMgr.byteToInt(arr);
		rMgr.register[0]=value;
    }
    
    public void STA(Instruction inst){
    	int ta = inst.disp + rMgr.register[8];
    	rMgr.rstargetAddr=ta;
    	char[] data=rMgr.intToChar(rMgr.register[0]); // A regi
    	rMgr.setMemory(ta, data, 3);
    }
    
    public void STL(Instruction inst){
    	int ta = inst.disp + rMgr.register[8];
    	rMgr.rstargetAddr=ta;
    	char[] data=rMgr.intToChar(rMgr.register[2]); //L regi
    	rMgr.setMemory(ta, data, 3);
    }

    public void STX(Instruction inst){
    	int ta;
    	if((inst.nixbpe & 1)==1){
	    	//4형식
			//target address 설정
			ta = inst.disp; //ta가 절대값임(absolute, direct)
    	}else{
    		//3형식
    		ta = inst.disp + rMgr.register[8]; //disp+pc=TA
    	}
    	//TA에 X에 있는 3byte(word)저장
    	rMgr.rstargetAddr=ta;
    	char[] data = rMgr.intToChar(rMgr.register[1]);
    	rMgr.setMemory(ta, data, 3);
    }
    
    public void STCH(Instruction inst){
    	int ta;
    	if((inst.nixbpe & 1)==1){
	    	//4형식
			//target address 설정
			ta = inst.disp; //ta가 절대값임(absolute, direct)
    	}else{
    		//3형식
    		ta = inst.disp + rMgr.register[8]; //disp+pc=TA
    	}
    	
    	if((inst.nixbpe & 8)==8){
    		//X가 있는 경우
    		ta += rMgr.register[1];
    	}
    	//TA에 A에 있는 1byte(word)저장
    	//char[] data = rMgr.intToChar(rMgr.register[0]); //이 함수가 무조건 3byte반환해서 쓸 수 없었다
    	rMgr.rstargetAddr=ta;
    	char[] data = new char[1];
    	data[0]=(char)rMgr.register[0];
    	rMgr.setMemory(ta, data, 1);
    }    
    
    public void JSUB(Instruction inst){
    	//PC값 변경
    	rMgr.subReturnAddr=rMgr.register[2];
    	rMgr.register[2]=rMgr.register[8]; //돌아올 주소(Lreg)에 PC값 저장
    	
    	if((inst.nixbpe & 1)==1){
    	//4형식
    		rMgr.register[8] = inst.disp; //relative가 아니므로 target address계산할 필요 없음
    		rMgr.rstargetAddr=inst.disp;
    	}else{
    	//3형식
    		int ta = inst.disp + rMgr.register[8]; //disp+pc=TA
    		rMgr.register[8] = ta;
    		rMgr.rstargetAddr=ta;
    	}
    }
    
    public void JEQ(Instruction inst){
    	//PC값 변경
    	if(rMgr.register[9]!=0)
    		return; //같지 않다면 돌아간다
    	
    	//같은 경우
    	int ta = inst.disp + rMgr.register[8]; //disp+pc=TA
    	rMgr.rstargetAddr=ta;
		rMgr.register[8] = ta;
    }
    
    public void JLT(Instruction inst){
    	//PC값 변경
    	if(rMgr.register[9]>=0)
    		return; //작지 않다면 돌아간다
    	
    	//작은 경우
    	int ta = inst.disp + rMgr.register[8]; //disp+pc=TA
    	rMgr.rstargetAddr=ta;
		rMgr.register[8] = ta;
    }
    
    public void J(Instruction inst){
    	//PC값 변경
    	if((inst.nixbpe&48)==48){
    		//n=i=1
    		//target address 설정
    		int ta = inst.disp + rMgr.register[8]; //disp+pc=TA
    		rMgr.rstargetAddr=ta;
    		rMgr.register[8] = ta;
    		 		
    	}else if((inst.nixbpe&32)==32){
    		//@=1 indirect
    		int ta = inst.disp + rMgr.register[8]; //disp+pc=TA
    		char[] arr = rMgr.getMemory(ta, 3);
    		ta = rMgr.byteToInt(arr); //indirect이므로 이 주소로 가야한다
    		rMgr.rstargetAddr=ta;
    		rMgr.register[8] = ta; //target address로 pc값 변경
    	}
    }
    
    public void CLEAR(Instruction inst){
    	//2형식
    	rMgr.register[inst.reg1]=0;//해당 register를 clear한다
    }
    
    public void COMP(Instruction inst){
    	//target과 A와 값 비교 후 SW reg 세팅
    	int ta;
    	int value=0;
    	if((inst.nixbpe&48)==48){
    		//n=i=1
    		//target address 설정
    		ta = inst.disp + rMgr.register[8]; //disp+pc=TA
    		rMgr.rstargetAddr=ta;
    		char[] arr = rMgr.getMemory(ta, 3);
    		value = rMgr.byteToInt(arr);
    		
    		
    	}else if((inst.nixbpe&16)==16){
    		//i=1, target address 설정 필요 X
    		value = inst.disp;
    	}
    	rMgr.register[9]=rMgr.register[0]-value;
    }
    
    public void COMPR(Instruction inst){
    	//2형식, reg1과 reg2를 비교한다.
    	rMgr.register[9]=rMgr.register[inst.reg1]-rMgr.register[inst.reg2];
    }
    
    public void TIXR(Instruction inst){
    	//2형식
    	rMgr.register[1] += 1; //X하나 증가
    	rMgr.register[9]=rMgr.register[1]-rMgr.register[inst.reg1];//reg1과 비교
    	//reg1값이 들어가는게 아니라 reg1 index가 들어가는 거임. reg1이 X인지 A인지를 알려주는 것
    	//SW에 (bit로 저장되지는 않음) X가 작으면 -1 X가 크면 1 X랑 같으면 0이 저장된다
    }
    
    public void TD(Instruction inst) throws IOException{
    	//Test Device
    	//test할 device 이름은 한 byte만 읽어서 온다.
    	int ta = inst.disp + rMgr.register[8]; //ta 설정
    	rMgr.rstargetAddr=ta;
    	char[] buf = rMgr.getMemory(ta,1);
    	//16진수로 고쳐야함
    	String devName = String.format("%02X",(int)buf[0]);
    	rMgr.rsusingDeviceName=devName;
    	rMgr.testDevice(devName);
    	rMgr.register[9]=1; //test 준비가 되었으니 sw를 1로 만든다
    }
    
    public void RD(Instruction inst) throws IOException{
    	//Read Device
    	int ta = inst.disp + rMgr.register[8]; //ta 설정
    	rMgr.rstargetAddr=ta;
    	char[] buf = rMgr.getMemory(ta,1);
    	String devName = String.format("%02X",(int)buf[0]);
    	rMgr.rsusingDeviceName=devName;
    	buf = rMgr.readDevice(devName, 1); //char 하나만 읽는다
    	if(buf==null)
    		rMgr.register[0]=0; //끝나면 null을 return하므로 0을 register에 넣는다
    	else
    		rMgr.register[0]=(int)buf[0];
    }
    
    public void WD(Instruction inst) throws IOException{
    	//Write Device
    	int ta = inst.disp + rMgr.register[8]; //ta 설정
    	rMgr.rstargetAddr=ta;
    	char[] buf = rMgr.getMemory(ta,1);
    	String devName = String.format("%02X",(int)buf[0]);
    	rMgr.rsusingDeviceName=devName;
    	buf = new char[1];
    	buf[0] = (char)rMgr.register[0]; //A reg 값을 그대로 넣는다.

    	rMgr.writeDevice(devName, buf, 1); //A register 값 중 끝에 1byte만 파일에 쓴다
    }
    
    public void RSUB(Instruction inst){
    	//L값->PC값에 저장
    	rMgr.register[8]=rMgr.register[2]; //caller로 돌아감
    	rMgr.register[2]=rMgr.subReturnAddr; //caller가 또 돌아갈 주소 값 복구
    }


    
    
}