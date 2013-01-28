package org.nlp.impl;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.nlp.algo.Bloom;

/**
 * @author KmaDou
 *
 */
public class BloomSegmentImpl {
	static Bloom bloom;
	static String defaultModel = "model";		
	private final static String punctuation = "[。，！？；,!?;]";	
	
	public BloomSegmentImpl(){
		this(defaultModel);
	}
	
	BloomSegmentImpl(String filePath) {
		initDic(filePath);
	}
		
	public static void initDic(){
		initDic(defaultModel);
	}
	
	public static void initDic(String filePath) {		
		if (bloom == null) {			
			bloom = new Bloom(filePath);
		}
	}		
	
	/**
	 * 全分词,返回字典里存在的词和新词
	 * @param text
	 * @return
	 */
	public static List<String> getTokensList(String text){		
		List<String> result = new ArrayList<String>();
		for (TokendWords word : getTokens(text,0)){
			result.add(word.getWord());
		}
		return result;
	}
		
	/**
	 * 基本分词,返回字典里存在的词
	 * @param text
	 * @return
	 */
	public static List<String> getBaseList(String text){
		List<String> result = new ArrayList<String>();
		for (TokendWords word : getTokens(text,1)){
			result.add(word.getWord());
		}
		return result;
	}	
				
	/**
	 * 基本分词,返回字典里存在的词
	 * @param text
	 * @return
	 */
	public static List<String> getNewList(String text){
		List<String> result = new ArrayList<String>();
		for (TokendWords word : getTokens(text,2)){
			result.add(word.getWord());
		}
		return result;
	}	
			
	
	/**
	 * @param text
	 * @param mode //0 全部词语 1基本词语 2新词识别
	 * @return
	 */
	public static List<TokendWords> getTokens(String text,int mode){
		List<TokendWords> result = new ArrayList<TokendWords>();
		StringBuffer NewWord = new StringBuffer();
				
		int lastOffset = 0;
		int start = 0;
		int pos = 0;
		BitSet bitSet = new BitSet(text.length()+1);
		int minGramSize = 2;
		int maxGramSize = 5;
		
		int length = 0;
		String[] split = text.split(punctuation); //把文本切割成句子
		for (int i = 0; i < split.length; i++) {					
			length = split[i].length();				
		//进行minGramSize到maxGramSize元切词
		for (int gramSize = minGramSize; gramSize <= maxGramSize; gramSize++) {
			//第gramSize元切词
			for (int n = 0; n <= length - gramSize; n++) {
				//用bloom过滤器保留在词典里的词
				if (bloom.contains(split[i].substring(n, n + gramSize))) {
					//当前切割的词语:text.substring(i, i + gramSize)
					if (mode != 2){
						result.add(new TokendWords(split[i].substring(n, n + gramSize), 1l,new String[] { "Word" }, gramSize, pos, start));
						pos++;
					}
					if (mode != 1){
						bitSet.set((start + n), (start + n + gramSize));	
					}					
					
				}
			}		
		}		
		bitSet.set(start+split[i].length());
		start = start + length + 1;
			
		}
		
		if (mode == 1)
			return result;
		
		//新词识别
		for (int i = 0; i < text.length(); i++) {
			if (!bitSet.get(i)) {//判断当前字附是否已经作为已识别词语
				//如果新词缓存长度为0认为是新词的第一个字
				if (NewWord.length() == 0) {
					start = i;
					NewWord.append(text.charAt(i));
					//设置新词开始位置					
				} else if ((i - lastOffset) == 1) { //当前位置和上一个字符偏移量为1则添加到新词缓冲区
					NewWord.append(text.charAt(i));
				} else {// 偏移量不等于1,说明上一个词已经结束,记录新词,并开始新一轮新词记录					
					result.add(new TokendWords(NewWord.toString(), 1l,new String[] { "NewWord" }, NewWord.length(), pos, start));
					pos++;					
					NewWord.delete(0, NewWord.length());
					start = i;
					NewWord.append(text.charAt(i));
				}
				lastOffset = i;
			}
			if (i == text.length()-1 && NewWord.length() > 0) {//最后一次循环将要退出时检查新词缓冲区内是否有字
				result.add(new TokendWords(NewWord.toString(), 1l,new String[] { "NewWord" }, NewWord.length(), pos, start));				
			}
		}		
		
		return result;
	}	
	
	
	public static void main(String[] args) {
		String s = "张一凡的电话13699191946,在上海市浦东新区耀华路99弄16号10402,上海市徐汇区东新路99弄38号402";
		s = "中华人民共和国,电话13810000000邮编100044";
		BloomSegmentImpl bloomSegmentImpl = new BloomSegmentImpl();
		System.out.println(bloomSegmentImpl.getBaseList(s));
		System.out.println(bloomSegmentImpl.getNewList(s));
	}
}