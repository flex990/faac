package ad.demo.util;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/*
   Generazione dinamica del testo di una istruzione SELECT.

   Vengono forniti i singoli componenti della istruzione SELECT.

   La struttura della SELECT e' la seguente:

   SELECT <field_or_expression1>
         ,<field_or_expression2>
          .
          .
          .
      FROM <from-clause>
         WHERE <where-clause-prefix>
           AND <field-condition1>
           AND <field-condition2>
           .
           .
           .
           AND <where-clause-postfix>
         GROUP BY <group-by-clause>
         ORDER BY <order-by-clause>

   Le <field-condition> vengono generate in base all'operatore richiesto.
   
   La <from-clause>, <where-clause-prefix> e la <where-clause-postfix> possono 
   avere dei markers di parametri.
   E' compito del programmatore associargli un valore una volta ottenuto
   il PreparedStatement.
   I parametri delle <field-condition> vengono invece assegnati in automatico.
*/

public class QueryBuilder implements Serializable
{
	/* Costanti per operatori per field condition */
	public static final int EQUALS=0;
	public static final int NOT_EQUALS=1;
	public static final int GREATER_THAN=2;
	public static final int GREATER_THAN_EQUALS=3;
	public static final int LESS_THAN=4;
	public static final int LESS_THAN_EQUALS=5;
	public static final int LIKE=6;
	public static final int ISNULL=7;
	public static final int ISNOTNULL=8;
	public static final int ENUMERAZIONE=9; 
	public static final int NOT_LIKE=10;  


   /* Array con le stringhe degli operatori */
   private static final String[] OPERATORS={"=?"
                                           ,"<>?"
                                           ,">?"
                                           ,">=?"
                                           ,"<?"
                                           ,"<=?"
                                           ," LIKE ?"
                                           ," IS NULL"
                                           ," IS NOT NULL"
										   ,""       
										   ," NOT LIKE ?"                                     
                                           };

   /* Array con stringhe costanti istruzione SELECT */
   private static final String[] KEYWORDS={"SELECT "
                                          ,"FROM "
                                          ,"WHERE "
										  ,"GROUP BY "
                                          ,"ORDER BY "
                                          ,","
                                          ,"AND "
                                          };
                                          
   /* Costante per marker dei parametri */
   private static final char PARAMETER_MARKER='?';
                                           
   /* Separatore di linea */
   private static final String LINE_SEP=System.getProperty("line.separator");


   /* Nested class di rappresentazione field condition */
   private static class FieldCondition implements Serializable
   {

      /* Testo field condition */
      String conditionText;

      /* Indica se la field condition necessita di un valore di bind */
      boolean hasConditionValue;

      /* Valore di bind */
      Object conditionValue;


      /* Costruttore (per comodita') */
      FieldCondition(String  conditionText
                    ,boolean hasConditionValue
                    ,Object  conditionValue)
      {
         this.conditionText=conditionText;
         this.hasConditionValue=hasConditionValue;
         this.conditionValue=conditionValue;
      }

   }


   /* Conta il numero di parameter marker nella stringa specificata */
   private static int countParameterMarkers(String sqlFragment)
   {
      /* Se la stringa e' nulla ritorna 0 */
      if (sqlFragment==null)
         return 0;

      /* Conta il numero di parameter markers */
      int nrParameterMarkers=0;
      
      for (int i=0;i<sqlFragment.length();i++)
         if (sqlFragment.charAt(i)==PARAMETER_MARKER)
            nrParameterMarkers++;

      return nrParameterMarkers;         	
   }

   /* Select list */
   private List<String> selectList=new ArrayList<String>();

   /* From clause */
   private String fromClause;

   /* Where clause prefix */
   private String whereClausePrefix;

   /* Where clause postfix */
   private String whereClausePostfix;

	/* Group by clause */
	private String groupByClause;

   /* Order by clause */
   private String orderByClause;

   /* Lista di field condition */
   private List fieldConditionList=new ArrayList();

	/* Costruttore no-args */
	public QueryBuilder() 
	{}
 
   /* Aggiunge un campo (o un espressione) nella SELECT list */
   public void addSelectField(String fieldName)
   {
      selectList.add(fieldName);
   }

   /* Azzera la SELECT list */
   public void clearSelectFieldList()
   {
      selectList.clear();
   }

   /* Ritorna la SELECT list */
   public String[] getSelectFieldList()
   {
      return (String[]) selectList.toArray(new String[selectList.size()]);
   }

