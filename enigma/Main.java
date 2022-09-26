package enigma;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import ucb.util.CommandArgs;

import static enigma.EnigmaException.*;

/** Enigma simulator.
 *  @author Sooyeon Kim
 */
public final class Main {

    /**
     * Process a sequence of encryptions and decryptions, as
     * specified by ARGS, where 1 <= ARGS.length <= 3.
     * ARGS[0] is the name of a configuration file.
     * ARGS[1] is optional; when present, it names an input file
     * containing messages.  Otherwise, input comes from the standard
     * input.  ARGS[2] is optional; when present, it names an output
     * file for processed messages.  Otherwise, output goes to the
     * standard output. Exits normally if there are no errors in the input;
     * otherwise with code 1.
     */

    private String leftoverCycles;

    public static void main(String... args) {
        try {
            CommandArgs options =
                    new CommandArgs("--verbose --=(.*){1,3}", args);
            if (!options.ok()) {
                throw error("Usage: java enigma.Main [--verbose] "
                        + "[INPUT [OUTPUT]]");
            }

            _verbose = options.contains("--verbose");
            new Main(options.get("--")).process();
            return;
        } catch (EnigmaException excp) {
            System.err.printf("Error: %s%n", excp.getMessage());
        }
        System.exit(1);
    }

    /**
     * Open the necessary files for non-option arguments ARGS (see comment
     * on main).
     */

    Main(List<String> args) {
        _config = getInput(args.get(0));
        if (args.size() > 1) {
            _input = getInput(args.get(1));
        } else {
            _input = new Scanner(System.in);
        }

        if (args.size() > 2) {
            _output = getOutput(args.get(2));
        } else {
            _output = System.out;
        }
    }

    /**
     * Return a Scanner reading from the file named NAME.
     */
    private Scanner getInput(String name) {
        try {
            return new Scanner(new File(name));
        } catch (IOException excp) {
            throw error("could not open %s", name);
        }
    }

    /**
     * Return a PrintStream writing to the file named NAME.
     */
    private PrintStream getOutput(String name) {
        try {
            return new PrintStream(new File(name));
        } catch (IOException excp) {
            throw error("could not open %s", name);
        }
    }

    /**
     * Configure an Enigma machine from the contents of configuration
     * file _config and apply it to the messages in _input, sending the
     * results to _output.
     */
    private void process() {
        Machine enigmaMachine = readConfig();
        while (_input.hasNextLine()) {
            String inputLine = _input.nextLine();
            if (inputLine.matches("(^\\s*$)")) {
                if (inputLine.isEmpty() && !_input.hasNextLine()) {
                    break;
                } else {
                    inputLine = _input.nextLine();
                    _output.println();
                }
            }
            if (inputLine == "") {
                while (inputLine == "") {
                    if (!_input.hasNextLine()) {
                        return;
                    }
                    inputLine = _input.nextLine();
                    _output.println();
                }
            }
            if (inputLine.charAt(0) == '*') {
                setUp(enigmaMachine, inputLine);
                if (!_input.hasNextLine()) {
                    break;
                }
                inputLine = _input.nextLine();
            }
            if (inputLine == "") {
                inputLine = _input.nextLine();
                _output.println();
            }
            if (inputLine.charAt(0) != '*') {
                String convertedMessage = enigmaMachine.convert(inputLine);
                printMessageLine(convertedMessage);
            } else if (inputLine.charAt(0) != '*') {
                setUp(enigmaMachine, inputLine);
                if (!_input.hasNextLine()) {
                    break;
                }
                inputLine = _input.nextLine();
            }
        }
    }

    /**
     * Return an Enigma machine configured from the contents of configuration
     * file _config.
     */

    private Machine readConfig() {
        try {
            String firstLine = _config.nextLine();
            _alphabet = new Alphabet(firstLine);

            int numRotor = _config.nextInt();
            int numPawls = _config.nextInt();
            _config.nextLine();

            ArrayList<Rotor> allRotors = new ArrayList<Rotor>();

            while (_config.hasNextLine() || leftoverCycles != null) {
                String newLeftover = leftoverCycles;
                if (leftoverCycles != null && newLeftover.matches("(^\\s*$)")) {
                    while (leftoverCycles.matches("(^\\s*$)")) {
                        if (!_config.hasNextLine()) {
                            return new Machine(_alphabet, numRotor,
                                    numPawls, allRotors);
                        }
                        leftoverCycles = _config.nextLine();
                    }
                }
                allRotors.add(readRotor());
            }
            return new Machine(_alphabet, numRotor, numPawls, allRotors);
        } catch (NoSuchElementException excp) {
            throw error("configuration file truncated");
        }
    }

