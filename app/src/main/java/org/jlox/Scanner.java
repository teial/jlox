package org.jlox;

import static org.jlox.TokenType.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

class Scanner {

    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private static final Map<String, TokenType> keywords = Map.ofEntries(
        Map.entry("and", AND),
        Map.entry("class", CLASS),
        Map.entry("else", ELSE),
        Map.entry("false", FALSE),
        Map.entry("for", FOR),
        Map.entry("fun", FUN),
        Map.entry("if", IF),
        Map.entry("nil", NIL),
        Map.entry("or", OR),
        Map.entry("print", PRINT),
        Map.entry("return", RETURN),
        Map.entry("super", SUPER),
        Map.entry("this", THIS),
        Map.entry("true", TRUE),
        Map.entry("var", VAR),
        Map.entry("while", WHILE)
    );

    private int start = 0;
    private int current = 0;
    private int line = 1;

    public Scanner(String source) {
        this.source = source;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            Token token = scanToken();
            if (token != null) tokens.add(token);
        }
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private Token scanToken() {
        char c = advance();
        return switch (c) {
            case '(' -> makeToken(LEFT_PAREN);
            case ')' -> makeToken(RIGHT_PAREN);
            case '{' -> makeToken(LEFT_BRACE);
            case '}' -> makeToken(RIGHT_BRACE);
            case ',' -> makeToken(COMMA);
            case '.' -> makeToken(DOT);
            case '-' -> makeToken(MINUS);
            case '+' -> makeToken(PLUS);
            case ';' -> makeToken(SEMICOLON);
            case '*' -> makeToken(STAR);
            case '!' -> makeToken(match('=') ? BANG_EQUAL : BANG);
            case '=' -> makeToken(match('=') ? EQUAL_EQUAL : EQUAL);
            case '<' -> makeToken(match('=') ? LESS_EQUAL : LESS);
            case '>' -> makeToken(match('=') ? GREATER_EQUAL : GREATER);
            case '"' -> string();
            case '/' -> match('/') ? comment() : makeToken(SLASH);
            case ' ', '\r', '\t' -> null;
            case '\n' -> {
                line++;
                yield null;
            }
            default -> {
                if (Character.isDigit(c)) yield number();
                else if (Character.isLetter(c)) yield identifier();
                else {
                    ErrorHandler.error(line, "Unexpected character.");
                    yield null;
                }
            }
        };
    }

    private char advance() {
        return source.charAt(current++);
    }

    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    private char peek() {
        return isAtEnd() ? '\0' : source.charAt(current);
    }

    private char peekNext() {
        return current + 1 >= source.length() ? '\0' : source.charAt(current + 1);
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private @Nonnull Token makeToken(TokenType type) {
        return makeToken(type, null);
    }

    private @Nonnull Token makeToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        return new Token(type, text, literal, line);
    }

    private Token comment() {
        while (peek() != '\n' && !isAtEnd()) advance();
        return null;
    }

    private Token string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }
        if (isAtEnd()) {
            ErrorHandler.error(line, "Unterminated string.");
            return null;
        }
        advance();
        String value = source.substring(start + 1, current - 1);
        return makeToken(STRING, value);
    }

    private Token number() {
        integer();
        if (peek() == '.' && Character.isDigit(peekNext())) {
            advance();
            integer();
        }
        double value = Double.parseDouble(source.substring(start, current));
        return makeToken(NUMBER, value);
    }

    private void integer() {
        while (Character.isDigit(peek())) advance();
    }

    private Token identifier() {
        while (isAlphaNumeric(peek())) advance();
        String text = source.substring(start, current);
        return makeToken(keywords.getOrDefault(text, IDENTIFIER));
    }

    private boolean isAlphaNumeric(char c) {
        return Character.isLetter(c) || Character.isDigit(c) || c == '_';
    }
}
