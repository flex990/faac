package ad.demo.controller;

import java.sql.SQLException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ad.demo.model.User;

@RestController
public class UserController
{
	@CrossOrigin(origins = "http://localhost:4200")
	@RequestMapping("/login")
    public ResponseEntity<String> login(@RequestParam Map<String, String> requestParams, Model model)
    {
		User user;
		String username = requestParams.get("username");
		String password = requestParams.get("password");
		
		try
		{
			user = User.findByPrimaryKey(username);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return new ResponseEntity<String>("Errore connessione al server", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		if (user != null)
		{
			if (!user.getPassword().equals(password))
				return new ResponseEntity<String>("Password errata", HttpStatus.UNAUTHORIZED);
		}
		else
			return new ResponseEntity<String>("Utente inesistente", HttpStatus.UNAUTHORIZED);
    	
		return new ResponseEntity<String>("Connesso", HttpStatus.OK);
    }
}
