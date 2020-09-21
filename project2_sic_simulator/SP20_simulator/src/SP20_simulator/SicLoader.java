package SP20_simulator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;

/**
 * SicLoader는 프로그램을 해석해서 메모리에 올리는 역할을 수행한다. 이 과정에서 linker의 역할 또한 수행한다. 
 * SicLoader가 수행하는 일을 예를 들면 다음과 같다.
 * - program code를 메모리에 적재시키기
 * - 주어진 공간만큼 메모리에 빈 공간 할당하기
 * - 과정에서 발생하는 symbol, 프로그램 시작주소, control section 등 실행을 위한 정보 생성 및 관리
 */
public class SicLoader {
	ResourceManager rMgr;
	
	int locctr; //section의 locctr
	int csAddr; //current section address
	int csLength; //현재 current section의 length
	
	public SicLoader(ResourceManager resourceManager) {
		// 필요하다면 초기화
		locctr=0;
		csAddr=0;
		csLength=0;
		setResourceManager(resourceManager);
	}

	/**
	 * Loader와 프로그램을 적재할 메모리를 연결시킨다.
	 * @param rMgr
	 */
	public void setResourceManager(ResourceManager resourceManager) {
		this.rMgr=resourceManager;
	}
	
	/**
	 * object code를 읽어서 load과정을 수행한다. load한 데이터는 resourceManager가 관리하는 메모리에 올라가도록 한다.
	 * load과정에서 만들어진 symbol table 등 자료구조 역시 resourceManager에 전달한다.
	 * @param objectCode 읽어들인 파일
	 */
	public void load(File objectCode) throws IOException{
		//program code를 메모리에 적재
		//pass1
		//메모리에 올라가는 주소는 current section address + locctr로 이루어진다
		BufferedReader br=new BufferedReader(new FileReader(objectCode));
		String line;
		while((line=br.readLine())!=null){
			switch(line.charAt(0)){
			case 'H':
				headRecord(line);
				break;
			case 'D':
				int i=1; //D record 제외하고 하나씩 symtab에 넣는다
				while(i<line.length()){
					rMgr.symtabList.putSymbol(line.substring(i, i+6), Integer.parseInt(line.substring(i+6, i+12),16)); //hexstring to int
					i+=12;
				}
				break;
			case 'E':
				if(line.length()>1){
					//E말고 뒤에 주소가 있다면
					//Address of First Instruction in Object Program을 설정
					rMgr.addrOfFirstInst=Integer.parseInt(line.substring(1, 7),16);
				}
				break;
				//남은 record들은 일단 넘긴다
			}
		}
		br.close();
		
		//pass2
		br=new BufferedReader(new FileReader(objectCode));
		while((line=br.readLine())!=null){
			switch(line.charAt(0)){
			case 'H':
				csAddr = rMgr.symtabList.search(line.substring(1, 7));
				break;
			case 'T':
				textRecord(line);
				break;
			case 'M':
				modifyRecord(line);
				break;
			}
		}
		br.close();
	}
	
	public void headRecord(String line){
		csAddr += csLength; //'이전' csLength를 누적하면 현재 cs의 시작 주소가 된다
		locctr = 0;
		//Program Name 설정(안되어있다면 설정)
		if(!rMgr.setName){
			rMgr.programName=line.substring(1,7);
			rMgr.setName=true;
		}
		//Start Address of Object Program 설정(안되어있다면 설정)
		if(!rMgr.setAddress){
			rMgr.programStartAddress=Integer.parseInt(line.substring(7,13),16);
			rMgr.setAddress=true;
			
		}		
		//section length 저장하고 더하기
		rMgr.programLength += Integer.parseInt(line.substring(13,19),16);
		rMgr.sectionLength.add(Integer.parseInt(line.substring(13,19),16));
		csLength = Integer.parseInt(line.substring(13,19),16); //csLength update
		
		//해당 Section Program symbolTable에 넣기
		rMgr.symtabList.putSymbol(line.substring(1,7), csAddr);
	}
	
	public void textRecord(String line){
		//이거는 그냥 2자리씩(1byte씩) 메모리에 올리기만 하는것이므로 음수 신경 안써도 됨
		locctr = Integer.parseInt(line.substring(1, 7),16); //hexstring to int
		int recordLength = Integer.parseInt(line.substring(7,9),16);
		int length=0;
		int i=9; //여기부터 2char를 1byte로 만들어서 char[] memory에 올려야 한다.
		while(length < recordLength){
			char x = (char)Integer.parseInt(line.substring(i,i+2),16); //16진수였으므로 표시를 해준다
			rMgr.memory[csAddr+locctr+length]=x;
			length++; //byte length는 하나 늘어남 1byte늘어난 것이므로
			i+=2; //i는 2씩 늘어난다
		}
		locctr+=recordLength;
	}
	
	public void modifyRecord(String line){
		int origin = Integer.parseInt(line.substring(1, 7),16); //바꿀 address
		//바꿀 길이는 05나 06이나 3byte를 가져와야하는건 마찬가지이므로 계산하지 않았다.
		int changeDisp = rMgr.symtabList.search(line.substring(10));
		String str = "";
		
		for(int i=0; i<3;i++){
			str += String.format("%02X",(int)rMgr.memory[csAddr+origin+i]); //hex로 받아야함, 6개 되도록 맞춰서!
			//1byte씩 가져와서 다시 합침
		}
		
		int originDisp = Integer.parseInt(str,16);
		
		if(line.charAt(9)=='+')
			originDisp += changeDisp;
		else
			originDisp -= changeDisp;
		
		//다시 1byte씩 올린다, int(10진수)상태에서는 1byte씩 올리기 어려우므로 다시 hexString으로 바꾼다
		str = String.format("%06X", originDisp);//**여섯자리가 안될수도 있음** 고로 빈공간 0으로 채우기
		int j=0;
		for(int i=0; i<3;i++){
			rMgr.memory[csAddr+origin+i]=(char)Integer.parseInt(str.substring(j, j+2),16);
			//i는 하나씩, j는 2char=>1byte로 만들어야하므로 2씩 늘려줌으로써 1byte씩 저장한다
			j+=2;
		}
	}	

}