   /* Ritorna la clausola FROM */
   public String getFromClause()
   {
      return fromClause;
   }

   /* Imposta la clausola FROM */
   public void setFromClause(String fromClause)
   {
      this.fromClause=fromClause;
   }

   /* Ritorna il prefisso della clausola WHERE */
   public String getWhereClausePrefix()
   {
      return whereClausePrefix;
   }   
   
   /* Imposta il prefisso della clausola WHERE */
   public void setWhereClausePrefix(String whereClausePrefix)
   {
      this.whereClausePrefix=whereClausePrefix;
   }

   /* Ritorna il postfisso della clausola WHERE */
   public String getWhereClausePostfix()
   {
      return whereClausePostfix;
   }   
   
   /* Imposta il postfisso della clausola WHERE */
   public void setWhereClausePostfix(String whereClausePostfix)
   {
      this.whereClausePostfix=whereClausePostfix;
   }
   
	/** Aggiunge il postfisso della clausola WHERE */
	public void addWhereClausePostfix(String whereClausePostfix)
	{
		if (whereClausePostfix == null || whereClausePostfix.length() <= 0)
			return;

		if (this.whereClausePostfix == null || this.whereClausePostfix.length() <= 0)
			this.whereClausePostfix = "";
		else
			this.whereClausePostfix += " and ";

		this.whereClausePostfix += whereClausePostfix;
	}
	
	/* Ritorna la clausola GROUP BY */
	public String getGroupByClause()
	{
		return groupByClause;
	}

	/* Imposta la clausola GROUP BY */
	public void setGroupByClause(String groupByClause)
	{
		this.groupByClause=groupByClause;
	}

   /* Ritorna la clausola ORDER BY */
   public String getOrderByClause()
   {
      return orderByClause;
   }

   /* Imposta la clausola ORDER BY */
   public void setOrderByClause(String orderByClause)
   {
      this.orderByClause=orderByClause;
   }

   /* Aggiunge una stringa alla clausola ORDER BY */
   public void addToOrderByClause(String subClause)
   {
      if (subClause==null) return;
      if (orderByClause!=null)
         orderByClause+=", "+subClause;
      else
         orderByClause=subClause;
   }

   /* Metodi di aggiunta delle condizioni della clausola WHERE */
   public void addFieldCondition(String fieldName
                                ,int    operator
                                ,byte   value)
   {
      addCondition(fieldName,operator,new Byte(value));
   }

   public void addFieldCondition(String fieldName
                                ,int    operator
                                ,short  value)
   {
      addCondition(fieldName,operator,new Short(value));
   }

   public void addFieldCondition(String fieldName
                                ,int    operator
                                ,int    value)
   {
      addCondition(fieldName,operator,new Integer(value));
   }

   public void addFieldCondition(String fieldName
                                ,int    operator
                                ,long   value)
   {
      addCondition(fieldName,operator,new Long(value));
   }

   public void addFieldCondition(String fieldName
                                ,int    operator
                                ,float  value)
   {
      addCondition(fieldName,operator,new Float(value));
   }

   public void addFieldCondition(String fieldName
                                ,int    operator
                                ,double value)
   {
      addCondition(fieldName,operator,new Double(value));
   }

   public void addFieldCondition(String     fieldName
                                ,int        operator
                                ,BigDecimal value)
   {
      addCondition(fieldName,operator,value);
   }

   public void addFieldCondition(String fieldName
                                ,int    operator
                                ,String value)
   {
	  if ((operator==LIKE || operator==NOT_LIKE) && value!=null)
		  value="%"+value+"%";
      
      addCondition(fieldName,operator,value);
   }
   
   public void addFieldCondition(String fieldName
                                ,int    operator
                                ,char value)
   {
      addFieldCondition(fieldName
                       ,operator
                       ,String.valueOf(value));
   }

   public void addFieldCondition(String        fieldName
                                ,int           operator
                                ,java.sql.Date value)
   {
      addCondition(fieldName,operator,value);
   }

   public void addFieldCondition(String fieldName
                                ,int    operator
                                ,Time   value)
   {
      addCondition(fieldName,operator,value);
   }

   public void addFieldCondition(String    fieldName
                                ,int       operator
                                ,Timestamp value)
   {
      addCondition(fieldName,operator,value);
   }

   /* Aggiunta condizione per campo nullo o non nullo */
   public void addNullFieldCondition(String fieldName
                                    ,int    operator)
   {
      addCondition(fieldName,operator);
   }


