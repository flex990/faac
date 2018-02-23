package ad.demo.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ad.demo.util.QueryBuilder;

public class User
{
	private static final String[] SELECT_LIST = {
												 "a.user"
												,"a.password"
	};
	
	protected static final String FROM_CLAUSE = "faac_users a";
	
	private static final String ORDER_BY_CLAUSE = "1 ASC";
	
	/**
	* Ricerca generica
	* @param qb - QueryBuilder
	* @return User che soddisfa i criteri di ricerca
	* @throws SQLException
	*/
	public static User[] find(QueryBuilder qb)
		throws SQLException
	{
		/* Valorizza la select list */
		for (int i = 0; i < SELECT_LIST.length; qb.addSelectField(SELECT_LIST[i++]));
		
		/* Valorizza la from clause */
		if(qb.getFromClause()==null)
			qb.setFromClause(FROM_CLAUSE);
		
		/* Valorizza la order by clause */
		if(qb.getOrderByClause()==null)
			qb.setOrderByClause(ORDER_BY_CLAUSE);
		
		PreparedStatement pstmt = qb.generatePreparedStatement();
		
		try
		{
			ResultSet rs = pstmt.executeQuery();
			
			try
			{
				List<User> userList = new ArrayList<User>();
				
				while (rs.next())
				{
					User user = new User();
					
					/* Impostazione campi comuni */
					user.setUsername(rs.getString("USER"));
					user.setPassword(rs.getString("PASSWORD"));
					
					userList.add(user);
				}
				
				return userList.toArray(new User[0]);
			}
			finally
			{
				/* Rilascio delle risorse allocate */
				rs.close();
			}
		}
		finally
		{
			/* Rilascio delle risorse allocate */
			pstmt.close();
		}
	}
	
	/* Ricerca per chiave primaria */
	public static User findByPrimaryKey(String username)
		throws SQLException
	{
		QueryBuilder qb = new QueryBuilder();
		
		qb.addFieldCondition("a.user", QueryBuilder.EQUALS, username);
		
		try
		{
			return find(qb)[0];
		}
		catch (IndexOutOfBoundsException ioobe)
		{
			return null;
		}
	}
	
	private String username;
	private String password;
	
	public String getUsername()
	{
		return username;
	}
	
	public String getPassword()
	{
		return password;
	}
	
	public void setUsername(String username)
	{
		this.username = username;
	}
	
	public void setPassword(String password)
	{
		this.password = password;
	}

}
