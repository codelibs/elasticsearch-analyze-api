package org.codelibs.elasticsearch.analyze.exception;

public class AnalyzeApiRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AnalyzeApiRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public AnalyzeApiRequestException(String message) {
        super(message);
    }

}
