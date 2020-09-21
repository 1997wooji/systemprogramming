package SP20_simulator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;

/**
 * SicLoader�� ���α׷��� �ؼ��ؼ� �޸𸮿� �ø��� ������ �����Ѵ�. �� �������� linker�� ���� ���� �����Ѵ�. 
 * SicLoader�� �����ϴ� ���� ���� ��� ������ ����.
 * - program code�� �޸𸮿� �����Ű��
 * - �־��� ������ŭ �޸𸮿� �� ���� �Ҵ��ϱ�
 * - �������� �߻��ϴ� symbol, ���α׷� �����ּ�, control section �� ������ ���� ���� ���� �� ����
 */
public class SicLoader {
	ResourceManager rMgr;
	
	int locctr; //section�� locctr
	int csAddr; //current section address
	int csLength; //���� current section�� length
	
	public SicLoader(ResourceManager resourceManager) {
		// �ʿ��ϴٸ� �ʱ�ȭ
		locctr=0;
		csAddr=0;
		csLength=0;
		setResourceManager(resourceManager);
	}

	/**
	 * Loader�� ���α׷��� ������ �޸𸮸� �����Ų��.
	 * @param rMgr
	 */
	public void setResourceManager(ResourceManager resourceManager) {
		this.rMgr=resourceManager;
	}
	
	/**
	 * object code�� �о load������ �����Ѵ�. load�� �����ʹ� resourceManager�� �����ϴ� �޸𸮿� �ö󰡵��� �Ѵ�.
	 * load�������� ������� symbol table �� �ڷᱸ�� ���� resourceManager�� �����Ѵ�.
	 * @param objectCode �о���� ����
	 */
	public void load(File objectCode) throws IOException{
		//program code�� �޸𸮿� ����
		//pass1
		//�޸𸮿� �ö󰡴� �ּҴ� current section address + locctr�� �̷������
		BufferedReader br=new BufferedReader(new FileReader(objectCode));
		String line;
		while((line=br.readLine())!=null){
			switch(line.charAt(0)){
			case 'H':
				headRecord(line);
				break;
			case 'D':
				int i=1; //D record �����ϰ� �ϳ��� symtab�� �ִ´�
				while(i<line.length()){
					rMgr.symtabList.putSymbol(line.substring(i, i+6), Integer.parseInt(line.substring(i+6, i+12),16)); //hexstring to int
					i+=12;
				}
				break;
			case 'E':
				if(line.length()>1){
					//E���� �ڿ� �ּҰ� �ִٸ�
					//Address of First Instruction in Object Program�� ����
					rMgr.addrOfFirstInst=Integer.parseInt(line.substring(1, 7),16);
				}
				break;
				//���� record���� �ϴ� �ѱ��
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
		csAddr += csLength; //'����' csLength�� �����ϸ� ���� cs�� ���� �ּҰ� �ȴ�
		locctr = 0;
		//Program Name ����(�ȵǾ��ִٸ� ����)
		if(!rMgr.setName){
			rMgr.programName=line.substring(1,7);
			rMgr.setName=true;
		}
		//Start Address of Object Program ����(�ȵǾ��ִٸ� ����)
		if(!rMgr.setAddress){
			rMgr.programStartAddress=Integer.parseInt(line.substring(7,13),16);
			rMgr.setAddress=true;
			
		}		
		//section length �����ϰ� ���ϱ�
		rMgr.programLength += Integer.parseInt(line.substring(13,19),16);
		rMgr.sectionLength.add(Integer.parseInt(line.substring(13,19),16));
		csLength = Integer.parseInt(line.substring(13,19),16); //csLength update
		
		//�ش� Section Program symbolTable�� �ֱ�
		rMgr.symtabList.putSymbol(line.substring(1,7), csAddr);
	}
	
	public void textRecord(String line){
		//�̰Ŵ� �׳� 2�ڸ���(1byte��) �޸𸮿� �ø��⸸ �ϴ°��̹Ƿ� ���� �Ű� �Ƚᵵ ��
		locctr = Integer.parseInt(line.substring(1, 7),16); //hexstring to int
		int recordLength = Integer.parseInt(line.substring(7,9),16);
		int length=0;
		int i=9; //������� 2char�� 1byte�� ���� char[] memory�� �÷��� �Ѵ�.
		while(length < recordLength){
			char x = (char)Integer.parseInt(line.substring(i,i+2),16); //16���������Ƿ� ǥ�ø� ���ش�
			rMgr.memory[csAddr+locctr+length]=x;
			length++; //byte length�� �ϳ� �þ 1byte�þ ���̹Ƿ�
			i+=2; //i�� 2�� �þ��
		}
		locctr+=recordLength;
	}
	
	public void modifyRecord(String line){
		int origin = Integer.parseInt(line.substring(1, 7),16); //�ٲ� address
		//�ٲ� ���̴� 05�� 06�̳� 3byte�� �����;��ϴ°� ���������̹Ƿ� ������� �ʾҴ�.
		int changeDisp = rMgr.symtabList.search(line.substring(10));
		String str = "";
		
		for(int i=0; i<3;i++){
			str += String.format("%02X",(int)rMgr.memory[csAddr+origin+i]); //hex�� �޾ƾ���, 6�� �ǵ��� ���缭!
			//1byte�� �����ͼ� �ٽ� ��ħ
		}
		
		int originDisp = Integer.parseInt(str,16);
		
		if(line.charAt(9)=='+')
			originDisp += changeDisp;
		else
			originDisp -= changeDisp;
		
		//�ٽ� 1byte�� �ø���, int(10����)���¿����� 1byte�� �ø��� �����Ƿ� �ٽ� hexString���� �ٲ۴�
		str = String.format("%06X", originDisp);//**�����ڸ��� �ȵɼ��� ����** ��� ����� 0���� ä���
		int j=0;
		for(int i=0; i<3;i++){
			rMgr.memory[csAddr+origin+i]=(char)Integer.parseInt(str.substring(j, j+2),16);
			//i�� �ϳ���, j�� 2char=>1byte�� �������ϹǷ� 2�� �÷������ν� 1byte�� �����Ѵ�
			j+=2;
		}
	}	

}
