package com.interview.authservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.interview.authservice.entity.AppUser;

@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {

	Optional<AppUser> findByUsername(String username);
	
	@Query("select u from AppUser u left join fetch u.roles where u.username = :username")
	Optional<AppUser> findByUsernameWithRoles(@Param("username") String username);
}
