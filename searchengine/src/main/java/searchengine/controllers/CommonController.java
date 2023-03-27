package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import searchengine.dto.ApiResponse;
import searchengine.error.ErrorMessage;

public class CommonController {

    @GetMapping("/indexPage")
    public ResponseEntity<ApiResponse> okResponse() {
        return new ResponseEntity<>(
            new ApiResponse(true), HttpStatus.OK
        );
    }
}
