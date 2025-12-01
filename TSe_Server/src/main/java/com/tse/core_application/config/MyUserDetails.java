package com.tse.core_application.config;

//import admin.xflow_admin.admin.Admin;
import com.tse.core_application.model.User;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Collection;
import java.util.List;


/*This is kind of user details bean, which will be instantiated in UserDetailService*/
public class MyUserDetails implements UserDetails {

	private User userObj;

	public MyUserDetails(User adminObj) {
	   this.userObj = adminObj;
	}
	
	//This function returns a collection of granted authorities
	/*what's a role and authority
	 * : :https://stackoverflow.com/questions/19525380/difference-between-role-and-grantedauthority-in-spring-security
	 * */
	
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// TODO Auto-generated method stub
		String role ="USER";// this.userObj.getRole();
		//This is the syntax which needs to be followed for naming role.
		role="ROLE_"+role;
		/*
		 * AFAIK GrantedAuthority and roles are same in spring security. GrantedAuthority's getAuthority() string is the role (as per default implementation SimpleGrantedAuthority).
		 * 
		 * */
		
		
		/*Here we are setting the role from our db object, to authority of user details bean.*/
		/*The authorization would be done in SecurityConfiguration's configure(HttpSecurity http) method*/
		/*
		 * Since you're calling hasRole method in authorization, the fact that you're passing a role is implied.:
		 * */
		/*
		 * But since from here we are passing authority, we have to add prefix ROLE_
		 * link for more details: https://stackoverflow.com/questions/42146110/when-should-i-prefix-role-with-spring-security
		 * 
		 * */
		/*
		 * SimpleGrantAuthority constructor converts a role into an authority.
		 * */ 
		
		return List.of(new SimpleGrantedAuthority(role));
	}

	@Override
	public String getPassword() {
		// TODO Auto-generated method stub
		String password = "TEMP_HARDCODED";//this.userObj.getPassword();
        return password;
		//		return passwordEncoder().encode(password);
	}
 
	 
	public User getAdminObj() {
		// TODO Auto-generated method stub
		return userObj;
	}

	@Override
	public boolean isAccountNonExpired() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return true;
	}

	
	
	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
	    return new BCryptPasswordEncoder(4);
	}

	@Override
	public String getUsername() {
		// TODO Auto-generated method stub
	    return "Sample hardcoded";
//		return null;
	}
	
}
