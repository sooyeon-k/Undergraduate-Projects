package enigma;

/** Class that represents a rotating rotor in the enigma machine.
 *  @author Sooyeon Kim
 */
class MovingRotor extends Rotor {

    /**
     * A rotor named NAME whose permutation in its default setting is
     * PERM, and whose notches are at the positions indicated in NOTCHES.
     * The Rotor is initially in its 0 setting (first character of its
     * alphabet).
     */

    MovingRotor(String name, Permutation perm, String notches) {
        super(name, perm);
        _notches = notches;
        set(setting());
    }

    @Override
    boolean rotates() {
        return true;
    }

    @Override
    void advance() {
        set(setting() + 1);
    }

    @Override
    String notches() {
        return _notches;
    }

    @Override
    boolean atNotch() {
        for (int i = 0; i < _notches.length(); i++) {
            if (alphabet().toChar(setting()) == _notches.charAt(i)) {
                return true;
            }
        }
        return false;
    }

    /** Creating notches. */
    private String _notches;
}
