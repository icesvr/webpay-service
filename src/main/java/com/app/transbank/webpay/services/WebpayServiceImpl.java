package com.app.transbank.webpay.services;

import java.io.File;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


import com.app.transbank.config.GenericResponse;
import com.app.transbank.util.Util;
import com.app.transbank.webpay.model.Billing;
import com.app.transbank.webpay.model.Compra;
import com.app.transbank.webpay.model.TransactionResult;
import com.app.transbank.webpay.repository.BillingRepository;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.*;
import com.transbank.webpay.wswebpay.service.TransactionResultOutput;
import com.transbank.webpay.wswebpay.service.WsInitTransactionOutput;
import com.transbank.webpay.wswebpay.service.WsTransactionDetailOutput;

import cl.transbank.onepay.exception.TransactionCreateException;
import cl.transbank.webpay.Webpay;
import cl.transbank.webpay.WebpayNormal;
import cl.transbank.webpay.configuration.Configuration;



@Service
public class WebpayServiceImpl implements WebpayService {

	Util util = new Util();
	
	@Autowired
	private BillingRepository billingRepository;
	
	
	public ResponseEntity<GenericResponse> createRequest(Compra compra) throws Exception {
		
		GenericResponse genericResponse = new GenericResponse();
		HttpStatus status = null;
		
		String buyOrder = compra.getBuyOrder();
		String sessionId = compra.getSessionId();
        double amount = compra.getAmount();
        String finalUrl = compra.getUrlRedirect();
        String billingId = compra.getBillingId();
		
        Map<String, Object> details = new HashMap<>();
        details.put("buyOrder", buyOrder);
        details.put("sessionId", sessionId);
        details.put("amount", amount);
        details.put("billingId", billingId);
      
		
		try {
			
			WebpayNormal transaction = new Webpay(Configuration.forTestingWebpayPlusNormal()).getNormalTransaction();
			
			
			WsInitTransactionOutput initResult = transaction.initTransaction(amount, sessionId, buyOrder,"http://n3gro.com:8081/webpay-result", finalUrl);	
			String formAction = initResult.getUrl();
			String tokenWs = initResult.getToken();
					
			details.put("url", formAction);
			details.put("token", tokenWs);
			Billing billing = new Billing();
			
			if(!formAction.isEmpty()) {
				billing.setIdBilling(billingId);
				billing.setToken(tokenWs);
				billingRepository.save(billing);
				genericResponse.setCode(200);
				genericResponse.setMsg("Transaccion exitosa!");
				genericResponse.setResponse(details);
				status = HttpStatus.OK;
				
			}else {
				genericResponse.setCode(500);
				genericResponse.setMsg("Transaccion exitosa!");
				status = HttpStatus.NO_CONTENT;
			}
						
			
		} catch (TransactionCreateException e ) {
			e.printStackTrace();
		}
		
		return new ResponseEntity<>(genericResponse,status);
	}

	
	public void validateTransaction(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		String token = httpRequest.getParameter("token_ws");
		
		try {
			
			WebpayNormal transaction = new Webpay(Configuration.forTestingWebpayPlusNormal()).getNormalTransaction();
			TransactionResultOutput result = transaction.getTransactionResult(token);	
			WsTransactionDetailOutput output = result.getDetailOutput().get(0);
			TransactionResult transactionResult = new TransactionResult();
						
			if(output.getResponseCode() == 0) {
				
				transactionResult.setBuyOrder(result.getBuyOrder());
				transactionResult.setSessionId(result.getSessionId());
				transactionResult.setCardNumber(result.getCardDetail().getCardNumber());
				transactionResult.setCardExpirationDate(result.getCardDetail().getCardExpirationDate());
				transactionResult.setAccoutingDate(result.getAccountingDate());
				transactionResult.setTransactionDate(result.getTransactionDate());
				transactionResult.setVci(result.getVCI());
				transactionResult.setUrlRedirect(result.getUrlRedirection());

				
				httpResponse.setHeader("Content-Type", "application/x-www-form-urlencoded");
				httpResponse.setStatus(307);
				httpResponse.setHeader("Location", result.getUrlRedirection());

			}else {
				System.out.println("Transaccion anulada");
				httpResponse.setHeader("Content-Type", "application/x-www-form-urlencoded");	 
				httpResponse.setStatus(307);
				httpResponse.setHeader("Location", "http://localhost:8081/get-token");
		
			}
				
		}catch(Exception ex) {
				
			System.out.println("Error: "+ex.getLocalizedMessage());
			ex.printStackTrace();
		}

	}
	
	public ResponseEntity<GenericResponse> getQr(String token){
		GenericResponse genericResponse = new GenericResponse();
		HttpStatus status = null;
		try {
			JsonObject json = new Gson().fromJson(token, JsonObject.class);
			String finalToken = json.get("token").getAsString();			
			Billing billing = billingRepository.findBillingByToken(finalToken);
			
			File file = QRCode.from(billing.getIdBilling()).to(ImageType.PNG).withSize(300, 300).file();
			String base64 = Util.encoder(file.getAbsolutePath());
		
			if(file.exists()) {
				file.delete();
			}
		
			if(base64 != "") {
				status = HttpStatus.CREATED;
				genericResponse.setCode(201);
				genericResponse.setMsg("Codigo QR creado");
				genericResponse.setResponse(base64);
			}else {
				status = HttpStatus.NO_CONTENT;
				genericResponse.setCode(204);
				genericResponse.setMsg("Error al generar codigo QR");
				genericResponse.setResponse(null);
			}
	
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		
		
		return new ResponseEntity<>(genericResponse, status );
	}

}
