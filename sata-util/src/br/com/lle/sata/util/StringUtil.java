package br.com.lle.sata.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {

	public static String removeExcessoEspacos(String str) {
		String padrao = "\\s{2,}";
	    Pattern regPat = Pattern.compile(padrao);
	    Matcher matcher = regPat.matcher(str);
	    String res = matcher.replaceAll(" ").trim();
	    return res;
	}
	
	public static String concat(Object... objs) {
		if (objs == null)
			throw new IllegalArgumentException("Os parametros nao podem ser nulos");
		StringBuilder sb = new StringBuilder();
		for (Object object : objs) {
			sb.append(object.toString());
		}
	    return sb.toString();
	}
	
}
