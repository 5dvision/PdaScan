package com.missfresh.pda_scan;

import android.text.TextUtils;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DecodeUtils {


    private static int[] secret_key = new int[]{2024, 85, 45, 22, 65, 213, 156, 398, 356, 1809, 354};//秘钥  和服务端统一定制

    private static String[] charSet = new String[]{"u0030", "u0039"};

    /*
     *
     * 是否为纯数字
     * */
    public static boolean isNumber(String matrial) {
        if (matrial != null && !"".equals(matrial.trim()))
            return matrial.matches("^[0-9]*$");
        else
            return false;
    }

    /*
     * 是否以BG开头
     * */
    public static boolean isPackage(String bgStr) {
        Pattern pattern = Pattern.compile("^[B][G][\\w]*");
        Matcher matcher = pattern.matcher(bgStr);
        return matcher.matches();
    }

    /**
     * 扫描二维码得到的值进行截取
     * 如果是正常的包裹号和物料码，直接返回
     * 否则有可能是加密的要进行截取（eg:https://s.missfresh.cn/sp?m\u003d6013001\u0026sv\u003d0565537^^^9^^^）
     *
     * @param qrStr
     * @return
     */
    public static String getSvStr(String qrStr) {
        String code = qrStr.trim();
        if (TextUtils.isEmpty(code)) {
            return "";
        }

        String urlCode = decode2(code);
        if (urlCode.contains("http") && urlCode.contains("&sv=")) {  //表示加密的
            String[] codes = urlCode.split("&sv=");
            return codes[1];
        }

        return qrStr;
    }


    /**
     * 根据秘钥进行解密
     *
     * @param str
     * @param secret_key
     * @param charSet
     * @return
     */
    public static String unShift(String str, int[] secret_key, String[] charSet) {

        //转16进制
        int start = Integer.parseInt(charSet[0].substring(1), 16); //48
        int end = Integer.parseInt(charSet[1].substring(1), 16) - start + 1;  //10
        int size = secret_key.length;  //11

        if (BuildConfig.DEBUG) {
            Log.e("unShift", String.format("start: %s, end: %s, size: %s", start, end, size));
        }

        StringBuffer sb = new StringBuffer();
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            String single = String.valueOf(str.charAt(i));
            if (isRegxp(single)) {
                int result = (int) str.charAt(i) - start;
                if (BuildConfig.DEBUG) {
                    Log.e("unShift--", String.format("result: %s, str: %s, start: %s", result, (int) str.charAt(i), start));
                }
                //((result - secret_key[i++ % size] % end + end) % end + start)
                //  ((0 - 2024%11 + 10)%10 + 48)
                int endResult = ((result - secret_key[count++ % size] % end + end) % end + start);
                char ch = byteAsciiToChar(endResult);
                single = String.valueOf(ch);
                //6013001
                if (BuildConfig.DEBUG) {
                    Log.e("unShift2", String.format("result: %s, endResult: %s, ch: %s, single: %s", result, endResult, ch, single));
                }
            }
            sb.append(single);
        }
        return sb.toString();
    }


    /**
     *  
     * ascii转换为char 直接int强制转换为char 
     *
     * @param ascii 
     * @return       
     */
    public static char byteAsciiToChar(int ascii) {
        return (char) ascii;
    }

    /**
     * 字符串转换成为16进制(无需Unicode编码)
     *
     * @param str
     * @return
     */
    public static String str2HexStr(String str) {
        char[] chars = "0123456789ABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder("");
        byte[] bs = str.getBytes();
        int bit;
        for (int i = 0; i < bs.length; i++) {
            bit = (bs[i] & 0x0f0) >> 4;
            sb.append(chars[bit]);
            bit = bs[i] & 0x0f;
            sb.append(chars[bit]);
            // sb.append(' ');
        }
        return sb.toString().trim();
    }

    /**
     * 根据正则判断是否是数字
     *
     * @param str
     * @return
     */
    public static Boolean isRegxp(String str) {
        Boolean isCode = false;
        try {
            //RegExp('[\\' + _cset[0] + '-\\' + _cset[1] + ']', 'g')
            Pattern p = Pattern.compile("[0-9]");
            Matcher m = p.matcher(str);
            isCode = m.matches();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isCode;
    }




    /**
     * 转中文
     *
     * @param s
     * @return
     */
    public static String decode2(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '\\' && chars[i + 1] == 'u') {
                char cc = 0;
                for (int j = 0; j < 4; j++) {
                    char ch = Character.toLowerCase(chars[i + 2 + j]);
                    if ('0' <= ch && ch <= '9' || 'a' <= ch && ch <= 'f') {
                        cc |= (Character.digit(ch, 16) << (3 - j) * 4);
                    } else {
                        cc = 0;
                        break;
                    }
                }
                if (cc > 0) {
                    i += 5;
                    sb.append(cc);
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }


}
