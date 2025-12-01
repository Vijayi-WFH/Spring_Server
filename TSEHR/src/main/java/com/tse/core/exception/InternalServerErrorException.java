package com.tse.core.exception;

    public class InternalServerErrorException  extends RuntimeException {

        public InternalServerErrorException() {
            super("Internal Server Error");
        }
    }

