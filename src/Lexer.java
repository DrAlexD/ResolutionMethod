import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

class Lexer {
    static StringBuilder seq_lexemes = new StringBuilder();

    void lex(String arg) {
        Compiler compiler = new Compiler();
        String program = null;
        try {
            program = new String(Files.readAllBytes(Paths.get(arg)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Scanner scanner = compiler.getScanner(program);
        while (true) {
            Token token = scanner.nextToken();
            if (token.tag == DomainTag.QUESTION_SIGN || token.tag == DomainTag.DASH) {
                seq_lexemes.append("\n");
            }

            seq_lexemes.append(token.tag.text);

            if (token.tag == DomainTag.QUESTION_SIGN || token.tag == DomainTag.DASH) {
                seq_lexemes.append(" ");
            }

            Parser.tokens.add(token);
            if (token.tag == DomainTag.END_OF_PROGRAM) {
                break;
            }
        }

        System.out.println("Начальная задача:");
        System.out.println(program);
        compiler.outputMessages();
    }
}

class Scanner {
    public final String program;
    private final Compiler compiler;
    private Position cur;

    public Scanner(String program, Compiler compiler) {
        this.compiler = compiler;
        this.program = program;
        cur = new Position(program);
    }

    public Token nextToken() {
        while (!cur.isEOF()) {
            while (cur.isWhitespace())
                cur = cur.next();
            Token token = switch (cur.getCode()) {
                case '(' -> new LeftBracketToken(cur, cur.next());
                case ')' -> new RightBracketToken(cur, cur.next());
                case '?' -> new QuestionSignToken(cur, cur.next());
                case '~' -> new InversionToken(cur, cur.next());
                case '&' -> new ConjunctionToken(cur, cur.next());
                case '|' -> new DisjunctionToken(cur, cur.next());
                case '<' -> readIdentity(cur);
                case '-' -> readDashOrImplication(cur);
                default -> readVariable(cur);
            };
            cur = token.coords.following;
            if (token.tag != DomainTag.ERROR) {
                return token;
            }
        }
        return new EndOfProgramToken("", DomainTag.END_OF_PROGRAM, cur, cur);
    }

    private Token readIdentity(Position cur) {
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toChars(cur.getCode()));
        Position p = cur.next();

        if (p.isDash()) {
            sb.append(Character.toChars(cur.getCode()));
            p = p.next();

            if (p.isRightCornerBracket()) {
                sb.append(Character.toChars(cur.getCode()));
                p = p.next();

                return new IdentityToken(cur, p);
            } else {
                compiler.addMessage(true, cur, "Wrong identity_token: " + sb.toString());
                return new ErrorToken(sb.toString(), cur, p);
            }
        } else {
            compiler.addMessage(true, cur, "Wrong identity_token: " + sb.toString());
            return new ErrorToken(sb.toString(), cur, p);
        }
    }

    private Token readDashOrImplication(Position cur) {
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toChars(cur.getCode()));
        Position p = cur.next();

        if (p.isRightCornerBracket()) {
            sb.append(Character.toChars(cur.getCode()));
            p = p.next();

            return new ImplicationToken(cur, p);
        } else if (p.isWhitespace()) {
            p = p.next();

            return new DashToken(cur, p);
        } else {
            compiler.addMessage(true, cur, "Wrong implication_token: " + sb.toString());
            return new ErrorToken(sb.toString(), cur, p);
        }
    }

    private Token readVariable(Position cur) {
        StringBuilder sb = new StringBuilder();
        Position p = cur;
        if (p.isLetter()) {
            sb.append(Character.toChars(p.getCode()));
            p = p.next();

            while (p.isLetter()) {
                sb.append(Character.toChars(p.getCode()));
                p = p.next();
            }
            return new VariableToken(sb.toString(), cur, p);
        } else {
            compiler.addMessage(true, cur, "Unexpected symbol: " + cur.text.charAt(cur.index));
            return new ErrorToken(String.valueOf(cur.text.charAt(cur.index)), cur, cur.next());
        }
    }
}

class Compiler {
    private final ArrayList<Message> messages;

    public Compiler() {
        messages = new ArrayList<>();
    }

    public void addMessage(boolean isErr, Position c, String text) {
        messages.add(new Message(isErr, text, c));
    }

    public void outputMessages() {
        for (Message m : messages) {
            System.out.print(m.isError ? "Error" : "Warning");
            System.out.print(" " + m.coord + ": ");
            System.out.println(m.text);
        }
    }

