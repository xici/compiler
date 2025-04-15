package group10;

/**
 * 定义Token的类型，覆盖标识符、关键字、数字、操作符、分隔符、错误以及文件结束符等
 */
public enum TokenType {
    IDENTIFIER,
    KEYWORD,
    NUMBER,
    OPERATOR,
    DELIMITER,
    STRING,
    PREPROCESSOR,
    ERROR,
    HEADER_FILE,
    MACRO_NAME,
    EOF
}
