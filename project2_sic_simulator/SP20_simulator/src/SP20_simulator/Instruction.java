package SP20_simulator;

/* Instruction 정보를 저장하는 Class */
public class Instruction {
	int opcode;
	int nixbpe;
	int disp;
	int reg1;
	int reg2;
	
	public Instruction(){
		this.opcode=0;
		this.nixbpe=0;
		this.disp=0;
	}
	
	public Instruction(int opcode, int nixbpe, int disp){
		//3,4형식
		this.opcode = opcode;
		this.nixbpe = nixbpe;
		this.disp = disp;
	}
	
	public Instruction(int opcode, int reg1, int reg2, boolean flag){
		//2형식
		this.opcode = opcode;
		this.reg1=reg1;
		this.reg2=reg2;
	}
	
}
