package enigma;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collection;

import static enigma.EnigmaException.*;

/** Class that represents a complete enigma machine.
 *  @author Sooyeon Kim
 */
class Machine {

    /** A new Enigma machine with alphabet ALPHA, 1 < NUMROTORS rotor slots,
     *  and 0 <= PAWLS < NUMROTORS pawls.  ALLROTORS contains all the
     *  available rotors. */

    Machine(Alphabet alpha, int numRotors, int pawls,
            Collection<Rotor> allRotors) {
        HashMap<String, Rotor> rotorMap = new HashMap<>();
        ArrayList<Rotor> newList = new ArrayList<>(allRotors);
        ArrayList<Rotor> rotorList = new ArrayList<Rotor>();
        for (int i = 0; i < newList.size(); i++) {
            Rotor rotor = newList.get(i);
            String rotorName = rotor.name();
            rotorMap.put(rotorName, rotor);
        }
        if (numRotors <= pawls || numRotors == 0) {
            throw new EnigmaException("Number of rotor "
                    + "slots has to be strictly "
                    + "greater than then the number of pawls and cannot be 0.");
        }
        _alphabet = alpha;
        _numRotors = numRotors;
        _pawls = pawls;
        _allRotors = rotorMap;
        _rotorsArrayList = rotorList;
        _plugboard = null;
    }

    /** Return the number of rotor slots I have. */
    int numRotors() {
        return _numRotors;
    }

    /** Return the number pawls (and thus rotating rotors) I have. */
    int numPawls() {
        return _pawls;
    }

    /** Return Rotor #K, where Rotor #0 is the reflector, and Rotor
     *  #(numRotors()-1) is the fast Rotor.  Modifying this Rotor has
     *  undefined results. */
    Rotor getRotor(int k) {
        if (k == 0 && _rotorsArrayList.get(k) instanceof Reflector) {
            return _rotorsArrayList.get(k);
        }
        if ((k == 0 || k >= (_numRotors - _pawls))
                && _rotorsArrayList.get(k) instanceof FixedRotor) {
            throw new EnigmaException("Fixed rotors can "
                    + "only be at positions 2 through S-P");
        }
        if ((k < (_numRotors - _pawls) || k > _numRotors)
                && _rotorsArrayList.get(k) instanceof MovingRotor) {
            throw new EnigmaException("Moving rotors can only "
                    + "be at positions S−P+1 through S");
        }
        return _rotorsArrayList.get(k);
    }

    Alphabet alphabet() {
        return _alphabet;
    }

    /** Set my rotor slots to the rotors named ROTORS from my set of
     *  available rotors (ROTORS[0] names the reflector).
     *  Initially, all rotors are set at their 0 setting. */

    void insertRotors(String[] rotors) {
        _rotorsArrayList = new ArrayList<Rotor>();
        for (int i = 0; i < rotors.length; i++) {
            Rotor rotorFromMap = _allRotors.get(rotors[i]);
            if (!_allRotors.containsKey(rotors[i])) {
                throw new EnigmaException("No such rotor exists.");
            }
            if (_rotorsArrayList.contains(rotorFromMap)) {
                throw new EnigmaException("No duplicate rotors allowed.");
            }
            _rotorsArrayList.add(rotorFromMap);
        }
    }

    /** Set my rotors according to SETTING, which must be a string of
     *  numRotors()-1 characters in my alphabet. The first letter refers
     *  to the leftmost rotor setting (not counting the reflector).  */
    void setRotors(String setting) {
        if (setting.length() != numRotors() - 1) {
            throw new EnigmaException("String setting "
                    + "is not the correct length");
        }
        for (int i = 1; i < _numRotors; i++) {
            _rotorsArrayList.get(i).set(setting.charAt(i - 1));
        }
    }

    void setExtraCredit(String setting) {
        for (int i = 1; i < _numRotors; i++) {
            _rotorsArrayList.get(i).setRing(setting.charAt(i - 1));
        }
    }

