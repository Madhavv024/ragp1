package com.madhavv.enterpriserag.service;

import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.Encodings;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.stereotype.Component;
import com.knuddels.jtokkit.api.IntArrayList;

import java.util.ArrayList;
import java.util.List;

@Component
public class OverlapTokenTextSplitter extends TextSplitter {

    private final int chunkSize;
    private final int overlap;
    private final int minChunkSizeChars;
    private final int minChunkLengthToEmbed;
    private final int maxNumChunks;
    private final boolean keepSeparator;
    private final List<Character> punctuationMarks;

    private final EncodingRegistry registry;
    private final Encoding encoding;

    public OverlapTokenTextSplitter() {
        this.chunkSize = 400;
        this.overlap = 50;
        this.minChunkSizeChars = 200;
        this.minChunkLengthToEmbed = 5;
        this.maxNumChunks = 10_000;
        this.keepSeparator = true;
        this.punctuationMarks = List.of('.', '?', '!', '\n');

        this.registry = Encodings.newLazyEncodingRegistry();
        this.encoding = registry.getEncoding(EncodingType.CL100K_BASE);
    }

    @Override
    protected List<String> splitText(String text) {

        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<Integer> tokens = encoding.encode(text).boxed();

        List<String> chunks = new ArrayList<>();

        int start = 0;
        int numChunks = 0;

        while (start < tokens.size() && numChunks < maxNumChunks) {

            int end = Math.min(start + chunkSize, tokens.size());

            List<Integer> candidateTokens = tokens.subList(start, end);

            IntArrayList candidateTokenList =
                    new IntArrayList(candidateTokens.size());

            candidateTokens.forEach(candidateTokenList::add);

            String chunkText = encoding.decode(candidateTokenList);

            /*
             * If this isn't the final chunk, try to move the
             * boundary back to the last punctuation mark.
             */
            if (end < tokens.size()) {
                int lastPunctuation = getLastPunctuationIndex(chunkText);
                if (lastPunctuation > minChunkSizeChars) {
                    chunkText = chunkText.substring(0, lastPunctuation + 1);
                }
            }

            String chunkTextToAppend =
                    keepSeparator
                            ? chunkText.trim()
                            : chunkText
                            .replace(
                                    System.lineSeparator(),
                                    " "
                            )
                            .trim();

            if (chunkTextToAppend.length() >
                    minChunkLengthToEmbed) {

                chunks.add(chunkTextToAppend);
            }

            /*
             * Determine how many tokens were actually consumed
             * after punctuation-based boundary adjustment.
             */
            int consumedTokens = encoding.encode(chunkText).size();

            if (consumedTokens == 0) {
                break;
            }

            /*
             * Move forward while retaining the configured
             * overlap from the previous chunk.
             */
            int nextStart = start + consumedTokens - overlap;

            if (nextStart <= start) {
                nextStart = start + consumedTokens;
            }

            start = nextStart;

            /*
             * Prevent overlap from moving us backwards.
             */
            if (start < 0) {
                start = 0;
            }

            numChunks++;
        }

        /*
         * Safety fallback if something remains because the
         * maximum chunk count was reached.
         */
        if (start < tokens.size() && numChunks < maxNumChunks) {

            List<Integer> remainingTokens =
                    tokens.subList(start, tokens.size());

            IntArrayList remainingTokenList =
                    new IntArrayList(remainingTokens.size());

            remainingTokens.forEach(remainingTokenList::add);

            String remainingText =
                    encoding.decode(remainingTokenList)
                            .replace(
                                    System.lineSeparator(),
                                    " "
                            )
                            .trim();

            if (remainingText.length() >
                    minChunkLengthToEmbed) {

                chunks.add(remainingText);
            }
        }

        return chunks;
    }

    private int getLastPunctuationIndex(String chunkText) {

        int maxLastPunctuation = -1;

        for (Character punctuationMark : punctuationMarks) {

            int lastPunctuation =
                    chunkText.lastIndexOf(punctuationMark);

            maxLastPunctuation =
                    Math.max(maxLastPunctuation, lastPunctuation);
        }

        return maxLastPunctuation;
    }
}