package com.interview.authservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.interview.authservice.entity.AppUser;
import com.interview.authservice.entity.Role;
import com.interview.authservice.repository.RoleRepository;
import com.interview.authservice.repository.UserRepository;

@Component
public class DbInitializer implements CommandLineRunner {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${app.seed.user-password}")
	private String userPassword;
	@Value("${app.seed.admin-password}")
	private String adminPassword;

	public DbInitializer(UserRepository repository, PasswordEncoder encoder, RoleRepository roleRepository) {
		this.userRepository = repository;
		this.passwordEncoder = encoder;
		this.roleRepository = roleRepository;
	}

	@Override
	@Transactional
	public void run(String... args) throws Exception {

		Role userRole = roleRepository.findByName("USER").orElseGet(() -> roleRepository.save(new Role("USER")));
		Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> roleRepository.save(new Role("ADMIN")));
		if (userRepository.findByUsername("hemanth").isEmpty()) {
			AppUser user = new AppUser("hemanth", passwordEncoder.encode(userPassword));
			user.addRole(userRole);
			userRepository.save(user);
		}
		if (userRepository.findByUsername("admin").isEmpty()) {
			AppUser admin = new AppUser("admin", passwordEncoder.encode(adminPassword));
			admin.addRole(adminRole);
			userRepository.save(admin);
		}
	}

}
