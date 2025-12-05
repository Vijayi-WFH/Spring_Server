package com.example.chat_app.controller;

import com.example.chat_app.constants.Constants;
import com.example.chat_app.handlers.CustomResponseHandler;
import com.example.chat_app.service.OrganizationCleanupService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(value = "*")
@RestController
@RequestMapping("/internal")
public class InternalController {

    private static final Logger logger = LogManager.getLogger(InternalController.class.getName());

    @Autowired
    private OrganizationCleanupService organizationCleanupService;

    @PostMapping("/users/deactivate")
    public ResponseEntity<Object> deactivateUsers(@RequestBody List<Long> accountIds) {
        logger.info("Received request to deactivate {} accounts", accountIds.size());
        try {
            organizationCleanupService.deactivateAccounts(accountIds);
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "Accounts deactivated successfully");
        } catch (Exception e) {
            logger.error("Failed to deactivate accounts: {}", e.getMessage());
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/users/reactivate")
    public ResponseEntity<Object> reactivateUsers(@RequestBody List<Long> accountIds) {
        logger.info("Received request to reactivate {} accounts", accountIds.size());
        try {
            organizationCleanupService.reactivateAccounts(accountIds);
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "Accounts reactivated successfully");
        } catch (Exception e) {
            logger.error("Failed to reactivate accounts: {}", e.getMessage());
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, e.getMessage());
        }
    }

    @DeleteMapping("/org/{orgId}/delete")
    public ResponseEntity<Object> deleteOrganizationData(@PathVariable Long orgId, @RequestBody List<Long> accountIds) {
        logger.info("Received request to delete organization data for orgId: {} with {} accounts", orgId, accountIds.size());
        try {
            organizationCleanupService.deleteOrganizationData(orgId, accountIds);
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "Organization data deleted successfully");
        } catch (Exception e) {
            logger.error("Failed to delete organization data for orgId {}: {}", orgId, e.getMessage());
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, e.getMessage());
        }
    }
}
