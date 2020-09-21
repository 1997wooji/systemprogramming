package SP20_simulator;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.io.File;
import java.io.IOException;

/**
 * VisualSimulator는 사용자와의 상호작용을 담당한다.<br>
 * 즉, 버튼 클릭등의 이벤트를 전달하고 그에 따른 결과값을 화면에 업데이트 하는 역할을 수행한다.<br>
 * 실제적인 작업은 SicSimulator에서 수행하도록 구현한다.
 */
public class VisualSimulator extends JFrame{
	
	ResourceManager resourceManager = new ResourceManager();
	SicLoader sicLoader = new SicLoader(resourceManager);
	SicSimulator sicSimulator = new SicSimulator(resourceManager);
	
	private static VisualSimulator mainFrame;
	
	static{
		mainFrame=new VisualSimulator();
	}
	
	private JPanel contentPane;
	
	/*step마다 update될 textfield*/
	JTextField fileName;
	JTextField programName;
	JTextField stObjectProgram;
	JTextField lthProgram;
	JTextField decA;
	JTextField hexA;
	JTextField decX;
	JTextField hexX;
	JTextField decL;
	JTextField hexL;
	JTextField decB;
	JTextField hexB;
	JTextField decS;
	JTextField hexS;
	JTextField decT;
	JTextField hexT;
	JTextField decF;
	JTextField hexF;
	JTextField decPC;
	JTextField hexPC;
	JTextField decSW;
	JTextField hexSW;
	JTextField addrOfFirst;
	JTextField stAddrMemory;
	JTextField targetAddr;
	JTextArea instruction;
	JTextField usingDevice;
	JTextArea log;
	
	
	/**
	 * 프로그램 로드 명령을 전달한다.
	 */
	public void load(File program) throws IOException{
		//...
		sicSimulator.load(program); //여기서 메모리 initialize
		sicLoader.load(program);		
	};

