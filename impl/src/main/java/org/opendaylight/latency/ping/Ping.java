/*
 * Copyright © 2015 Mingming Chen and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.latency.ping;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Ping {
    
    public static double ping02(String ipAddress) throws Exception {
        String line = null;
        double result = 0;
        try {
            Process pro = Runtime.getRuntime().exec("ping " + ipAddress);
            BufferedReader buf = new BufferedReader(new InputStreamReader(
                    pro.getInputStream()));
            int i = 0;
            while ((line = buf.readLine()) != null && i < 2) {
                System.out.println(line);
                if(i == 1) {
                	String time = "(time=\\d+.\\d+)";
                	Pattern pattern = Pattern.compile(time);
                	Matcher matcher = pattern.matcher(line);
                	if(matcher.find()){
                		String timeIs = matcher.group(i);
                		pattern = Pattern.compile("\\d+.\\d+");
                		matcher = pattern.matcher(timeIs);
                		if(matcher.find()){
                			String res = matcher.group(0);
                			result = Double.parseDouble(res);
                			pro.destroy();
                		}
                	} else {
                		System.out.println("didn't");
                	}
                }
                i++;
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        System.out.println("double is" + result);
        
		return result;
    }
    //若line含有=18ms TTL=16字样,说明已经ping通,返回1,否則返回0.
    private static int getCheckResult(String line) {  // System.out.println("控制台输出的结果为:"+line);  
        Pattern pattern = Pattern.compile("(\\d+ms)(\\s+)(TTL=\\d+)",    Pattern.CASE_INSENSITIVE);  
        Matcher matcher = pattern.matcher(line);  
        while (matcher.find()) {
            return 1;
        }
        return 0; 
    }
    public static Long pingEnter(String ipAddress) throws Exception {
        Long result = Math.round(ping02(ipAddress));
        return result;
    }
}
