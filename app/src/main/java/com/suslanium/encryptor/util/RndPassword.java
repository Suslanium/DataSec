package com.suslanium.encryptor.util;

import java.security.SecureRandom;
import java.util.Random;

public final class RndPassword {
    private static final String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String lowercase = "abcdefghijklmnopqrstuvwxyz";
    private static final String numbers = "0123456789";
    private static final String symbols = "~!@#$%^&*()_";

    /**
     * @param mode the selected mode for password. U means uppercase, L means lowercase, N means numbers, S means symbols(example: ULNS means all symbols)
     * @return
     */
    public static String generateRandomPasswordStr(int length, String mode){
        String alphabet = "";
        if(mode.contains("U")){
            alphabet = alphabet+uppercase;
        }
        if(mode.contains("L")){
            alphabet = alphabet+lowercase;
        }
        if(mode.contains("N")){
            alphabet = alphabet+numbers;
        }
        if(mode.contains("S")){
            alphabet = alphabet+symbols;
        }
        SecureRandom random = new SecureRandom();
        char[] result = new char[length];
        for(int i = 0; i < length; i++){
            result[i] = alphabet.charAt(random.nextInt(alphabet.length()));
        }
        return new String(result);
    }

    /**
     * @param mode the selected mode for password. U means uppercase, L means lowercase, N means numbers, S means symbols(example: ULNS means all symbols)
     * @return
     */
    public static char[] generateRandomPasswordCharArray(int length, String mode){
        String alphabet = "";
        if(mode.contains("U")){
            alphabet = alphabet+uppercase;
        }
        if(mode.contains("L")){
            alphabet = alphabet+lowercase;
        }
        if(mode.contains("N")){
            alphabet = alphabet+numbers;
        }
        if(mode.contains("S")){
            alphabet = alphabet+symbols;
        }
        SecureRandom random = new SecureRandom();
        char[] result = new char[length];
        for(int i = 0; i < length; i++){
            result[i] = alphabet.charAt(random.nextInt(alphabet.length()));
        }
        return result;
    }
}
