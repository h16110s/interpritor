package newlang3;

import java.util.Map;
import java.util.HashMap;
import java.io.*;

public class LexicalAnalyzerImpl implements LexicalAnalyzer {

	//クラス変数
	PushbackReader r;
	private static final Map RESERVED_WORD=new HashMap();
	private static final Map SYMBOLS=new HashMap();

	static {	//スタティックイニシャライザ　最初に１度だけ実行
		RESERVED_WORD.put("if",LexicalType.IF);
		RESERVED_WORD.put("then",LexicalType.THEN);
		RESERVED_WORD.put("else",LexicalType.ELSE);
		RESERVED_WORD.put("elseif",LexicalType.ELSEIF);
		RESERVED_WORD.put("endif",LexicalType.ENDIF);
		RESERVED_WORD.put("for",LexicalType.FOR);
		RESERVED_WORD.put("forall",LexicalType.FORALL);
		RESERVED_WORD.put("next",LexicalType.NEXT);
		RESERVED_WORD.put("func",LexicalType.FUNC);
		RESERVED_WORD.put("dim",LexicalType.DIM);
		RESERVED_WORD.put("as",LexicalType.AS);
		RESERVED_WORD.put("end",LexicalType.END);
		RESERVED_WORD.put("while",LexicalType.WHILE);
		RESERVED_WORD.put("do",LexicalType.DO);
		RESERVED_WORD.put("until",LexicalType.UNTIL);
		RESERVED_WORD.put("loop",LexicalType.LOOP);
		RESERVED_WORD.put("to",LexicalType.TO);
		RESERVED_WORD.put("wend",LexicalType.WEND);

		SYMBOLS.put('\n',LexicalType.NL);
		SYMBOLS.put('\r',LexicalType.NL);
		SYMBOLS.put(".",LexicalType.DOT);
		SYMBOLS.put("+",LexicalType.ADD);
		SYMBOLS.put("-",LexicalType.SUB);
		SYMBOLS.put("*",LexicalType.MUL);
		SYMBOLS.put("/",LexicalType.DIV);
		SYMBOLS.put("(",LexicalType.LP);
		SYMBOLS.put(")",LexicalType.RP);
		SYMBOLS.put(",",LexicalType.COMMA);
		SYMBOLS.put('=',LexicalType.EQ);
		SYMBOLS.put("<",LexicalType.LT);
		SYMBOLS.put("<=",LexicalType.LE);
		SYMBOLS.put("=<",LexicalType.LE);
		SYMBOLS.put("<>",LexicalType.NE);
		SYMBOLS.put(">",LexicalType.GT);
		SYMBOLS.put(">=",LexicalType.GE);
		SYMBOLS.put("=>",LexicalType.GE);
	}

	public LexicalAnalyzerImpl(PushbackReader in){
		r=in;
	}

	public LexicalUnit get() throws Exception {
		CharType type;
		while((type=getNextCharType())==CharType.SKIP){
			r.read();						//いらないので読み進む
		}
		if (type==CharType.LETTER){
			return getString();
		} else if (type==CharType.DIGIT){
			return getNumber();
		} else if (type==CharType.LITERAL){
			return getLiteral();
		} else if (type==CharType.SYMBOL){
			return getSymbol();
		} else if (type==CharType.EOF){
			return new LexicalUnit(LexicalType.EOF);
		} else {
			System.out.println("解釈不能な文字を見つけました。");
			System.exit(-1);
		}
		throw new InternalError();
	}

	public boolean expect(LexicalType type) throws Exception{
		return false;
	}

	public void unget(LexicalUnit token) throws Exception{
	}

	private LexicalUnit getLiteral(){	//先頭が"または'の場合
		String s="";					//結果文字列格納用。前後の\"や\'は入れないようにする。
		char c;							//一時的な保存領域
		try {
			char openChar=(char)r.read();	//開いた記号。\"か\'のいずれかが入るはず
			while(getNextCharType()!=CharType.EOF){
				c=(char)r.read();
				if ((char)c==openChar){			//リテラル終端に到達
					break;
				} 
				s+=(char)c;
			}		
		} catch (Exception e){
			System.out.println("type判定中の例外:"+e);
		}
		return new LexicalUnit(LexicalType.LITERAL,new ValueImpl(s));
	}

	private LexicalUnit getString(){	//先頭がアルファベット
		String target="";
		while(getNextCharType()==CharType.LETTER || getNextCharType()==CharType.DIGIT){
			try {
				target+=(char)r.read();
			} catch (Exception e){
				System.out.println("getStringでの例外:"+e);
			}
		}
		if (RESERVED_WORD.containsKey(target.toLowerCase())==true){		//予約語
			return new LexicalUnit((LexicalType)RESERVED_WORD.get(target.toLowerCase()));
		} else {											//予約語以外＝変数名など
			return new LexicalUnit(LexicalType.NAME,new ValueImpl(target));
		}
	}

	private LexicalUnit getNumber(){	//先頭が数字の場合
		String s="";					//結果文字列格納用。前後の\"や\'は入れない
		char c;							//一時的な保存領域
		boolean dp=false;				//小数点を見つけたらtrueにする
		try {
			while(true){
				if (getNextCharType()==CharType.DIGIT){		//数字
					s+=(char)r.read();
				} else if (getNextCharType()==CharType.SYMBOL){
					c=(char)r.read();
					if (c=='.' && dp==false){			//小数点
						dp=true;
						s+=c;
					} else {
						r.unread(c);
						break;
					}
				} else {									//その他
					break;
				}
			}
		} catch (Exception e){
				System.out.println("getNumberでの例外:"+e);
		}
		ValueImpl v=new ValueImpl(s);
		LexicalUnit lu;
		if(dp){
			lu=new LexicalUnit(LexicalType.DOUBLEVAL,v);
		} else {
			lu=new LexicalUnit(LexicalType.INTVAL,v);
		}
		return lu;
	}

	private LexicalUnit getSymbol(){	//先頭が記号
		char c,c2=0;
		LexicalUnit lu=null;
		try {
			c=(char)r.read();
			if (getNextCharType()==CharType.SYMBOL){		//次も記号
				c2=(char)r.read();
			}
			if (SYMBOLS.get(""+c+c2)!=null){
				lu=new LexicalUnit((LexicalType)SYMBOLS.get(""+c+c2));
			} else {
				if (c2!=0){
					r.unread(c2);									//２文字目はいらない
				}
				lu=new LexicalUnit((LexicalType)SYMBOLS.get(c));
			}
		} catch (Exception e){
			System.out.println("getSimbolでの例外:"+e);
			System.exit(-1);
		}
		return lu;
	}

	private CharType getNextCharType(){
		int ci=0;
		try {
			ci=r.read();
			if (ci<0){
				return CharType.EOF;
			}
			r.unread(ci);
		} catch (Exception e){
			System.out.println("getNextCharType内での例外:"+e);
			System.exit(-1);
		}
		char c=(char)ci;
		if (c==' ' || c=='\t'){
			return CharType.SKIP;
		} else if (Character.isLetter(c)){
			return CharType.LETTER;
		} else if (Character.isDigit(c)){
			return CharType.DIGIT;
		} else if (c=='\"' || c=='\''){
			return CharType.LITERAL;
		} else if (SYMBOLS.containsKey((char)c)==true){
			return CharType.SYMBOL;
		}
		return CharType.OTHER;
	}
}
