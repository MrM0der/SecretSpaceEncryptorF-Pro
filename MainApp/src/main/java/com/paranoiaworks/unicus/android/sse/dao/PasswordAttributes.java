package com.paranoiaworks.unicus.android.sse.dao;

import com.paranoiaworks.unicus.android.sse.R;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Support password related atributes
 * in version 1.0.x mainly "Password Strength"
 * 
 * @author Paranoia Works
 * @version 1.0.1
 */
public class PasswordAttributes {
	
	int passwordStrength;
	int passwordStrengthWeight;
	
	public  PasswordAttributes(char[] password) {
		Integer[] strength = new Integer[2];
		checkPasswordStrengthWeight(password, strength);
		this.passwordStrength = strength[0];
		this.passwordStrengthWeight = strength[1];
	}
	
	public int getDrawableID()
	{
		return getDrawableID(this.passwordStrengthWeight);
	}
	
	public static int getDrawableID(int psw)
	{
        int drawableID = -1;
        switch (psw) {
            case 1:  drawableID = R.drawable.d_button_password_1; break;
            case 2:  drawableID = R.drawable.d_button_password_2; break;
            case 3:  drawableID = R.drawable.d_button_password_3; break;
            case 4:  drawableID = R.drawable.d_button_password_4; break;
            case 5:  drawableID = R.drawable.d_button_password_5; break;
            default: drawableID = R.drawable.d_button_password_0; break;
        }
        return drawableID;
	}
	
	public int getCommentID()
	{
		return getCommentID(this.passwordStrengthWeight);
	}
	
	public static int getCommentID(int psw)
	{
        int commentID = -1;
        switch (psw) {
            case 1:  commentID = R.string.passwordDialog_passwordWeak; break;
            case 2:  commentID = R.string.passwordDialog_passwordWeak; break;
            case 3:  commentID = R.string.passwordDialog_passwordFair; break;
            case 4:  commentID = R.string.passwordDialog_passwordStrong; break;
            case 5:  commentID = R.string.passwordDialog_passwordStrong; break;
            default: commentID = R.string.passwordDialog_passwordShort; break;
        }
        return commentID;
	}
	
	public int getSMImageID()
	{
		return getSMImageID(this.passwordStrengthWeight);
	}
	
	public static int getSMImageID(int psw)
	{
        int smiid = -1;
        switch (psw) {
	        case 1:  smiid = R.drawable.strength_metter_1; break;
	        case 2:  smiid = R.drawable.strength_metter_2; break;
	        case 3:  smiid = R.drawable.strength_metter_3; break;
	        case 4:  smiid = R.drawable.strength_metter_4; break;
	        case 5:  smiid = R.drawable.strength_metter_5; break;
	        default: smiid = R.drawable.strength_metter_0; break;
        }
        return smiid;
	}

	public static int checkPasswordStrengthWeight(char[] password)
	{
		return checkPasswordStrengthWeight(password, null);
	}
	
	public static int checkPasswordStrengthWeight(char[] password, Integer[] fullResults)
	{
        int strength = checkPasswordStrength(password);
        int weight = 0;
        if(strength == 0) weight = 0;
        else if(strength < 25) weight = 1;
		else if(strength < 50) weight = 2;
		else if(strength < 70) weight = 3;
		else if(strength < 90) weight = 4;
		else weight = 5;
		if(fullResults != null) {
			fullResults[0] = strength;
			fullResults[1] = weight;
		}
		return weight;
	}
	
	public static int checkPasswordStrength(char[] password)
	{
		if(password.length < 8) return 0;
		char[] passwordC = Arrays.copyOf(password, password.length);
		char[] noDup = removeDuplicatesChar(passwordC);
		if(noDup.length < 4) {
			Arrays.fill(noDup, '\u0000');
			Arrays.fill(passwordC, '\u0000');
			return 1;
		}
		
		double strength = 0;
		double multip = 1;
		
		final int upperCase = matches(passwordC, "[A-Z]");
		double upperCaseWeight = 2.0;
		final int lowerCase = matches(passwordC, "[a-z]");
		double lowerCaseWeight = 2.0;
		final int numbers = matches(passwordC, "[0-9]");
		double numbersWeight = 1.6;
		final int specialCharacters = passwordC.length - upperCase - lowerCase - numbers;
		double specialCharactersWeight = 2.5;
		
		if (upperCase > 0) multip += 0.5;
		if (lowerCase > 0) multip += 0.5;
		if (numbers > 0) multip += 0.5;
		if (specialCharacters > 0) multip += 0.5;

		strength =  (upperCase * upperCaseWeight) +
					(lowerCase * lowerCaseWeight) +
					(numbers * numbersWeight) +
					(specialCharacters * specialCharactersWeight);
		
		double entropy = (double)passwordC.length - (double)noDup.length;

		strength -= entropy;

		Arrays.fill(noDup, '\u0000');
		Arrays.fill(passwordC, '\u0000');

		return (int) (strength * multip);
	}
	
	private static int matches(final char[] string, final String regexPattern) {
		int matches = 0;
		final Pattern pattern = Pattern.compile(regexPattern);
		final Matcher matcher = pattern.matcher(CharBuffer.wrap(string));

		while (matcher.find()) {
			++matches;
		}

		return matches;
	}

	private static char[] removeDuplicatesChar(char[] chars)
	{
		int noUniqueChars = chars.length;
		for (int i = 0; i < noUniqueChars; i++)
		{
			for (int j = i+1; j < noUniqueChars; j++)
			{
				if(chars[i] == chars[j])
				{
					chars[j] = chars[noUniqueChars - 1];
					noUniqueChars--;
					j--;
				}
			}
		}

		return Arrays.copyOf(chars, noUniqueChars);
	}
}
