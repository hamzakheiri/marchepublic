package fr._42.marchepublic.controller;


import fr._42.marchepublic.model.User;
import fr._42.marchepublic.service.UsersService;
import org.springframework.web.bind.annotation.*;
//import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.processing.Generated;

@RestController
@RequestMapping("/test")
public class test {
    private final UsersService usersService;

    public test(UsersService usersService) {
        this.usersService = usersService;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello World";
    }

    @PostMapping("/createuser")
    public User CreateUser(@RequestBody User user){
        return usersService.createUser(user);
    }
}