    /**
     * Return a rotor, reading its description from _config.
     */
    private Rotor readRotor() {
        try {
            if (leftoverCycles == null) {
                leftoverCycles = _config.nextLine();
            }
            Scanner rotorScanner = new Scanner(leftoverCycles);
            String rotorName = rotorScanner.next();
            String rotorType = rotorScanner.next();
            String cycles = rotorScanner.nextLine();

            if (!_config.hasNextLine()) {
                leftoverCycles = null;
            } else {
                leftoverCycles = _config.nextLine();
            }

            while (leftoverCycles != null
                    && leftoverCycles.replaceAll("\\s", "").startsWith("(")) {
                cycles += leftoverCycles;
                if (!_config.hasNextLine()) {
                    leftoverCycles = null;
                    break;
                }
                leftoverCycles = _config.nextLine();
            }

            String notches = "";
            char fixedOrRotating = rotorType.charAt(0);
            for (int i = 1; i < rotorType.length(); i++) {
                notches += rotorType.charAt(i);
            }

            Permutation perm = new Permutation(cycles, _alphabet);

            if (fixedOrRotating == 'N') {
                return new FixedRotor(rotorName, perm);
            } else if (fixedOrRotating == 'M') {
                return new MovingRotor(rotorName, perm, notches);
            } else if (fixedOrRotating == 'R') {
                return new Reflector(rotorName, perm);
            }
        } catch (NoSuchElementException excp) {
            throw error("Bad rotor description.");
        }
        return null;
    }

    /** Set M according to the specification given on SETTINGS,
     *  which must have the format specified in the assignment. */
    private void setUp(Machine M, String settings) {
        String[] rotorArray = new String[M.numRotors()];
        Scanner rotorScanner = new Scanner(settings);
        String rotorStar = rotorScanner.next();

        if (!rotorStar.equals("*")) {
            throw new
                    EnigmaException("Settings must start with *");
        }
        for (int i = 0; i < M.numRotors(); i++) {
            rotorArray[i] = rotorScanner.next();
        }
        M.insertRotors(rotorArray);
        M.setRotors(rotorScanner.next());

        if (rotorScanner.hasNext()) {
            if (rotorScanner.hasNext("[A-Z]+")) {
                String ringStellung = rotorScanner.next().trim();
                M.setExtraCredit(ringStellung);
                Permutation plugboardPerm =
                        new Permutation("", _alphabet);
                M.setPlugboard(plugboardPerm);
            } else {
                Permutation plugboardPerm =
                        new Permutation(rotorScanner.nextLine(), _alphabet);
                M.setPlugboard(plugboardPerm);
            }
        } else if (!rotorScanner.hasNext()) {
            Permutation plugboardPerm =
                    new Permutation("", _alphabet);
            M.setPlugboard(plugboardPerm);
        }
    }

    /** Return true iff verbose option specified. */
    static boolean verbose() {
        return _verbose;
    }

    /** Print MSG in groups of five (except that the last group may
     *  have fewer letters). */
    private void printMessageLine(String msg) {
        String messageLine = "";
        while (msg.length() != 0) {
            if (msg.length() < 5) {
                messageLine += msg;
                msg = "";
            } else {
                messageLine += msg.substring(0, 5);
                msg = msg.substring(5);
                if (msg.length() != 0) {
                    messageLine += " ";
                }
            }
        }
        _output.println(messageLine);
    }

    /** Alphabet used in this machine. */
    private Alphabet _alphabet;

    /** Source of input messages. */
    private Scanner _input;

    /** Source of machine configuration. */
    private Scanner _config;

    /** File for encoded/decoded messages. */
    private PrintStream _output;

    /** True if --verbose specified. */
    private static boolean _verbose;
}
