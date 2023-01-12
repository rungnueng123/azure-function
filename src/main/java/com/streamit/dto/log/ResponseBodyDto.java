package com.streamit.dto.log;

public class ResponseBodyDto {
	
	private String Message;
	private String Status;
	
	public ResponseBodyDto(String message, String status) {
		super();
		Message = message;
		Status = status;
	}

}
