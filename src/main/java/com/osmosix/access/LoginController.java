package com.osmosix.access;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.ValidationUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by irraju on 04/09/14.
 */
@Controller
@RequestMapping("user/")
public class LoginController {

	@Autowired
	private LoginService loginService;


	//Just a method to return weather the service is ready or not.
	@RequestMapping(value = "sendOk", method = RequestMethod.GET)
	public @ResponseBody
	ModelAndView heartbeat(HttpServletRequest request) {
		RedirectView redirectView = new RedirectView("../../index.html", true);
		return new ModelAndView(redirectView);
	}

	@RequestMapping(value = "login", method = RequestMethod.POST)
	public void login(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		if(request.getCharacterEncoding() == null){
			try {
				request.setCharacterEncoding("UTF-8");
			} catch (UnsupportedEncodingException exception) {
				throw new ServletException(exception);
			}
		}
		try
		{

			HttpSession httpSession = request.getSession(true);
			LoginModel creds = new LoginModel();
			creds.setUserName(request.getParameter("username"));
			creds.setUserName(request.getParameter("password"));

			if(loginService.validate(creds)){
				httpSession.setAttribute("LOGGED-IN", true);

			}else{

				httpSession.setAttribute("LOGGED-IN", false);
			}

		} catch (Exception e) {
			System.out.println("Exception in LoginController "+e.getMessage());
			e.printStackTrace();
			try{
				if (!response.isCommitted()) {
					response.addHeader("CAN-NOT-AUTH:",e.getMessage());
					response.sendError(521);
				}
			}catch(IOException ioe){
				throw new ServletException("Unable to send error msg "+ioe.getMessage());
			}

		}
	}

}
