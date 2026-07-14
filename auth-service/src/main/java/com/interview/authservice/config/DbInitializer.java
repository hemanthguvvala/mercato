package com.interview.authservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.interview.authservice.entity.AppUser;
import com.interview.authservice.repository.UserRepository;

@Component
public class DbInitializer implements CommandLineRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${app.seed.user-password}")
	private String userPassword;
	@Value("${app.seed.admin-password}")
	private String adminPassword;

	public DbInitializer(UserRepository repository, PasswordEncoder encoder) {
		this.userRepository = repository;
		this.passwordEncoder = encoder;
	}

	@Override
	public void run(String... args) throws Exception {
		if (userRepository.findByUsername("hemanth").isEmpty()) {
			userRepository.save(new AppUser("hemanth", passwordEncoder.encode(userPassword), "USER"));
		}
		if (userRepository.findByUsername("admin").isEmpty()) {
			userRepository.save(new AppUser("admin", passwordEncoder.encode(adminPassword), "ADMIN"));
		}
	}

}
