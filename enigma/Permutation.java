package enigma;

import java.util.HashMap;
import java.util.Map;

/** Represents a permutation of a range of integers starting at 0 corresponding
 *  to the characters of an alphabet.
 *  @author Sooyeon Kim
 */

class Permutation {

    /**
     * Set this Permutation to that specified by CYCLES, a string in the
     * form "(cccc) (cc) ..." where the c's are characters in ALPHABET, which
     * is interpreted as a permutation in cycle notation.  Characters in the
     * alphabet that are not included in any cycle map to themselves.
     * Whitespace is ignored.
     */

    Permutation(String cycles, Alphabet alphabet) {

        String correctCycles = cycles.trim().replaceAll(" ", "");
        if (correctCycles.length() > 0
                && correctCycles.charAt(correctCycles.length() - 1) != ')') {
            throw new EnigmaException("Needs to end in parentheses.");
        }

        _alphabet = alphabet;
        String temp = cycles.replaceAll(" ", "");
        HashMap<Character, Character> map = new HashMap<>();
        char tempFirstValue = 0;
        for (int i = 0; i < temp.length(); i++) {
            if (i == 0) {
                tempFirstValue = temp.charAt(i + 1);
            } else if (i == temp.length() - 1) {
                map.put(temp.charAt(i - 1), tempFirstValue);
            } else if (temp.charAt(i) == '('
                    && temp.charAt(i + 1) == ')') {
                map.put(temp.charAt(i), temp.charAt(i));
            } else if (temp.charAt(i) == '(') {
                tempFirstValue = temp.charAt(i + 1);
            } else if (temp.charAt(i) == ')') {
                map.put(temp.charAt(i - 1), tempFirstValue);
            } else if (temp.charAt(i - 1) != '('
                    || temp.charAt(i + 1) != ')') {
                map.put(temp.charAt(i), temp.charAt(i + 1));
            }
        }
        _cycles = map;
        for (int i = 0; i < _alphabet.size(); i++) {
            if (!_cycles.containsKey(_alphabet.toChar(i))) {
                _cycles.put(_alphabet.toChar(i), _alphabet.toChar(i));
            }
        }
    }

    public Character getKey(Map<Character, Character> map, Character value) {
        for (Map.Entry<Character, Character> entry : map.entrySet()) {
            if (value == (entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** Add the cycle c0->c1->...->cm->c0 to the permutation, where CYCLE is
     *  c0c1...cm. */
    private void addCycle(String cycle) {
        for (int i = 0; i < cycle.length(); i++) {
            char tempFirstValue = 0;
            if (i == 0) {
                tempFirstValue = cycle.charAt(i);
            } else if (i == cycle.length() - 1) {
                _cycles.put(cycle.charAt(i), tempFirstValue);
            } else {
                _cycles.put(cycle.charAt(i), cycle.charAt(i + 1));
            }
        }
    }

    /** Return the value of P modulo the size of this permutation. */
    final int wrap(int p) {
        int r = p % size();
        if (r < 0) {
            r += size();
        }
        return r;
    }

    /** Returns the size of the alphabet I permute. */
    int size() {
        return _alphabet.size();
    }

    /** Return the result of applying this permutation to P modulo the
     *  alphabet size. */
    int permute(int p) {
        int moduloP = wrap(p);
        char key = _alphabet.toChar(moduloP);
        char newLetter = _cycles.get(key);
        return _alphabet.toInt(newLetter);
    }

    /** Return the result of applying the inverse of this permutation
     *  to C modulo the alphabet size. */
    int invert(int c) {
        int moduloP = wrap(c);
        char key = _alphabet.toChar(moduloP);
        char newLetter = getKey(_cycles, key);
        return _alphabet.toInt(newLetter);
    }

    /** Return the result of applying this permutation to the index of P
     *  in ALPHABET, and converting the result to a character of ALPHABET. */
    char permute(char p) {
        int wrapped = _alphabet.toInt(p);
        int wrappedIndex = permute(wrapped);
        char newCharacter = _alphabet.toChar(wrappedIndex);
        return newCharacter;
    }

    /** Return the result of applying the inverse of this permutation to C. */
    char invert(char c) {
        char newValueMap = (char) getKey(_cycles, c);
        return newValueMap;
    }

    /** Return the alphabet used to initialize this Permutation. */
    Alphabet alphabet() {
        return _alphabet;
    }

    /** Return true iff this permutation is a derangement (i.e., a
     *  permutation for which no value maps to itself). */
    boolean derangement() {
        for (int i = 0; i < _alphabet.size(); i++) {
            char key = _alphabet.toChar(i);
            char mapValue = (char) _cycles.get(key);
            if (key == mapValue)  {
                return false;
            }
        }
        return true;
    }

    /** Alphabet of this permutation. */
    private Alphabet _alphabet;
    /** Creating hashmap of cycles. */
    private HashMap<Character, Character> _cycles;
}
