package com.tse.core_application.service.Impl;


import com.tse.core_application.config.MyUserDetails;
import com.tse.core_application.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MyUserDetailsService implements UserDetailsService {

	public MyUserDetailsService() {
	}
	@Autowired
    UserService userService;
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		/*TODO:utk: logic for fetching userDetails object from username, most probably from userDetails service.*/
		User user = userService.getUserByUserName(username);

		/*We're passing the user name as entered in default form,
		 * Password is whatever we have returned from MyUserDetails bean.
		 * Encoding is done there.
		 * */
		
		/*Here we're retrieving our Admin object based on admin's email provided,
		 * Then we are populating that found Admin object into MyUserDetails and getting the value in all the getters
		 * */
//		Admin adminObj = adminService.getAdminByEmail(username);
		
        /*After populating adminobj to myuserDetails, implement all the methods correctly, eg getPassword, return password from adminObject set by constructor, etc etc*/
        return new MyUserDetails(user);
//	return null;
	}

}
