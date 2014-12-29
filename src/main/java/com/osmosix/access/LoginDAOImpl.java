package com.osmosix.access;

/**
 * Created by irraju on 04/09/14.
 */
import org.springframework.stereotype.Repository;

/**
 * Implementation of the LoginDao Interface
 * @author come2niks
 */
@Repository("loginDAO")
public class LoginDAOImpl implements LoginDAO {

	@Override
	public boolean validate(LoginModel loginModel)
	{
		try{

		}catch (Exception ex){
			return false;
		}
		return true;
	}
}