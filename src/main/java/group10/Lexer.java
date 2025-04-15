package group10;

import java.io.IOException;
import java.util.Set;

/**
 * Lexer (词法分析器) 类负责将 C 语言源代码分解为一系列的 Token。
 * 逐个读取字符，识别出标识符、关键字、数字、操作符、分隔符、字符串、预处理指令等词法单元。
 */
public class Lexer {
    /**
     * 输入处理器，用于读取源文件字符
     */
    private final InputHandler input;
    /**
     * 当前正在处理的字符 (以 int 形式表示，-1 表示 EOF)
     */
    private int currentChar;
    /**
     * 当前行号
     */
    private int line;
    /**
     * 当前列号
     */
    private int column;

    /**
     * Lexer 构造函数。
     * 初始化输入处理器，设置初始行号和列号，并读取第一个字符。
     * 
     * @param filename 要分析的源文件名。
     * @throws IOException 读取文件时发生 I/O 错误。
     */
    public Lexer(String filename) throws IOException {
        input = new InputHandler(filename);
        line = 1;
        column = 0;
        currentChar = input.getNextChar();
    }

    /**
     * 对整个源文件进行词法分析，并将结果格式化为字符串。
     * 循环读取字符，根据当前字符的类型调用相应的处理方法来生成 Token，
     * 直到文件结束 (EOF)。
     *
     * @return 包含所有 Token 信息的格式化字符串。
     */
    public String analyzeToString() {
        StringBuilder sb = new StringBuilder();
        while (currentChar != -1) {
            if (currentChar == '#' && column == 0) {
                Token directive = readPreprocessorDirective();
                appendTokenToOutput(sb, directive);
                String directiveLexeme = directive.getLexeme();
                switch (directiveLexeme) {
                    case "#include":
                        appendTokenToOutput(sb, readHEADER_FILE());
                        break;
                    case "#define":
                    case "#undef":
                        appendTokenToOutput(sb, readMacroDefine());
                        break;
                    case "#ifdef":
                    case "#ifndef":
                        appendTokenToOutput(sb, readMacroCondition());
                        break;
                    case "#endif":
                        break;
                }
                continue;
            }
            if (currentChar == '/') {
                advance();
                if (currentChar == '/') {
                    skipSingleLineComment();
                } else if (currentChar == '*') {
                    skipMultiLineComment();
                } else {
                    appendTokenToOutput(sb, new Token(TokenType.OPERATOR, "/", line, column - 1));
                }
                continue;
            }
            if (currentChar == '"') {
                appendTokenToOutput(sb, readStringLiteral());
                continue;
            }
            if (currentChar == '\'') {
                appendTokenToOutput(sb, readCharLiteral());
                continue;
            }
            if (Character.isWhitespace(currentChar)) {
                skipWhitespace();
                continue;
            }
            if (Character.isLetter(currentChar) || currentChar == '_') {
                appendTokenToOutput(sb, readIdentifier());
                continue;
            }
            if (Character.isDigit(currentChar)) {
                appendTokenToOutput(sb, readNumber());
                continue;
            }
            if (isOperatorOrDelimiter((char) currentChar)) {
                appendTokenToOutput(sb, readOperatorOrDelimiter());
                continue;
            }
            ErrorHandler.reportError("非法字符: " + (char) currentChar, line, column);
            appendTokenToOutput(sb, new Token(TokenType.ERROR, String.valueOf((char) currentChar), line, column));
            advance();
        }
        appendTokenToOutput(sb, new Token(TokenType.EOF, "EOF", line, column));
        try {
            input.close();
        } catch (IOException e) {
            sb.append("关闭文件时出错。\n");
        }
        return sb.toString();
    }

    /**
     * 将 Token 信息追加到输出 StringBuilder 中。
     * 格式：TokenType Lexeme @Line:Column
     *
     * @param sb    用于构建输出的 StringBuilder。
     * @param token 要追加的 Token。
     */
    private void appendTokenToOutput(StringBuilder sb, Token token) {
        if (token == null) {
            ErrorHandler.reportError("尝试添加 null Token", line, column);
            return;
        }
        sb.append(token.getType()).append("\t")
                .append(token.getLexeme()).append("\t")
                .append(String.format("@%d:%d\n", token.getLine(), token.getColumn()));
    }

