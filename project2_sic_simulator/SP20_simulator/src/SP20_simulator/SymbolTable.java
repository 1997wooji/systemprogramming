package SP20_simulator;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * symbol�� ���õ� �����Ϳ� ������ �����Ѵ�.
 * section ���� �ϳ��� �ν��Ͻ��� �Ҵ��Ѵ�.
 */
public class SymbolTable {
	ArrayList<String> symbolList;
	ArrayList<Integer> addressList;
	// ��Ÿ literal, external ���� �� ó������� �����Ѵ�.
	
	public SymbolTable(){
		symbolList = new ArrayList<String>();
		addressList = new ArrayList<Integer>();
	}
	

	/**
	 * ���ο� Symbol�� table�� �߰��Ѵ�.
	 * @param symbol : ���� �߰��Ǵ� symbol�� label
	 * @param address : �ش� symbol�� ������ �ּҰ�
	 * 
	 * ���� : ���� �ߺ��� symbol�� putSymbol�� ���ؼ� �Էµȴٸ� �̴� ���α׷� �ڵ忡 ������ ������ ��Ÿ����. 
	 * ��Ī�Ǵ� �ּҰ��� ������ modifySymbol()�� ���ؼ� �̷������ �Ѵ�.
	 */
	public void putSymbol(String symbol, int address){
		StringTokenizer tk = new StringTokenizer(symbol, " "); //Ȥ�ó� ���Ⱑ ���� ���, ���ֱ� ����
		symbolList.add(tk.nextToken());
		//�̹��� �ߺ��� symbol�� �־ �����ؾ� �Ѵ�.
		//section���� table�� �ϳ��� ����� ���� �ƴ϶� table�� �ϳ��̱� ����(�������� �����Ͻ�)
		addressList.add(address);
	}
	
	/**
	 * ������ �����ϴ� symbol ���� ���ؼ� ����Ű�� �ּҰ��� �����Ѵ�.
	 * @param symbol : ������ ���ϴ� symbol�� label
	 * @param newaddress : ���� �ٲٰ��� �ϴ� �ּҰ�
	 */
	public void modifySymbol(String symbol, int newaddress) {
		int index = symbolList.indexOf(symbol);
		if(index>=0)
			addressList.set(index, newaddress);
	}
	
	/**
	 * ���ڷ� ���޵� symbol�� � �ּҸ� ��Ī�ϴ��� �˷��ش�. 
	 * @param symbol : �˻��� ���ϴ� symbol�� label
	 * @return symbol�� ������ �ִ� �ּҰ�. �ش� symbol�� ���� ��� -1 ����
	 */
	public int search(String symbol) {
		StringTokenizer tk = new StringTokenizer(symbol, " "); //Ȥ�ó� ���Ⱑ ���� ���, ���ֱ� ����
		int address = 0;
		int index = symbolList.indexOf(tk.nextToken());
		if(index<0)
			address = -1;
		else
			address = addressList.get(index);
		return address;
	}
	
	
	
}
