package com.app.transbank.webpay.controllers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


import com.app.transbank.config.GenericResponse;
import com.app.transbank.webpay.model.Compra;
import com.app.transbank.webpay.services.WebpayServiceImpl;



@Controller
public class WebpayController{
	
	
	@Autowired
	private WebpayServiceImpl webpayService;
	
	
	@CrossOrigin
	@PostMapping("/get-token")
	public ResponseEntity<GenericResponse> webPayRequest(@RequestBody Compra compra ) throws Exception {
		return webpayService.createRequest(compra);
	}
	
	@PostMapping("/webpay-result")
	public void webPayEnd(HttpServletRequest httpRequest, HttpServletResponse httpResponse){
		
		webpayService.validateTransaction(httpRequest, httpResponse);
	
	}
	
	@PostMapping("/get-qr")
	public ResponseEntity<GenericResponse> getQr(@RequestBody String token){
		
		return webpayService.getQr(token);
	}
	
	
	
	
}