    /**
     * 读取输入流中的下一个字符，并更新当前行号和列号。
     * 如果遇到换行符 '\n'，行号增加，列号重置为 0。
     * 否则，列号增加。
     * 如果读取时发生 IOException 或到达文件末尾，将 currentChar 设置为 -1。
     */
    private void advance() {
        try {
            if (currentChar == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }
            currentChar = input.getNextChar();
        } catch (IOException e) {
            ErrorHandler.reportError("读取文件时出错: " + e.getMessage(), line, column);
            currentChar = -1;
        }
    }

    /**
     * 跳过连续的空白字符（空格、制表符、换行符等）。
     * 循环调用 advance() 直到遇到非空白字符或文件结束。
     */
    private void skipWhitespace() {
        while (currentChar != -1 && Character.isWhitespace(currentChar)) {
            advance();
        }
    }

    /**
     * 读取一个标识符或关键字。
     * 标识符由字母、数字或下划线组成，且不能以数字开头。
     * 读取完成后，检查是否为预定义的关键字。
     *
     * @return 识别出的 IDENTIFIER 或 KEYWORD 类型的 Token。
     */
    private Token readIdentifier() {
        StringBuilder sb = new StringBuilder();
        int tokenLine = line;
        int tokenColumn = column;
        while (currentChar != -1 && (Character.isLetterOrDigit(currentChar) || currentChar == '_')) {
            sb.append((char) currentChar);
            advance();
        }
        String lexeme = sb.toString();
        final Set<String> keywords = Set.of(
                "auto", "break", "case", "char", "const", "continue", "default", "do",
                "double", "else", "enum", "extern", "float", "for", "goto", "if",
                "int", "long", "register", "return", "short", "signed", "sizeof", "static",
                "struct", "switch", "typedef", "union", "unsigned", "void", "volatile", "while");
        if (keywords.contains(lexeme)) {
            return new Token(TokenType.KEYWORD, lexeme, tokenLine, tokenColumn);
        } else {
            return new Token(TokenType.IDENTIFIER, lexeme, tokenLine, tokenColumn);
        }
    }

    /**
     * 读取一个数字字面量。
     * 支持整数和简单的浮点数（包含一个小数点）。
     * 不支持科学计数法 (如 1e10) 或十六进制/八进制表示。
     *
     * @return 识别出的 NUMBER 类型的 Token。
     */
    private Token readNumber() {
        StringBuilder sb = new StringBuilder();
        int tokenLine = line;
        int tokenColumn = column;
        while (currentChar != -1 && Character.isDigit(currentChar)) {
            sb.append((char) currentChar);
            advance();
        }
        if (currentChar == '.') {
            sb.append('.');
            advance();
            if (currentChar != -1 && Character.isDigit(currentChar)) {
                while (currentChar != -1 && Character.isDigit(currentChar)) {
                    sb.append((char) currentChar);
                    advance();
                }
            }
        }
        return new Token(TokenType.NUMBER, sb.toString(), tokenLine, tokenColumn);
    }

    /**
     * 读取一个操作符或分隔符。
     * 处理单字符的操作符和分隔符。
     * TODO: 需要扩展以支持多字符操作符 (如 ==, +=, ->, << 等)。
     *
     * @return 识别出的 OPERATOR 或 DELIMITER 类型的 Token，如果无法识别则返回 ERROR Token。
     */
    private Token readOperatorOrDelimiter() {
        int tokenLine = line;
        int tokenColumn = column;
        char ch = (char) currentChar;
        advance();
        String delimiters = "();,{}[]'.?:";
        String operators = "+-*/=<>!%^~&|";
        if (delimiters.indexOf(ch) != -1) {
            return new Token(TokenType.DELIMITER, String.valueOf(ch), tokenLine, tokenColumn);
        } else if (operators.indexOf(ch) != -1) {
            return new Token(TokenType.OPERATOR, String.valueOf(ch), tokenLine, tokenColumn);
        } else if (ch == '.') {
            return new Token(TokenType.DELIMITER, String.valueOf(ch), tokenLine, tokenColumn);
        } else {
            ErrorHandler.reportError("未知符号: " + ch, tokenLine, tokenColumn);
            return new Token(TokenType.ERROR, String.valueOf(ch), tokenLine, tokenColumn);
        }
    }

