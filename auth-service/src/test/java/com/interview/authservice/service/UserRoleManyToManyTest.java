package com.interview.authservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.interview.authservice.entity.AppUser;
import com.interview.authservice.entity.Role;
import com.interview.authservice.repository.RoleRepository;
import com.interview.authservice.repository.UserRepository;

/**
 * Track B — proves User<->Role many-to-many (raw @ManyToMany + @JoinTable, a PURE join):
 *  - a user's roles persist to the user_role join and reload via the fetch-join
 *  - DbUserDetailsService maps role names to Spring authorities (ROLE_ prefix)
 */
@DataJpaTest
@Import(DbUserDetailsService.class)
class UserRoleManyToManyTest {

	@Autowired
	UserRepository userRepository;
	@Autowired
	RoleRepository roleRepository;
	@Autowired
	DbUserDetailsService userDetailsService;
	@Autowired
	TestEntityManager em;

	@Test
	void userRolesPersistViaJoin_andMapToAuthorities() {
		Role userRole = roleRepository.save(new Role("USER"));
		Role adminRole = roleRepository.save(new Role("ADMIN"));

		AppUser hemanth = new AppUser("hemanth", "pw");
		hemanth.addRole(userRole);
		hemanth.addRole(adminRole);
		userRepository.save(hemanth);

		em.flush();
		em.clear();

		// entity level: both roles come back through the join
		AppUser reloaded = userRepository.findByUsernameWithRoles("hemanth").orElseThrow();
		assertThat(reloaded.getRoles()).extracting(Role::getName)
				.containsExactlyInAnyOrder("USER", "ADMIN");

		// service level: mapped to Spring authorities with the ROLE_ prefix
		UserDetails details = userDetailsService.loadUserByUsername("hemanth");
		assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
				.containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
	}
}
