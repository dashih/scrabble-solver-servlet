package org.dannyshih.scrabblesolver.solvers;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * A Trie data structure that supports only methods needed for this application.
 *
 * @author dshih
 */
final class Trie {
    private final Node m_root;

    Trie() {
        m_root = new Node();
    }

    void addWord(String s) {
        Preconditions.checkArgument(StringUtils.isNotBlank(s));
        Node current = m_root;
        for(int i = 0; i < s.length(); i++) {
            current.children.putIfAbsent(s.charAt(i), new Node());
            current = current.children.get(s.charAt(i));
        }

        current.isWord = true;
    }

    /**
     * Is the string a prefix to a valid word? "ATTR" should return true, because "ATTRIBUTE" is a word.
     */
    boolean beginsWord(String s) {
        return findFinalNode(s) != null;
    }

    boolean isWord(String s) {
        final Node finalNode = findFinalNode(s);
        return finalNode != null && finalNode.isWord;
    }

    private Node findFinalNode(String s) {
        Preconditions.checkNotNull(s);
        Node current = m_root;
        for (int i = 0; i < s.length(); i++) {
            if (current.children.containsKey(s.charAt(i))) {
                current = current.children.get(s.charAt(i));
            } else {
                return null;
            }
        }

        return current;
    }

    private static final class Node {
        Map<Character, Node> children;
        boolean isWord;

        Node() {
            children = new HashMap<>();
        }
    }
}