    /**
     * 读取预处理指令（如 #include, #define）。
     * 从 '#' 开始，读取指令名称。
     *
     * @return PREPROCESSOR 类型的 Token。
     */
    private Token readPreprocessorDirective() {
        int tokenLine = line;
        int tokenColumn = column;
        StringBuilder sb = new StringBuilder();
        sb.append((char) currentChar);
        advance();
        skipWhitespace();
        while (currentChar != -1 && Character.isLetter(currentChar)) {
            sb.append((char) currentChar);
            advance();
        }
        return new Token(TokenType.PREPROCESSOR, sb.toString(), tokenLine, tokenColumn);
    }

    /**
     * 读取 #include 指令后的头文件名。
     * 支持尖括号 <...> 和双引号 "..." 两种形式。
     *
     * @return 识别出的 HEADER_FILE 类型的 Token。
     */
    private Token readHEADER_FILE() {
        int tokenLine = line;
        int tokenColumn = column;
        StringBuilder sb = new StringBuilder();
        skipWhitespace();
        char startChar = (char) currentChar;
        boolean isAngleBracket = startChar == '<';
        boolean isDoubleQuote = startChar == '"';
        if (!isAngleBracket && !isDoubleQuote) {
            ErrorHandler.reportError("无效的 #include 格式，缺少 '<' 或 '\"'", line, column);
            while (currentChar != -1 && currentChar != '\n') {
                sb.append((char) currentChar);
                advance();
            }
            return new Token(TokenType.ERROR, sb.toString(), tokenLine, tokenColumn);
        }
        sb.append(startChar);
        advance();
        char endChar = isAngleBracket ? '>' : '"';
        while (currentChar != -1 && currentChar != '\n' && currentChar != endChar) {
            sb.append((char) currentChar);
            advance();
        }
        if (currentChar == endChar) {
            sb.append((char) currentChar);
            advance();
        } else {
            ErrorHandler.reportError("未闭合的头文件名 " + (isAngleBracket ? "<...>" : "\"...\""), tokenLine, tokenColumn);
            return new Token(TokenType.ERROR, sb.toString(), tokenLine, tokenColumn);
        }
        return new Token(TokenType.HEADER_FILE, sb.toString(), tokenLine, tokenColumn);
    }

    /**
     * 读取 #define 或 #undef 指令后的宏名称。
     * 读取指令后的第一个非空白单词作为宏名。
     * TODO: 需要处理带参数的宏定义。
     *
     * @return 识别出的 MACRO_NAME 类型的 Token。
     */
    private Token readMacroDefine() {
        int tokenLine = line;
        int tokenColumn = column;
        StringBuilder sb = new StringBuilder();
        skipWhitespace();
        if (currentChar != -1 && (Character.isLetter(currentChar) || currentChar == '_')) {
            while (currentChar != -1 && (Character.isLetterOrDigit(currentChar) || currentChar == '_')) {
                sb.append((char) currentChar);
                advance();
            }
        } else if (currentChar != -1 && !Character.isWhitespace(currentChar)) {
            ErrorHandler.reportError("无效的宏名称起始字符: " + (char) currentChar, line, column);
            while (currentChar != -1 && !Character.isWhitespace(currentChar)) {
                sb.append((char) currentChar);
                advance();
            }
            return new Token(TokenType.ERROR, sb.toString(), tokenLine, tokenColumn);
        } else {
            ErrorHandler.reportError("缺少宏名称", line, column);
            return new Token(TokenType.ERROR, "", tokenLine, tokenColumn);
        }
        return new Token(TokenType.MACRO_NAME, sb.toString(), tokenLine, tokenColumn);
    }

    /**
     * 读取 #ifdef 或 #ifndef 指令后的宏名称。
     * 其处理方式与读取 #define 后的宏名相同。
     *
     * @return 识别出的 MACRO_NAME 类型的 Token。
     */
    private Token readMacroCondition() {
        return readMacroDefine();
    }

