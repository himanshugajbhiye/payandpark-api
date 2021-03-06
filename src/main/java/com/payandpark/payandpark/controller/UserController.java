package com.payandpark.payandpark.controller;

import com.payandpark.payandpark.user.model.CreateUserRequest;
import com.payandpark.payandpark.user.model.User;
import com.payandpark.payandpark.user.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Log4j2
@Controller
public class UserController {

    @Autowired
    UserService userService;

    @PutMapping(value = "/parking/user/create/")
    ResponseEntity createUser(@RequestBody CreateUserRequest request) {
        log.info("Request received to create new user for mobile :: {}", request.getMobile());
        userService.createUser(request);
        return new ResponseEntity(HttpStatus.OK);
    }

    @GetMapping(value = "/parking/user/fetch/{userId}")
    ResponseEntity fetchUser(@PathVariable int userId) {
        log.info("Request received to fetch user :: {}", userId);
        User user = userService.fetchUserById(userId);
        return new ResponseEntity(user, HttpStatus.OK);
    }
}
