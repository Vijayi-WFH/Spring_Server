package com.tse.core_application.utils;

import com.tse.core_application.exception.DuplicateFileException;
import com.tse.core_application.exception.FileNameException;
import com.tse.core_application.exception.FileNotFoundException;
import com.tse.core_application.exception.InternalServerErrorException;

import java.io.IOException;

public class ExceptionUtil {

    private void onUncheckedException(RuntimeException runtimeException) {
        if (runtimeException instanceof FileNameException) {
            throw runtimeException;
        } else if (runtimeException instanceof DuplicateFileException) {
            throw runtimeException;
        } else {
            if (runtimeException instanceof FileNotFoundException) {
                throw runtimeException;
            } else {
                throw new InternalServerErrorException(runtimeException.getMessage());
            }
        }
    }

    private void onCheckedException(Exception exception) {
        if (exception instanceof IOException) {
            throw new InternalServerErrorException(exception.getMessage());
        }
    }

    public void onException(Exception exception) {
        if (exception instanceof RuntimeException) {
            onUncheckedException((RuntimeException) exception);
        } else {
            onCheckedException(exception);
        }
    }
}
