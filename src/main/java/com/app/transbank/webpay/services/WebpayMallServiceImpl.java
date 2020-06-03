package com.app.transbank.webpay.services;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

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
import com.transbank.webpay.wswebpay.service.WsTransactionDetail;
import com.transbank.webpay.wswebpay.service.WsTransactionDetailOutput;

import cl.transbank.onepay.exception.TransactionCreateException;
import cl.transbank.webpay.Webpay;
import cl.transbank.webpay.WebpayMallNormal;
import cl.transbank.webpay.WebpayNormal;
import cl.transbank.webpay.configuration.Configuration;

@Service
public class WebpayMallServiceImpl implements WebpayService {

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

		String txType = "", urlReturn = "", urlFinal = "";
		ArrayList storesTransaction = new ArrayList();

		try {
			Webpay webpay = new Webpay(Configuration.forTestingWebpayPlusMall());
			WsInitTransactionOutput resultInit = new WsInitTransactionOutput();

			// urlReturn = "https://cc42b0d4.ngrok.io";
			urlReturn = "http://localhost:8081/webpay-mall/webpay-result";
			urlFinal = "http://localhost:8081/webpay-mall/webpay-result";
			Random r = new Random();
			int menor = 150987676;
			int mayor = 356000000;
			int intervalo = mayor - menor;
			int Result = r.nextInt(intervalo) + menor;

			buyOrder = String.valueOf(Result);
			sessionId = "aj2h4kj2";
			ArrayList storeCodes = new ArrayList();
			storeCodes.add("597044444402");
			storeCodes.add("597044444403");

			// Para el ejemplo se usaran 2 comercios. Los que fueron definidos en archivo
			// cert-mall-normal.jsp
			// Commerce 1
			System.out.println(amount);

			BigDecimal amountStore = new BigDecimal(amount * 0.2);

			WsTransactionDetail storeTransaction = new WsTransactionDetail();
			storeTransaction.setAmount(amountStore);

			// Se genera un numero aleatorio para la orden de compra.
			r = new Random();
			menor = 150987676;
			mayor = 356000000;
			intervalo = mayor - menor;
			Result = r.nextInt(intervalo) + menor;

			buyOrder = String.valueOf(Result);

			storeTransaction.setBuyOrder(buyOrder);
			storeTransaction.setCommerceCode((String) storeCodes.get(0));

			storesTransaction.add(storeTransaction);

			// Commerce 2
			amountStore = new BigDecimal(amount * 0.8);

			storeTransaction = new WsTransactionDetail();
			storeTransaction.setAmount(amountStore);

			// Se genera un numero aleatorio para la orden de compra.
			r = new Random();
			menor = 150987676;
			mayor = 356000000;
			intervalo = mayor - menor;
			Result = r.nextInt(intervalo) + menor;

			buyOrder = String.valueOf(Result);
			storeTransaction.setBuyOrder(buyOrder);
			storeTransaction.setCommerceCode((String) storeCodes.get(1));

			storesTransaction.add(storeTransaction);

			resultInit = webpay.getMallNormalTransaction().initTransaction(buyOrder, sessionId, urlReturn, urlFinal,
					storesTransaction);

			String formAction = resultInit.getUrl();
			String tokenWs = resultInit.getToken();

			details.put("url", formAction);
			details.put("token", tokenWs);
			Billing billing = new Billing();

			if (!formAction.isEmpty()) {
				billing.setIdBilling(billingId);
				billing.setToken(tokenWs);
				billingRepository.save(billing);
				genericResponse.setCode(200);
				genericResponse.setMsg("Transaccion exitosa!");
				genericResponse.setResponse(details);
				status = HttpStatus.OK;

			} else {
				genericResponse.setCode(500);
				genericResponse.setMsg("Transaccion exitosa!");
				status = HttpStatus.NO_CONTENT;
			}

		} catch (TransactionCreateException e) {
			e.printStackTrace();
		}

		return new ResponseEntity<>(genericResponse, status);
	}

	public void validateTransaction(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		String token = httpRequest.getParameter("token_ws");
		String test = "";

		try {
			test = httpRequest.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println(token);

		Webpay webpay = new Webpay(Configuration.forTestingWebpayPlusMall());

		TransactionResultOutput resultGetResult = new TransactionResultOutput();

		try {
			resultGetResult = webpay.getNormalTransaction().getTransactionResult(token);
			
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(resultGetResult.getUrlRedirection());

		/*
		 * try {
		 * 
		 * WebpayNormal transaction = new
		 * Webpay(Configuration.forTestingWebpayPlusNormal()).getNormalTransaction();
		 * TransactionResultOutput result = transaction.getTransactionResult(token);
		 * WsTransactionDetailOutput output = result.getDetailOutput().get(0);
		 * TransactionResult transactionResult = new TransactionResult();
		 * 
		 * if(output.getResponseCode() == 0) {
		 * 
		 * transactionResult.setBuyOrder(result.getBuyOrder());
		 * transactionResult.setSessionId(result.getSessionId());
		 * transactionResult.setCardNumber(result.getCardDetail().getCardNumber());
		 * transactionResult.setCardExpirationDate(result.getCardDetail().
		 * getCardExpirationDate());
		 * transactionResult.setAccoutingDate(result.getAccountingDate());
		 * transactionResult.setTransactionDate(result.getTransactionDate());
		 * transactionResult.setVci(result.getVCI());
		 * transactionResult.setUrlRedirect(result.getUrlRedirection());
		 * 
		 * 
		 * httpResponse.setHeader("Content-Type", "application/x-www-form-urlencoded");
		 * httpResponse.setStatus(307); httpResponse.setHeader("Location",
		 * result.getUrlRedirection());
		 * 
		 * }else { System.out.println("Transaccion anulada");
		 * httpResponse.setHeader("Content-Type", "application/x-www-form-urlencoded");
		 * httpResponse.setStatus(307); httpResponse.setHeader("Location",
		 * "http://localhost:8081/get-token");
		 * 
		 * }
		 * 
		 * }catch(Exception ex) {
		 * 
		 * System.out.println("Error: "+ex.getLocalizedMessage()); ex.printStackTrace();
		 * }
		 */

	}

	public ResponseEntity<GenericResponse> getQr(String token) {
		GenericResponse genericResponse = new GenericResponse();
		HttpStatus status = null;
		try {
			JsonObject json = new Gson().fromJson(token, JsonObject.class);
			String finalToken = json.get("token").getAsString();
			Billing billing = billingRepository.findBillingByToken(finalToken);

			File file = QRCode.from(billing.getIdBilling()).to(ImageType.PNG).withSize(300, 300).file();
			String base64 = Util.encoder(file.getAbsolutePath());

			if (file.exists()) {
				file.delete();
			}

			if (base64 != "") {
				status = HttpStatus.CREATED;
				genericResponse.setCode(201);
				genericResponse.setMsg("Codigo QR creado");
				genericResponse.setResponse(base64);
			} else {
				status = HttpStatus.NO_CONTENT;
				genericResponse.setCode(204);
				genericResponse.setMsg("Error al generar codigo QR");
				genericResponse.setResponse(null);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return new ResponseEntity<>(genericResponse, status);
	}

}
