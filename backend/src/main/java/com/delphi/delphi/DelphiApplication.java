package com.delphi.delphi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.delphi.delphi.entities.User;
import com.delphi.delphi.services.UserService;

@SpringBootApplication
@RestController
// TODO: Enable async support?
// @EnableAsync

public class DelphiApplication {

    private final UserService userService;

    DelphiApplication(UserService userService) {
        this.userService = userService;
    }
	// TODO: add authorization to all endpoints
	// TODO: add more endpoints for data access
	private User getCurrentUser() {
        return userService.getUserByEmail(getCurrentUserEmail());
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userDetails.getUsername();
    }
	
	@GetMapping("/")
	public String hello() {
		return "Hello, World!";
	}

	@GetMapping("/health")
	public String health() {
		User user = getCurrentUser();
		return "Welcome, " + user.getName() + "!";
	}

	public static void main(String[] args) {
		SpringApplication.run(DelphiApplication.class, args);
	}

}
