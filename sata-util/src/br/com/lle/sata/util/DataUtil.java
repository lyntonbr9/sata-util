package br.com.lle.sata.util;

import java.sql.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DataUtil { 

	public static int getDiferencaDias(Date dia1, Date dia2) {
		return getDiferencaDias(converteToCalendar(dia1), converteToCalendar(dia2));
	}
	
	public static int getDiferencaDias(Calendar dia1, Calendar dia2) {
		long longDia = dia1.getTimeInMillis();
		long longFechamento = dia2.getTimeInMillis();
		return (int) (((longFechamento - longDia) / (24*60*60*1000)));
	}
	
	public static Date converteToDate(String data) throws ParseException {
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
		java.util.Date dataUtil = df.parse(data);
		return new Date(dataUtil.getTime()); 
	}
	
	public static Calendar converteToCalendar(Date data) {
		Calendar cal = Calendar.getInstance(); 
		cal.setTime(data);
		return cal;
	}
	
	public static String getDataAtualStr(String pattern) {
		DateFormat df = new SimpleDateFormat(pattern);
		return df.format(Calendar.getInstance().getTime());
	}
	
	public static Date getDataAtual() {
		java.util.Date dataAtual = Calendar.getInstance().getTime(); 
		return new Date(dataAtual.getTime());
	}
	
	public static String format(Date data, String pattern) {
		DateFormat df = new SimpleDateFormat(pattern);
		return df.format(converteToCalendar(data).getTime());
	}
	
	public static Date addDays(Date data, int dias) {
		Calendar cal = converteToCalendar(data);
		cal.add(Calendar.DAY_OF_MONTH, dias);
		return new Date(cal.getTime().getTime());
	}
	
	public static Date addDays(int dias) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, dias);
		return new Date(cal.getTime().getTime());
	}
	
}
