package com.app.transbank.webpay.services;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.ResponseEntity;

import com.app.transbank.config.GenericResponse;
import com.app.transbank.webpay.model.Compra;

public interface WebpayService {
	
	ResponseEntity<GenericResponse> createRequest(Compra compra) throws Exception;
	
	void validateTransaction(HttpServletRequest httpRequest, HttpServletResponse httpResponse);
	
	ResponseEntity<GenericResponse> getQr(String token);
	
}
