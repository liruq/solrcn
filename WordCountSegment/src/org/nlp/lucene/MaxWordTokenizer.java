package org.nlp.lucene;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.nlp.impl.TokendWords;

public class MaxWordTokenizer extends Tokenizer {

	private CharTermAttribute termAtt;
	private static final int IO_BUFFER_SIZE = 4096;
	private char[] ioBuffer = new char[IO_BUFFER_SIZE];

	private final StringBuilder buffer = new StringBuilder();
	
	private Iterator<String> tokenIter;
	private List<TokendWords> tokenBuffer;
	private final static String[] tokenAttrib = new String[] { "Word" };
	
	private int tokenStart = 0, tokenEnd = 0, tokenLength = 0 , tokenPos =0;
	private boolean hasIllegalOffsets;
	private int currentTokenType = 0, tokenType = 0;	

	private boolean done;
	private int i = 0; // i是用来控制起始位置的变量
	private int upto = 0;

	public MaxWordTokenizer(Reader input) {
		super(input);
		termAtt = (CharTermAttribute) addAttribute(CharTermAttribute.class);
		this.done = false;
	}

	public void resizeIOBuffer(int newSize) {
		if (ioBuffer.length < newSize) {
			final char[] newCharBuffer = new char[newSize];
			System.arraycopy(ioBuffer, 0, newCharBuffer, 0, ioBuffer.length);
			ioBuffer = newCharBuffer;
		}
	}

	@Override
	public boolean incrementToken() throws IOException {
		clearAttributes();
		buffer.setLength(0);

		ArrayList<String> al = new ArrayList<String>();
		char ch;
		int ci;

		tokenStart = tokenEnd;
		ci = input.read();
		ch = (char) ci;
		currentTokenType = tokenType = Character.getType(ch);

		while (true) {
			if (ci == -1) {
				break;
			} else {
				if (currentTokenType == tokenType) {
					buffer.append(ch);
					tokenLength++;
				} else {
					tokenBuffer.add(new TokendWords(buffer.toString(), 1L,
							tokenAttrib, tokenLength, tokenPos, 0));
					tokenPos++;
					al.add(buffer.toString());
					buffer.delete(0, tokenLength);
					tokenLength = 0;
				}

				ci = input.read();
				ch = (char) ci;
				tokenType = Character.getType(ch);				
				tokenEnd++;
			}
		}

		if (al.size() > 0) {
			hasIllegalOffsets = (tokenStart + termAtt.length()) != tokenEnd;
			tokenIter = al.iterator();
			String nextWord = tokenIter.next();			
			termAtt.copyBuffer(nextWord.toCharArray(), 0, nextWord.length());
			return true;
		} else {
			return false;
		}

	}

}
