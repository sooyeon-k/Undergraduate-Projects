package enigma;

/** An alphabet of encodable characters.  Provides a mapping from characters
 *  to and from indices into the alphabet.
 *  @author Sooyeon Kim
 */
class Alphabet {

    /** A new alphabet containing CHARS. The K-th character has index
     *  K (numbering from 0). No character may be duplicated. */

    Alphabet(String chars) {
        String tempChars = chars;
        String ch = "";
        if (tempChars.length() == 0) {
            throw new EnigmaException("Chars cannot be null.");
        } else {
            for (int i = 0; i < tempChars.length(); i++) {
                _chars = chars;
                String character = String.valueOf(_chars.charAt(i));
                if (ch.contains(character)) {
                    throw new EnigmaException("No duplicates in Alphabet.");
                }
                if (character.equals("(")
                        || character.equals(")")
                        || character.equals("*")) {
                    throw new EnigmaException("Illegal characters.");
                } else {
                    ch += _chars.charAt(i);
                }
            }
        }
        _chars = ch;
    }

    /** A default alphabet of all upper-case characters. */
    Alphabet() {
        this("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    }

    /** Returns the size of the alphabet. */
    int size() {
        return _chars.length();
    }

    /** Returns true if CH is in this alphabet. */
    boolean contains(char ch) {
        String chString = String.valueOf(ch);
        return _chars.contains(chString);
    }

    /** Returns character number INDEX in the alphabet, where
     *  0 <= INDEX < size(). */
    char toChar(int index) {
        if (0 <= index && index < _chars.length()) {
            return _chars.charAt(index);
        } else {
            throw new EnigmaException("INDEX is out of bounds.");
        }
    }

    /** Returns the index of character CH which must be in
     *  the alphabet. This is the inverse of toChar(). */
    int toInt(char ch) {
        return _chars.indexOf(ch);
    }

    /** Creating string. */
    private String _chars;

}