	/**
	 * 하나의 명령어만 수행할 것을 SicSimulator에 요청한다.
	 */
	public int oneStep(){
		try{
			if(sicSimulator.oneStep()>0){
				update();
				return 1;
			}else{
				//program이 끝난 경우
				resourceManager.closeDevice();
				JOptionPane.showMessageDialog(contentPane, "더 이상 실행할 코드가 없어 종료합니다.");
				return -1;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return -1;
	};

	/**
	 * 남아있는 모든 명령어를 수행할 것을 SicSimulator에 요청한다.
	 */
	public void allStep(){
		try{
		while(oneStep()>0)
			;
		}catch(Exception e){
			e.printStackTrace();
		}
	};
	
	/**
	 * 화면을 최신값으로 갱신하는 역할을 수행한다.
	 */
	public void update(){
		
		decA.setText(String.format("%d", resourceManager.register[0]));
		hexA.setText(String.format("%06X", resourceManager.register[0]));
		decX.setText(String.format("%d", resourceManager.register[1]));
		hexX.setText(String.format("%06X", resourceManager.register[1]));
		decL.setText(String.format("%d", resourceManager.register[2]));
		hexL.setText(String.format("%06X", resourceManager.register[2]));
		decB.setText(String.format("%d", resourceManager.register[3]));
		hexB.setText(String.format("%06X", resourceManager.register[3]));
		decS.setText(String.format("%d", resourceManager.register[4]));
		hexS.setText(String.format("%06X", resourceManager.register[4]));
		decT.setText(String.format("%d", resourceManager.register[5]));
		hexT.setText(String.format("%06X", resourceManager.register[5]));
		decF.setText(String.format("%d", resourceManager.register[6]));
		hexF.setText(String.format("%06X", resourceManager.register[6]));
		decPC.setText(String.format("%d", resourceManager.register[8]));
		hexPC.setText(String.format("%06X", resourceManager.register[8]));
		decSW.setText(String.format("%d", resourceManager.register[9]));
		hexSW.setText(String.format("%06X", resourceManager.register[9]));
		
		//target address가 바뀌는 경우에만 Target Addr 값이 바뀐다(immediate일 경우, TA가 없으므로 바뀌지 않음)
		//Device도 마찬가지로 바뀌는 경우에만 바뀐다. 그렇지 않을 경우 유지된다.
		stAddrMemory.setText(String.format("%06X", resourceManager.rsstAddrMemory));
		targetAddr.setText(String.format("%06X", resourceManager.rstargetAddr));
		usingDevice.setText(resourceManager.rsusingDeviceName);
		
		//얘네는 하나씩 항목을 추가해야함
		instruction.append(sicSimulator.curInstruction+"\n"); // instruction 내용을 붙이고
		instruction.setCaretPosition(instruction.getDocument().getLength()); //맨 아래로 스크롤한다.
		log.append(sicSimulator.curLog+"\n");
		log.setCaretPosition(log.getDocument().getLength());
		
	};
	
	/*load 후 file 정보와 program 정보를 update하는 함수*/
	public void updateFileInfo(String file){
		fileName.setText(file);
		programName.setText(resourceManager.programName);
		String stobpr = String.format("%06X", resourceManager.programStartAddress);
		stObjectProgram.setText(stobpr);
		String lthp = String.format("%06X", resourceManager.programLength);
		lthProgram.setText(lthp);
		String adf = String.format("%06X", resourceManager.addrOfFirstInst);
		addrOfFirst.setText(adf);
	}
	
	public void stop(){
		try{
		resourceManager.closeDevice();
		JOptionPane.showMessageDialog(contentPane, "프로그램을 종료합니다.");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static VisualSimulator getInstance(){
		return mainFrame;
	}

	public static void main(String[] args) {
		
		EventQueue.invokeLater(new Runnable(){
			public void run(){
				try{
					VisualSimulator frame = mainFrame;
					frame.setVisible(true);}
				catch(Exception e){
					e.printStackTrace();
				}
			}
		});
	}
	
	public VisualSimulator(){ //생성자, GUI 화면 구성
		super("SIC/XE Simulator by 20160458");
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //윈도우 종료시 강제 종료
		setBounds(200,200,540,640); //위치와 크기 설정
		contentPane=new JPanel(new BorderLayout());
		
		JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT,15,5));
		JLabel lblFileName = new JLabel("File Name : ");
		fileName = new JTextField(15);
		JButton btnFileOpen=new JButton("open");
		filePanel.add(lblFileName);
		filePanel.add(fileName);
		filePanel.add(btnFileOpen);
		
		JPanel leftPanel = new JPanel(new FlowLayout());
		leftPanel.setPreferredSize(new Dimension(260,400));
		JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,10,5));
		headerPanel.setPreferredSize(new Dimension(260,130));
		headerPanel.setBorder(new TitledBorder(new EtchedBorder(), "H (Header Record)"));
		JLabel lblProgramName = new JLabel("Program Name : ");
		programName = new JTextField(10);
		JLabel lblStObjectProgram = new JLabel("<html>Start Address of <br>Object Program : <html>");
		stObjectProgram = new JTextField(10);
		JLabel lblLthProgram = new JLabel("Length of Program : ");
		lthProgram = new JTextField(8);
		
		headerPanel.add(lblProgramName);
		headerPanel.add(programName);
		headerPanel.add(lblStObjectProgram);
		headerPanel.add(stObjectProgram);
		headerPanel.add(lblLthProgram);
		headerPanel.add(lthProgram);
		
		
		JPanel registerPanel = new JPanel(new GridLayout(10,3));
		registerPanel.setPreferredSize(new Dimension(260,250));
		registerPanel.setBorder(new TitledBorder(new EtchedBorder(), "Register"));
		JLabel lblDec = new JLabel("Dec");
		JLabel lblHex = new JLabel("Hex");
		JLabel lblA = new JLabel("A(#0) ");
		decA = new JTextField(6);
		hexA = new JTextField(6);
		JLabel lblX = new JLabel("X(#1) ");
		decX = new JTextField(6);
		hexX = new JTextField(6);
		JLabel lblL = new JLabel("L(#2)");
		decL = new JTextField(6);
		hexL = new JTextField(6);
		JLabel lblB = new JLabel("B(#3)");
		decB = new JTextField(6);
		hexB = new JTextField(6);
		JLabel lblS = new JLabel("S(#4)");
		decS = new JTextField(6);
		hexS = new JTextField(6);
		JLabel lblT = new JLabel("T(#5)");
		decT = new JTextField(6);
		hexT = new JTextField(6);
		JLabel lblF = new JLabel("F(#6)");
		decF = new JTextField(6);
		hexF = new JTextField(6);
		JLabel lblPC = new JLabel("PC(#8)");
		decPC = new JTextField(6);
		hexPC = new JTextField(6);
		JLabel lblSW = new JLabel("SW(#9)");
		decSW = new JTextField(6);
		hexSW = new JTextField(6);
		registerPanel.add(Box.createHorizontalGlue());
		registerPanel.add(lblDec);
		registerPanel.add(lblHex);
		registerPanel.add(lblA);
		registerPanel.add(decA);
		registerPanel.add(hexA);
		registerPanel.add(lblX);
		registerPanel.add(decX);
		registerPanel.add(hexX);
		registerPanel.add(lblL);
		registerPanel.add(decL);
		registerPanel.add(hexL);
		registerPanel.add(lblB);
		registerPanel.add(decB);
		registerPanel.add(hexB);
		registerPanel.add(lblS);
		registerPanel.add(decS);
		registerPanel.add(hexS);
		registerPanel.add(lblT);
		registerPanel.add(decT);
		registerPanel.add(hexT);
		registerPanel.add(lblF);
		registerPanel.add(decF);
		registerPanel.add(hexF);
		registerPanel.add(lblPC);
		registerPanel.add(decPC);
		registerPanel.add(hexPC);
		registerPanel.add(lblSW);
		registerPanel.add(decSW);
		registerPanel.add(hexSW);
		
		leftPanel.add(headerPanel);
		leftPanel.add(registerPanel);
		
		JPanel rightPanel = new JPanel(new FlowLayout());
		rightPanel.setPreferredSize(new Dimension(260,400));
		JPanel endPanel = new JPanel(new FlowLayout());
		endPanel.setPreferredSize(new Dimension(260,90));
		endPanel.setBorder(new TitledBorder(new EtchedBorder(), "E (End Record)"));
		JLabel lblAddrOfFirst = new JLabel("<html>Address of First Instruction<br> in Object Program : <html>");
		addrOfFirst = new JTextField(6);
		endPanel.add(lblAddrOfFirst);
		endPanel.add(addrOfFirst);
		
		JPanel instructionPanel = new JPanel(new BorderLayout());
		instructionPanel.setPreferredSize(new Dimension(260,300));
		
		JPanel instNorthPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,10,5));
		instNorthPanel.setPreferredSize(new Dimension(260,60));
		JLabel lblStAddrMemory = new JLabel("Start Address in Memory ");
		stAddrMemory = new JTextField(8);
		JLabel lblTargetAddr = new JLabel("Target Address : ");
		targetAddr = new JTextField(12);
		instNorthPanel.add(lblStAddrMemory);
		instNorthPanel.add(stAddrMemory);
		instNorthPanel.add(lblTargetAddr);
		instNorthPanel.add(targetAddr);
		
		JPanel instWestPanel = new JPanel(new FlowLayout());
		instWestPanel.setPreferredSize(new Dimension(130,250));
		JLabel lblInstructions = new JLabel("Instructions : ");
		instruction = new JTextArea(11,10);
		JScrollPane instscroll = new JScrollPane(instruction);
		instWestPanel.add(lblInstructions);
		instWestPanel.add(instscroll);
		
		JPanel instEastPanel = new JPanel(new FlowLayout());
		instEastPanel.setPreferredSize(new Dimension(130,250));
		JLabel lblUsingDevice = new JLabel("사용중인 장치");
		usingDevice = new JTextField(6);
		JButton btnOneStep=new JButton("실행(1step)");
		btnOneStep.setPreferredSize(new Dimension(100,30));
		JButton btnAllStep=new JButton("실행(All)");
		btnAllStep.setPreferredSize(new Dimension(100,30));
		JButton btnStop=new JButton("종료");
		btnStop.setPreferredSize(new Dimension(100,30));
		instEastPanel.add(lblUsingDevice);
		instEastPanel.add(usingDevice);
		instEastPanel.add(btnOneStep);
		instEastPanel.add(btnAllStep);
		instEastPanel.add(btnStop);
		
		
		instructionPanel.add(instNorthPanel,BorderLayout.NORTH);
		instructionPanel.add(instWestPanel,BorderLayout.WEST);
		instructionPanel.add(instEastPanel,BorderLayout.EAST);
		
		rightPanel.add(endPanel);
		rightPanel.add(instructionPanel);
		
		JPanel logPanel = new JPanel(new FlowLayout());
		logPanel.setPreferredSize(new Dimension(520,180));
		JLabel lblLog = new JLabel("Log(명령어 수행 관련) : ");
		log = new JTextArea(7,45);
		JScrollPane logscroll = new JScrollPane(log);
		logPanel.add(lblLog);
		logPanel.add(logscroll);
		contentPane.add(filePanel,BorderLayout.NORTH);
		contentPane.add(leftPanel,BorderLayout.WEST);
		contentPane.add(rightPanel,BorderLayout.EAST);
		contentPane.add(logPanel,BorderLayout.SOUTH);
		
		btnFileOpen.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent event){
				JFileChooser chooser = new JFileChooser();
				int ret = chooser.showOpenDialog(null);
				if(ret==JFileChooser.APPROVE_OPTION){
					try{
						load(chooser.getSelectedFile());
						updateFileInfo(chooser.getSelectedFile().getName());
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
		});
		
		btnOneStep.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent event){
				oneStep();
			}
		});
		
		btnAllStep.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent event){
				allStep();
			}
		});
		
		/*종료 버튼은 지금 실행하는 program을 종료하는거지 이 UI를 끄는 것이 아님.
		 * UI를 끄려면 X 버튼을 눌러 나가야함.*/
		btnStop.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent event){
				stop();
			}
		});
		
		setContentPane(contentPane);
	}
}

