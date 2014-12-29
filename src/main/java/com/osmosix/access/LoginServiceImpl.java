package com.osmosix.access;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by irraju on 04/09/14.
 */
@Service("loginService")
public class LoginServiceImpl implements LoginService {
	@Autowired
	LoginDAO loginDAO;

	public boolean validate(LoginModel loginModel) {
		return loginDAO.validate(loginModel);
	}

}