    /** Return the current plugboard's permutation. */
    Permutation plugboard() {
        return _plugboard;
    }

    /** Set the plugboard to PLUGBOARD. */
    void setPlugboard(Permutation plugboard) {
        _plugboard = plugboard;
    }

    /** Returns the result of converting the input character C (as an
     *  index in the range 0..alphabet size - 1), after first advancing
     *  the machine. */
    int convert(int c) {
        advanceRotors();
        if (Main.verbose()) {
            System.err.printf("[");
            for (int r = 1; r < numRotors(); r += 1) {
                System.err.printf("%c",
                        alphabet().toChar(getRotor(r).setting()));
            }
            System.err.printf("] %c -> ", alphabet().toChar(c));
        }
        c = plugboard().permute(c);
        if (Main.verbose()) {
            System.err.printf("%c -> ", alphabet().toChar(c));
        }
        c = applyRotors(c);
        c = plugboard().permute(c);
        if (Main.verbose()) {
            System.err.printf("%c%n", alphabet().toChar(c));
        }
        return c;
    }

    /** Advance all rotors to their next position. */
    private void advanceRotors() {
        if (_rotorsArrayList.isEmpty()) {
            throw new EnigmaException("Rotors should not be empty.");
        }

        ArrayList<Rotor> rotatingRotors = new ArrayList<Rotor>();
        Rotor fastRotor = _rotorsArrayList.get(_rotorsArrayList.size() - 1);

        for (int i = 1; i < _rotorsArrayList.size() - 1; i++) {
            if (_rotorsArrayList.get(i + 1).atNotch()
                    && _rotorsArrayList.get(i).rotates()) {
                if (i + 1 != _rotorsArrayList.size() - 1) {
                    rotatingRotors.add(_rotorsArrayList.get(i + 1));
                }
                rotatingRotors.add(_rotorsArrayList.get(i));
            }
        }

        for (int i = 0; i < rotatingRotors.size(); i++) {
            for (int j = i + 1; j < rotatingRotors.size(); j++) {
                if (rotatingRotors.get(i) == rotatingRotors.get(j)) {
                    rotatingRotors.remove(rotatingRotors.get(j));
                }
            }
        }

        for (int i = 0; i < rotatingRotors.size(); i++) {
            rotatingRotors.get(i).advance();
        }
        fastRotor.advance();
    }

    /** Return the result of applying the rotors to the character C (as an
     *  index in the range 0..alphabet size - 1). */
    private int applyRotors(int c) {
        for (int i = _rotorsArrayList.size() - 1; i >= 0; i--) {
            c = _rotorsArrayList.get(i).convertForward(c);
        } for (int i = 1; i < _rotorsArrayList.size(); i++) {
            c = _rotorsArrayList.get(i).convertBackward(c);
        }
        return c;
    }

    /** Returns the encoding/decoding of MSG, updating the state of
     *  the rotors accordingly. */
    String convert(String msg) {
        msg = msg.replaceAll("\\s", "");

        for (int i = 0; i < msg.length(); i++) {
            if (!_alphabet.contains(msg.charAt(i))) {
                throw new EnigmaException("Character not in alphabet.");
            }
        }

        String convertedMsg = "";
        for (int i = 0; i < msg.length(); i++) {
            int alphabetInt = alphabet().toInt(msg.charAt(i));
            char alphabetChar = alphabet().toChar(convert(alphabetInt));
            convertedMsg += alphabetChar;
        }
        return convertedMsg;
    }

    /** Common alphabet of my rotors. */
    private final Alphabet _alphabet;
    /** Number of rotors. */
    private int _numRotors;
    /** Number of pawls. */
    private int _pawls;
    /** Creating hashmap. */
    private HashMap<String, Rotor> _allRotors;
    /** Creating rotor arraylist. */
    private ArrayList<Rotor> _rotorsArrayList;
    /** Creating plugboard. */
    private Permutation _plugboard;
}
