package com.mibombay.sistemaresurante.exceptions;

public class BusinessException extends RuntimeException {
    public BusinessException(String mensaje) {
        super(mensaje);
    }
}
