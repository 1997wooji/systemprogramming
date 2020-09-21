/* 
 * my_assembler 함수를 위한 변수 선언 및 매크로를 담고 있는 헤더 파일이다.
 */
#define MAX_INST 256
#define MAX_LINES 5000
#define MAX_OPERAND 3
#define MAX_SECTIONS 5 //최대 control section 개수
#define MAX_NAME 6 //최대 변수 이름 길이
#define MAX_COUNT 4096 //input data 한줄 읽을 때 최대 문자 수

/*
 * instruction 목록 파일로 부터 정보를 받아와서 생성하는 구조체 변수이다.
 * 구조는 각자의 instruction set의 양식에 맞춰 직접 구현하되
 * 라인 별로 하나의 instruction을 저장한다.
 */
struct inst_unit
{
	char *mnemonic; //문자 명령어(LDA, STA 등)
	char *format; // 명령어 형식 1, 2, 3(=3/4)
	int opcode; // 해당 문자 명령어의 opcode (+) int로 수정하였다
	char *n_operand; // 오퍼랜드의 개수 (0,1,2)
};

// instruction의 정보를 가진 구조체를 관리하는 테이블 생성(opcode)
typedef struct inst_unit inst;
inst *inst_table[MAX_INST]; 
int inst_index; // 지금까지 몇 개가 채워졌는지 알려주는 index

/*
 * 어셈블리 할 소스코드를 입력받는 테이블이다. 라인 단위로 관리할 수 있다.
 */
char *input_data[MAX_LINES];
static int line_num; // 지금까지 몇 라인 채워졌는지 저장하는 index

/*
 * 어셈블리 할 소스코드를 토큰단위로 관리하기 위한 구조체 변수이다.
 * operator는 renaming을 허용한다.
 * nixbpe는 8bit 중 하위 6개의 bit를 이용하여 n,i,x,b,p,e를 표시한다.
 */
struct token_unit
{
    char *label;                //명령어 라인 중 label
    char *operator_sym;         //명령어 라인 중 operator => operator symbol로 renaming했다.
    char *operand[MAX_OPERAND]; //명령어 라인 중 operand, 최대 MAX_OPERAND(=3)개
    char *comment;              //명령어 라인 중 comment
	char nixbpe;				//하위 6bit 사용: _ _ n i x b p e
};

// token들을 관리할 수 있는 table
typedef struct token_unit token;
token *token_table[MAX_LINES];
static int token_line; // 지금까지 몇개의 token_unit이 채워졌는지 알려주는 index

/*
 * 심볼을 관리하는 구조체이다.
 * 심볼 테이블은 심볼 이름, 심볼의 위치로 구성된다.
 */
struct symbol_unit
{
    char symbol[10];
    int addr;
};

typedef struct symbol_unit symbol;
symbol sym_table[MAX_LINES];
static int sym_index; //sym_table에 얼만큼 채워졌는지 저장하는 index

/*
* 리터럴을 관리하는 구조체이다.
* 리터럴 테이블은 리터럴의 이름, 리터럴의 위치로 구성된다.
*/
struct literal_unit
{
    char literal[10];
    int addr;
	char format; //(+)Char(C), Hex(X)인지 구분하기 위해 추가하였다.
	int section; //(+)이 literal이 어느 section에 있는지
};

typedef struct literal_unit literal;
literal literal_table[MAX_LINES];
static int literal_index; //sym_table에 얼만큼 채워졌는지 저장하는 index

static int locctr;
static int cs_length_table[MAX_SECTIONS]; //각 control section의 길이를 저장하는 table
int cs_index; //cs_length_table이 얼마나 채워졌는지 관리하는 index

char object_code[MAX_LINES][MAX_COUNT]; //Object code를 저장하는 buffer
static int object_index; // 지금까지 object code가 몇 라인 채워졌는지 저장하는 index

/*
* 레지스터-번호를 저장하는 구조체이다.
* 레지스터 이름과 그 번호로 구성된다.
*/
struct register_unit {
	char * name;
	int number;
};

typedef struct register_unit regi;
regi register_table[MAX_LINES]; //레지스터 정보들을 담고있는 table
static int regi_index;

/*
* modify record를 저장하기 위한 구조체
*/
struct m_record_unit {
	int addr; //수정해야할 주소
	int length; //수정해야할 (column)길이
	char plus; //주소를 더해야하는지, 빼야하는지 기호
	char name[MAX_NAME + 1]; //더해야할 주소의 변수 이름
	int section; //해당 record가 존재하는 section
};

typedef struct m_record_unit m_record;
m_record modify_table[MAX_LINES]; //M record 정보를 담고 있는 table
static int m_index;

//------ 기본 함수 --------

static char *input_file;
static char *output_file;
int init_my_assembler(void);
int init_inst_file(char *inst_file);
int init_input_file(char *input_file);
int token_parsing(char *str);
int search_opcode(char *str);
static int assem_pass1(void);
void make_opcode_output(char *file_name); //현재 프로젝트에서는 쓰이지 않음

void make_symtab_output(char *file_name);
void make_literaltab_output(char *file_name);
static int assem_pass2(void);
void make_objectcode_output(char *file_name);
 
//------ 추가 함수 --------
int search_n_operand(char *str); //명령어(mnemonic)를 주면 operand 개수를 리턴하는 함수
void init_token(int index); //token을 처음 NULL들로 초기화해주는 함수
void data_free(); //동적할당한 데이터들을 모두 해제하는 함수
int search_n_byte(char *str); //명령어(mnemonic)를 주면 해당 명령어의 byte를 리턴하는 함수
int search_literal_table(char *str); //literal을 주면 해당 literal의 주소를 반환하는 함수
void add_literal_addr(); //literal_table의 literal에 address를 할당하는 함수
int symbol_address(char *str, int cs); //symbol 이름을 주면 address를 반환하는 함수
void init_register_number(); //register table을 초기화하는 함수
int search_register(char *x); //register의 number를 알려주는 함수
int add_text_record(char * buf, char *record, char * object, int * tot_length_ptr, int byte);
//현재 instruction을 text record에 추가하는 함수
void fin_text_record(char * buf, char *record, int * tot_length_ptr); //text record를 object program에 적고 끝내는 함수
void add_modify_unit(int addr, int length, char plus, char *name, int section); //modify table에 수정할 유닛을 추가하는 함수