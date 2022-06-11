package org.dannyshih.scrabblesolver.solvers;

final class Utils {
    static void swap(StringBuilder sb, int idx0, int idx1) {
        char tmp = sb.charAt(idx0);
        sb.setCharAt(idx0, sb.charAt(idx1));
        sb.setCharAt(idx1, tmp);
    }
}
