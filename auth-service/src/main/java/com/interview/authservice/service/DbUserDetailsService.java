package com.interview.authservice.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.interview.authservice.entity.AppUser;
import com.interview.authservice.entity.Role;
import com.interview.authservice.repository.UserRepository;

@Service
public class DbUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public DbUserDetailsService(UserRepository repository) {
		this.userRepository = repository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		AppUser user = userRepository.findByUsernameWithRoles(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
		String[] roleName = user.getRoles().stream().map(Role::getName).toArray(String[]::new);
		return User.withUsername(user.getUsername()).password(user.getPassword()).roles(roleName).build();

	}

}
