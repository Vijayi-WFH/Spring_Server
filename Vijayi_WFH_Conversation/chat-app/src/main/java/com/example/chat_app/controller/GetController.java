package com.example.chat_app.controller;

import com.example.chat_app.config.MigrationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(value = "*")
@RestController
@RequestMapping("/api/mig")
public class GetController {

    @Autowired
    MigrationData migrationData;

    @GetMapping("/test")
    public ResponseEntity<Object> test() {
        migrationData.migrateData();
        System.out.println("Migration has been done.");
        return  new ResponseEntity<>("Welcome to Vijayi!",HttpStatus.OK);
    }
}