    public Scanner getScanner(String program) {
        return new Scanner(program, this);
    }
}

enum DomainTag {
    LEFT_BRACKET("("),
    RIGHT_BRACKET(")"),
    DASH("-"),
    QUESTION_SIGN("?"),
    INVERSION("~"),
    CONJUNCTION("&"),
    DISJUNCTION("|"),
    IDENTITY("<->"),
    IMPLICATION("->"),
    VARIABLE("var"),
    ERROR("ERROR"),
    END_OF_PROGRAM("$");

    final String text;

    DomainTag(String text) {
        this.text = text;
    }
}

class Position {
    String text;
    int line, pos, index;

    int getLine() {
        return line;
    }

    int getPos() {
        return pos;
    }

    int getIndex() {
        return index;
    }

    String getText() {
        return text;
    }

    Position(String text) {
        this.text = text;
        line = pos = 1;
        index = 0;
    }

    Position(Position p) {
        this.text = p.getText();
        this.line = p.getLine();
        this.pos = p.getPos();
        this.index = p.getIndex();
    }

    @Override
    public String toString() {
        return "(" + line + "," + pos + ")";
    }

    boolean isEOF() {
        return index == text.length();
    }

    int getCode() {
        return isEOF() ? -1 : text.codePointAt(index);
    }

    boolean isWhitespace() {
        return !isEOF() && Character.isWhitespace(getCode());
    }

    boolean isLetter() {
        return !isEOF() && String.valueOf(text.charAt(index)).matches("[a-zA-Z]");
    }

    boolean isDigit() {
        return !isEOF() && String.valueOf(text.charAt(index)).matches("[0-9]");
    }

    boolean isLeftCornerBracket() {
        return !isEOF() && text.charAt(index) == '<';
    }

    boolean isDash() {
        return !isEOF() && text.charAt(index) == '-';
    }

    boolean isRightCornerBracket() {
        return !isEOF() && text.charAt(index) == '>';
    }

    boolean isNewLine() {
        if (isEOF()) {
            return true;
        }

        if (text.charAt(index) == '\r' && index + 1 < text.length()) {
            return (text.charAt(index + 1) == '\n');
        }

        return (text.charAt(index) == '\n');
    }

    Position next() {
        Position p = new Position(this);
        if (!p.isEOF()) {
            if (p.isNewLine()) {
                if (p.text.charAt(p.index) == '\r')
                    p.index++;
                p.line++;
                p.pos = 1;
            } else {
                if (Character.isHighSurrogate(p.text.charAt(p.index)))
                    p.index++;
                p.pos++;
            }
            p.index++;
        }
        return p;
    }
}

class Fragment {
    Position starting;
    Position following;

    Fragment(Position starting, Position following) {
        this.starting = starting;
        this.following = following;
    }

    public String toString() {
        return starting.toString() + "-" + following.toString();
    }
}

class Message {
    boolean isError;
    String text;
    Position coord;

    Message(boolean isError, String text, Position coord) {
        this.isError = isError;
        this.text = text;
        this.coord = coord;
    }
}

abstract class Token implements Cloneable {
    DomainTag tag;
    Fragment coords;
    String attr;

    Token(String attr, DomainTag tag, Position starting, Position following) {
        this.attr = attr;
        this.tag = tag;
        this.coords = new Fragment(starting, following);
    }

    Token(Token token) {
        this.tag = token.tag;
        this.coords = new Fragment(token.coords.starting, token.coords.following);
        this.attr = token.attr;
    }

    abstract protected Token clone() throws CloneNotSupportedException;

    @Override
    public String toString() {
        return coords.toString() + ": " + attr;
    }
}

class LeftBracketToken extends Token implements Cloneable {
    LeftBracketToken(Position starting, Position following) {
        super("(", DomainTag.LEFT_BRACKET, starting, following);
    }

    LeftBracketToken(LeftBracketToken token) {
        super(token);
    }

    @Override
    protected Token clone() throws CloneNotSupportedException {
        return new LeftBracketToken(this);
    }

    @Override
    public String toString() {
        return "LEFT_BRACKET " + super.toString();
    }
}

class RightBracketToken extends Token implements Cloneable {
    RightBracketToken(Position starting, Position following) {
        super(")", DomainTag.RIGHT_BRACKET, starting, following);
    }

    RightBracketToken(RightBracketToken token) {
        super(token);
    }

    @Override
    protected Token clone() throws CloneNotSupportedException {
        return new RightBracketToken(this);
    }