   /* Azzera la field condition list */
   public void clearFieldConditionList()
   {
      fieldConditionList.clear();
   }

   /* Genera esclusivamente il testo dell'istruzione SQL (per notifiche) */
   public String generateSqlText()
      throws SQLException
   {
      /* Verifica che esista almeno una espressione nella SELECT list */
      if (selectList.size()==0)
         throw new SQLException("Nessun campo/espressione di SELECT specificato");

      /* Verifica che esista la clausola FROM */
      if (fromClause==null)
         throw new SQLException("Clausola FROM non specificata");

      String stmt=KEYWORDS[0]+LINE_SEP+' '+selectList.get(0)+LINE_SEP;

      /* Generazione SELECT list */
      for (int i=1;i<selectList.size();stmt+=KEYWORDS[5]+selectList.get(i++)+LINE_SEP);

      /* Clausola FROM */
      stmt+=KEYWORDS[1]+fromClause+LINE_SEP;

      /* Condizioni */
      String conditions=null;

      if (fieldConditionList.size()>0)
      {

		 conditions=((FieldCondition) fieldConditionList.get(0)).conditionText+LINE_SEP;

         for (int i=1;i<fieldConditionList.size();i++)
         {
         	conditions+=KEYWORDS[6]+((FieldCondition) fieldConditionList.get(i)).conditionText+LINE_SEP;
         } 

      }

      if (whereClausePrefix!=null || conditions!=null || whereClausePostfix!=null)
         stmt+=KEYWORDS[2];

      boolean needAnd=false;

      if (whereClausePrefix!=null)
      {
         stmt+=whereClausePrefix+LINE_SEP;

         needAnd=true;
      }

      if (conditions!=null)
         if (needAnd)
            stmt+=KEYWORDS[6]+conditions;
         else
         {
            stmt+=conditions;

            needAnd=true;
         }

      if (whereClausePostfix!=null)
         if (needAnd)
            stmt+=KEYWORDS[6]+whereClausePostfix+LINE_SEP;
         else
            stmt+=whereClausePostfix+LINE_SEP;

		if (groupByClause!=null)
			stmt+=KEYWORDS[3]+groupByClause+LINE_SEP;

      if (orderByClause!=null)
         stmt+=KEYWORDS[4]+orderByClause;

      return stmt;
   }

   /*
      Genera e ritorna il PreparedStatement.

      Si occupa di chiudere il PreparedStatement in caso di errori.
   */
   public PreparedStatement generatePreparedStatement()
      throws SQLException
   {
	   
		//Database db = new Database();
		//db.connetti();
	 		
      /* Ottiene il PreparedStatement */
      PreparedStatement pstmt=null;
      pstmt=Database.connect().prepareStatement(generateSqlText());

      /* Sostituzione valori */
      try
      {
         /* Offset per parametri della fromClause e whereClausePrefix */
         int i=countParameterMarkers(selectList.toString())+countParameterMarkers(fromClause)+countParameterMarkers(whereClausePrefix)+1;

         Iterator iter=fieldConditionList.iterator();

         while(iter.hasNext())
         {
            FieldCondition fc=(FieldCondition) iter.next();

				
            if (fc.hasConditionValue)
            {	
            	pstmt.setObject(i++,fc.conditionValue);
            }
            
         }
      }
      catch (SQLException e)
      {
         pstmt.close();

         throw e;
      }

      return pstmt;
   }


   /* Aggiunge una field condition con valore di bind */
   private void addCondition(String fieldName
                            ,int    operator
                            ,Object value)
   {
      if (operator<EQUALS || (operator>=ISNULL && operator<=ISNOTNULL) || operator>NOT_LIKE)
         throw new IllegalArgumentException("Operatore errato. Valore: "+operator);

      fieldConditionList.add(new FieldCondition(fieldName+OPERATORS[operator]
                                               ,true,value));
   }

   /* Aggiunge una field condition senza valore di bind */
   private void addCondition(String fieldName
                            ,int    operator)
   {
		if (operator<ISNULL || operator>ISNOTNULL)
         throw new IllegalArgumentException("Operatore errato. Valore: "+operator);

      fieldConditionList.add(new FieldCondition(fieldName+OPERATORS[operator]
                                               ,false,null));
   }

	/** Restituisce una copia dell'oggetto (clone) */
	public QueryBuilder cloneSerializzato()
	{
		try
		{
			return (QueryBuilder)super.clone();
		}
		catch  (CloneNotSupportedException e)
		{
			return null;
		}
	}
	
}
