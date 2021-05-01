package com.suslanium.encryptor;

import java.security.SecureRandom;
import java.util.Random;

public final class RndPassword {
    private static final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_^~'/";

    public static String generateRandomPasswordStr(int length){
        SecureRandom random = new SecureRandom();
        char[] result = new char[length];
        for(int i = 0; i < length; i++){
            result[i] = alphabet.charAt(random.nextInt(alphabet.length()));
        }
        return new String(result);
    }
    public static char[] generateRandomPasswordCharArray(int length){
        Random random = new Random();
        char[] result = new char[length];
        for(int i = 0; i < length; i++){
            result[i] = alphabet.charAt(random.nextInt(alphabet.length()));
        }
        return result;
    }
}
