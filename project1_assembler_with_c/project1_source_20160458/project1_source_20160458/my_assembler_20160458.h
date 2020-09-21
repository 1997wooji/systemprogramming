/* 
 * my_assembler �Լ��� ���� ���� ���� �� ��ũ�θ� ��� �ִ� ��� �����̴�.
 */
#define MAX_INST 256
#define MAX_LINES 5000
#define MAX_OPERAND 3
#define MAX_SECTIONS 5 //�ִ� control section ����
#define MAX_NAME 6 //�ִ� ���� �̸� ����
#define MAX_COUNT 4096 //input data ���� ���� �� �ִ� ���� ��

/*
 * instruction ��� ���Ϸ� ���� ������ �޾ƿͼ� �����ϴ� ����ü �����̴�.
 * ������ ������ instruction set�� ��Ŀ� ���� ���� �����ϵ�
 * ���� ���� �ϳ��� instruction�� �����Ѵ�.
 */
struct inst_unit
{
	char *mnemonic; //���� ��ɾ�(LDA, STA ��)
	char *format; // ��ɾ� ���� 1, 2, 3(=3/4)
	int opcode; // �ش� ���� ��ɾ��� opcode (+) int�� �����Ͽ���
	char *n_operand; // ���۷����� ���� (0,1,2)
};

// instruction�� ������ ���� ����ü�� �����ϴ� ���̺� ����(opcode)
typedef struct inst_unit inst;
inst *inst_table[MAX_INST]; 
int inst_index; // ���ݱ��� �� ���� ä�������� �˷��ִ� index

/*
 * ����� �� �ҽ��ڵ带 �Է¹޴� ���̺��̴�. ���� ������ ������ �� �ִ�.
 */
char *input_data[MAX_LINES];
static int line_num; // ���ݱ��� �� ���� ä�������� �����ϴ� index

/*
 * ����� �� �ҽ��ڵ带 ��ū������ �����ϱ� ���� ����ü �����̴�.
 * operator�� renaming�� ����Ѵ�.
 * nixbpe�� 8bit �� ���� 6���� bit�� �̿��Ͽ� n,i,x,b,p,e�� ǥ���Ѵ�.
 */
struct token_unit
{
    char *label;                //��ɾ� ���� �� label
    char *operator_sym;         //��ɾ� ���� �� operator => operator symbol�� renaming�ߴ�.
    char *operand[MAX_OPERAND]; //��ɾ� ���� �� operand, �ִ� MAX_OPERAND(=3)��
    char *comment;              //��ɾ� ���� �� comment
	char nixbpe;				//���� 6bit ���: _ _ n i x b p e
};

// token���� ������ �� �ִ� table
typedef struct token_unit token;
token *token_table[MAX_LINES];
static int token_line; // ���ݱ��� ��� token_unit�� ä�������� �˷��ִ� index

/*
 * �ɺ��� �����ϴ� ����ü�̴�.
 * �ɺ� ���̺��� �ɺ� �̸�, �ɺ��� ��ġ�� �����ȴ�.
 */
struct symbol_unit
{
    char symbol[10];
    int addr;
};

typedef struct symbol_unit symbol;
symbol sym_table[MAX_LINES];
static int sym_index; //sym_table�� ��ŭ ä�������� �����ϴ� index

/*
* ���ͷ��� �����ϴ� ����ü�̴�.
* ���ͷ� ���̺��� ���ͷ��� �̸�, ���ͷ��� ��ġ�� �����ȴ�.
*/
struct literal_unit
{
    char literal[10];
    int addr;
	char format; //(+)Char(C), Hex(X)���� �����ϱ� ���� �߰��Ͽ���.
	int section; //(+)�� literal�� ��� section�� �ִ���
};

typedef struct literal_unit literal;
literal literal_table[MAX_LINES];
static int literal_index; //sym_table�� ��ŭ ä�������� �����ϴ� index

static int locctr;
static int cs_length_table[MAX_SECTIONS]; //�� control section�� ���̸� �����ϴ� table
int cs_index; //cs_length_table�� �󸶳� ä�������� �����ϴ� index

char object_code[MAX_LINES][MAX_COUNT]; //Object code�� �����ϴ� buffer
static int object_index; // ���ݱ��� object code�� �� ���� ä�������� �����ϴ� index

/*
* ��������-��ȣ�� �����ϴ� ����ü�̴�.
* �������� �̸��� �� ��ȣ�� �����ȴ�.
*/
struct register_unit {
	char * name;
	int number;
};

typedef struct register_unit regi;
regi register_table[MAX_LINES]; //�������� �������� ����ִ� table
static int regi_index;

/*
* modify record�� �����ϱ� ���� ����ü
*/
struct m_record_unit {
	int addr; //�����ؾ��� �ּ�
	int length; //�����ؾ��� (column)����
	char plus; //�ּҸ� ���ؾ��ϴ���, �����ϴ��� ��ȣ
	char name[MAX_NAME + 1]; //���ؾ��� �ּ��� ���� �̸�
	int section; //�ش� record�� �����ϴ� section
};

typedef struct m_record_unit m_record;
m_record modify_table[MAX_LINES]; //M record ������ ��� �ִ� table
static int m_index;

//------ �⺻ �Լ� --------

static char *input_file;
static char *output_file;
int init_my_assembler(void);
int init_inst_file(char *inst_file);
int init_input_file(char *input_file);
int token_parsing(char *str);
int search_opcode(char *str);
static int assem_pass1(void);
void make_opcode_output(char *file_name); //���� ������Ʈ������ ������ ����

void make_symtab_output(char *file_name);
void make_literaltab_output(char *file_name);
static int assem_pass2(void);
void make_objectcode_output(char *file_name);
 
//------ �߰� �Լ� --------
int search_n_operand(char *str); //��ɾ�(mnemonic)�� �ָ� operand ������ �����ϴ� �Լ�
void init_token(int index); //token�� ó�� NULL��� �ʱ�ȭ���ִ� �Լ�
void data_free(); //�����Ҵ��� �����͵��� ��� �����ϴ� �Լ�
int search_n_byte(char *str); //��ɾ�(mnemonic)�� �ָ� �ش� ��ɾ��� byte�� �����ϴ� �Լ�
int search_literal_table(char *str); //literal�� �ָ� �ش� literal�� �ּҸ� ��ȯ�ϴ� �Լ�
void add_literal_addr(); //literal_table�� literal�� address�� �Ҵ��ϴ� �Լ�
int symbol_address(char *str, int cs); //symbol �̸��� �ָ� address�� ��ȯ�ϴ� �Լ�
void init_register_number(); //register table�� �ʱ�ȭ�ϴ� �Լ�
int search_register(char *x); //register�� number�� �˷��ִ� �Լ�
int add_text_record(char * buf, char *record, char * object, int * tot_length_ptr, int byte);
//���� instruction�� text record�� �߰��ϴ� �Լ�
void fin_text_record(char * buf, char *record, int * tot_length_ptr); //text record�� object program�� ���� ������ �Լ�
void add_modify_unit(int addr, int length, char plus, char *name, int section); //modify table�� ������ ������ �߰��ϴ� �Լ