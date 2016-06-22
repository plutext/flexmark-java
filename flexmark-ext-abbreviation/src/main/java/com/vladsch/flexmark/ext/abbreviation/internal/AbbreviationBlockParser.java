package com.vladsch.flexmark.ext.abbreviation.internal;

import com.vladsch.flexmark.ext.abbreviation.AbbreviationBlock;
import com.vladsch.flexmark.ext.abbreviation.AbbreviationExtension;
import com.vladsch.flexmark.internal.util.BasedSequence;
import com.vladsch.flexmark.node.Block;
import com.vladsch.flexmark.parser.InlineParser;
import com.vladsch.flexmark.parser.block.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AbbreviationBlockParser extends AbstractBlockParser {
    private static Pattern ABBREVIATION_BLOCK = Pattern.compile("^\\*\\[\\s*.*\\s*\\]:");

    private final AbbreviationBlock block = new AbbreviationBlock();

    @Override
    public Block getBlock() {
        return block;
    }

    @Override
    public BlockContinue tryContinue(ParserState state) {
        return BlockContinue.none();
    }

    @Override
    public void addLine(ParserState state, BasedSequence line) {
        throw new IllegalStateException("Abbreviation Blocks hold a single line");
    }

    @Override
    public void closeBlock(ParserState parserState) {
        // add it to the map
        AbbreviationRepository abbreviationMap = parserState.getProperties().get(AbbreviationExtension.ABBREVIATIONS);
        abbreviationMap.put(abbreviationMap.normalizeKey(block.getText()), block);
    }

    @Override
    public void parseInlines(InlineParser inlineParser) {
        // no inlines in text or or abbreviation
    }

    @Override
    public boolean isContainer() {
        return true;
    }

    public static class Factory extends AbstractBlockParserFactory {

        @Override
        public BlockStart tryStart(ParserState state, MatchedBlockParser matchedBlockParser) {
            if (state.getIndent() >= 4) {
                return BlockStart.none();
            }

            BasedSequence line = state.getLine();
            int nextNonSpace = state.getNextNonSpaceIndex();

            BasedSequence trySequence = line.subSequence(nextNonSpace, line.length());
            Matcher matcher = ABBREVIATION_BLOCK.matcher(trySequence);
            if (matcher.find()) {
                // abbreviation definition
                int openingStart = nextNonSpace + matcher.start();
                int openingEnd = nextNonSpace + matcher.end();
                BasedSequence openingMarker = trySequence.subSequence(openingStart, openingStart + 2);
                BasedSequence text = trySequence.subSequence(openingStart + 2, openingEnd - 2).trim();
                BasedSequence closingMarker = trySequence.subSequence(openingEnd - 2, openingEnd);

                AbbreviationBlockParser abbreviationBlock = new AbbreviationBlockParser();
                abbreviationBlock.block.setOpeningMarker(openingMarker);
                abbreviationBlock.block.setText(text);
                abbreviationBlock.block.setClosingMarker(closingMarker);
                abbreviationBlock.block.setAbbreviation(trySequence.subSequence(matcher.end()).trim());
                abbreviationBlock.block.setCharsFromContent();

                return BlockStart.of(abbreviationBlock)
                        .atIndex(line.length());
            } else {
                return BlockStart.none();
            }
        }
    }
}