    @Override
    public String toString() {
        return "RIGHT_BRACKET " + super.toString();
    }
}

class DashToken extends Token implements Cloneable {
    DashToken(Position starting, Position following) {
        super("-", DomainTag.DASH, starting, following);
    }

    DashToken(DashToken token) {
        super(token);
    }

    @Override
    protected Token clone() throws CloneNotSupportedException {
        return new DashToken(this);
    }

    @Override
    public String toString() {
        return "DASH " + super.toString();
    }
}

class QuestionSignToken extends Token implements Cloneable {
    QuestionSignToken(Position starting, Position following) {
        super("?", DomainTag.QUESTION_SIGN, starting, following);
    }

    QuestionSignToken(QuestionSignToken token) {
        super(token);
    }

    @Override
    protected Token clone() throws CloneNotSupportedException {
        return new QuestionSignToken(this);
    }

    @Override
    public String toString() {
        return " QUESTION_SIGN " + super.toString();
    }
}

class InversionToken extends Token implements Cloneable {
    InversionToken(Position starting, Position following) {
        super("~", DomainTag.INVERSION, starting, following);
    }

    InversionToken(InversionToken token) {
        super(token);
    }

    @Override
    protected Token clone() throws CloneNotSupportedException {
        return new InversionToken(this);
    }

    @Override
    public String toString() {
        return "RIGHT_BRACKET " + super.toString();
    }
}

class ConjunctionToken extends Token implements Cloneable {
    ConjunctionToken(Position starting, Position following) {
        super("&", DomainTag.CONJUNCTION, starting, following);
    }

    ConjunctionToken(ConjunctionToken token) {
        super(token);
    }

    @Override
    protected Token clone() throws CloneNotSupportedException {
        return new ConjunctionToken(this);
    }

    @Override
    public String toString() {
        return "CONJUNCTION_SIGN " + super.toString();
    }
}

class DisjunctionToken extends Token implements Cloneable {
    DisjunctionToken(Position starting, Position following) {
        super("|", DomainTag.DISJUNCTION, starting, following);
    }

    DisjunctionToken(DisjunctionToken token) {
        super(token);
    }

    @Override
    protected Token clone() throws CloneNotSupportedException {
        return new DisjunctionToken(this);
    }

    @Override
    public String toString() {
        return "DISJUNCTION_SIGN " + super.toString();
    }
}

class IdentityToken extends Token implements Cloneable {
    IdentityToken(Position starting, Position following) {
        super("<->", DomainTag.IDENTITY, starting, following);
    }

    IdentityToken(IdentityToken token) {
        super(token);
    }

    @Override
    protected Token clone() throws CloneNotSupportedException {
        return new IdentityToken(this);
    }

    @Override
    public String toString() {
        return "IDENTITY_TOKEN " + super.toString();
    }
}

class ImplicationToken extends Token implements Cloneable {
    ImplicationToken(Position starting, Position following) {
        super("->", DomainTag.IMPLICATION, starting, following);
    }

    ImplicationToken(ImplicationToken token) {
        super(token);
    }

    @Override
    protected Token clone() throws CloneNotSupportedException {
        return new ImplicationToken(this);
    }

    @Override
    public String toString() {
        return "IMPLICATION_TOKEN " + super.toString();
    }
}

class VariableToken extends Token implements Cloneable {
    VariableToken(String attr, Position starting, Position following) {
        super(attr, DomainTag.VARIABLE, starting, following);
    }

    VariableToken(VariableToken token) {
        super(token);
    }

    @Override
    protected Token clone() throws CloneNotSupportedException {
        return new VariableToken(this);
    }

    @Override
    public String toString() {
        return "VARIABLE " + super.toString();
    }
}

class ErrorToken extends Token implements Cloneable {
    ErrorToken(String attr, Position starting, Position following) {
        super(attr, DomainTag.ERROR, starting, following);
    }

    ErrorToken(ErrorToken token) {
        super(token);
    }

    @Override
    protected Token clone() throws CloneNotSupportedException {
        return new ErrorToken(this);
    }

    @Override
    public String toString() {
        return attr;
    }
}

class EndOfProgramToken extends Token implements Cloneable {
    EndOfProgramToken(String attr, DomainTag tag, Position starting, Position following) {
        super(attr, tag, starting, following);
        assert (tag == DomainTag.END_OF_PROGRAM);
    }

    EndOfProgramToken(EndOfProgramToken token) {
        super(token);
    }

    @Override
    protected Token clone() throws CloneNotSupportedException {
        return new EndOfProgramToken(this);
    }

    @Override
    public String toString() {
        return "END_OF_PROGRAM " + super.toString();
    }
}