    /**
     * 读取字符串字面量，以双引号 " 开始和结束。
     * 处理转义字符 '\'。
     *
     * @return 识别出的 STRING 类型的 Token，如果字符串未闭合则返回 ERROR Token。
     */
    private Token readStringLiteral() {
        int tokenLine = line;
        int tokenColumn = column;
        StringBuilder sb = new StringBuilder();
        advance();
        while (currentChar != -1 && (char) currentChar != '"') {
            if (currentChar == '\n') {
                ErrorHandler.reportError("字符串未闭合，已到行尾", tokenLine, tokenColumn);
                skipToNextLine();
                return new Token(TokenType.ERROR, sb.toString(), tokenLine, tokenColumn);
            }
            isEscapeCharacter(sb);
        }
        if (currentChar == -1) {
            ErrorHandler.reportError("未闭合的字符串字面量", tokenLine, tokenColumn);
            return new Token(TokenType.ERROR, sb.toString(), tokenLine, tokenColumn);
        }
        advance();
        return new Token(TokenType.STRING, sb.toString(), tokenLine, tokenColumn);
    }

    private void skipToNextLine() {
        while (currentChar != -1 && currentChar != '\n') {
            advance();
        }
        if (currentChar == '\n') {
            advance();
        }
    }

    /**
     * 读取字符字面量，以单引号 ' 开始和结束。
     * 处理转义字符 '\'。
     * C 语言中字符字面量通常只包含一个字符（或一个转义序列）。
     * TODO: 添加对字符字面量长度的校验。
     *
     * @return 识别出的 STRING (或 CHAR) 类型的 Token，如果字符未闭合则返回 ERROR Token。
     *         注意：当前 TokenType 没有 CHAR，暂时用 STRING 代替。
     */
    private Token readCharLiteral() {
        int tokenLine = line;
        int tokenColumn = column;
        StringBuilder sb = new StringBuilder();
        advance();
        while (currentChar != -1 && (char) currentChar != '\'') {
            if (currentChar == '\n') {
                ErrorHandler.reportError("字符字面量未闭合，已到行尾", tokenLine, tokenColumn);
                skipToNextLine();
                return new Token(TokenType.ERROR, sb.toString(), tokenLine, tokenColumn);
            }
            isEscapeCharacter(sb);
        }
        if (currentChar == -1) {
            ErrorHandler.reportError("未闭合的字符字面量", tokenLine, tokenColumn);
            return new Token(TokenType.ERROR, sb.toString(), tokenLine, tokenColumn);
        }
        advance();
        return new Token(TokenType.STRING, sb.toString(), tokenLine, tokenColumn);
    }

    /**
     * 处理字符串或字符字面量中的字符，特别是转义字符 '\'。
     * 如果当前字符是 '\'，则将其和下一个字符一起添加到 StringBuilder 中。
     * 否则，只添加当前字符。
     * 该方法会负责调用 advance() 来移动到下一个字符。
     *
     * @param sb 用于构建字面量内容的 StringBuilder。
     */
    private void isEscapeCharacter(StringBuilder sb) {
        if (currentChar == '\\') {
            sb.append((char) currentChar);
            advance();
            if (currentChar != -1) {
                sb.append((char) currentChar);
                advance();
            }
        } else {
            sb.append((char) currentChar);
            advance();
        }
    }

    /**
     * 检查给定字符是否是 C 语言中的（单字符）操作符或分隔符。
     * 
     * @param ch 要检查的字符。
     * @return 如果是操作符或分隔符，则返回 true；否则返回 false。
     */
    private boolean isOperatorOrDelimiter(char ch) {
        return "+-*/=<>!;(),{}[].'?:&|".indexOf(ch) != -1;
    }

    /**
     * 跳过单行注释（从 "
     * 循环调用 advance() 直到遇到换行符 '\n' 或文件结束。
     */
    private void skipSingleLineComment() {
        while (currentChar != -1 && currentChar != '\n') {
            advance();
        }
    }

    /**
     * 跳过多行注释。
     * 处理嵌套注释（虽然 C 语言标准不支持嵌套，但某些编译器可能允许）。
     * 注意：当前实现不处理嵌套注释，遇到第一个就结束。
     */
    private void skipMultiLineComment() {
        advance();
        while (currentChar != -1) {
            if (currentChar == '*') {
                advance();
                if (currentChar == '/') {
                    advance();
                    break;
                }
            } else {
                advance();
            }
        }
        if (currentChar == -1) {
            ErrorHandler.reportError("未闭合的多行注释", line, column);
        }
    }
}
