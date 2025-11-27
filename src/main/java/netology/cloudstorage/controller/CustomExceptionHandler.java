package netology.cloudstorage.controller;

import netology.cloudstorage.dto.ErrorResponse;
import netology.cloudstorage.exceptions.DuplicateUsernameException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CustomExceptionHandler {

    /**
     * Обработка ситуации, когда имя пользователя уже занято.
     * Возникает при попытке регистрации нового пользователя с уже существующим именем.
     * Возвращает ответ с HTTP-статусом CONFLICT (409) и объектом ошибки, содержащим сообщение о конфликте.
     *
     * @param ex исключение, вызванное попыткой создать пользователя с повторяющимся именем.
     * @return объект ответа с описанием ошибки и соответствующим HTTP-статусом.
     */
    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateUsernameException(DuplicateUsernameException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

}
