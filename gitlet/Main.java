package gitlet;


import java.io.IOException;
import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.nio.file.FileSystems;
import java.util.List;
import java.util.Date;

import static gitlet.Utils.*;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Sooyeon Kim
 */
public class Main implements Serializable {

    /** HEADPOINTER is an SHA1 Hash ID of a commit and
     * represents the current commit.  */
    private static String headPointer;

    /** CURRENTBRANCH represents the name of the current
     * working branch. */
    private static String currentBranch;

    /** ALLBRANCHES is a HashMap of all the branches.*/
    private static HashMap<String, String> allBranches;

    /** COMMITLIST is a LinkedList of commitIDs.
     * Each commit points to its parent commit*/
    private static LinkedList<String> commitList;

    /** BLOB File. */
    private static File blobs;

    /** COMMITS File. */
    private static File commits;

    /** BRANCHES File. */
    private static File branches;

    /** STAGING File. */
    private static File staging;

    /** GITINFO File. */
    private static File gitInfo;

    /** GITINFO Class. */
    private static GitInfo info;

    /** STAGINGREMOVE File. */
    private static File stagingRemove;

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {
        try {
            if (args.length == 0) {
                System.out.println();
            }
            switch (args[0]) {
            case "init":
                init();
                break;
            case "add":
                loadIn();
                add(args[1]);
                break;
            case "log":
                log();
                break;
            case "rm":
                loadIn();
                rm(args[1]);
                break;
            case "commit":
                loadIn();
                commit(args[1]);
                break;
            case "checkout":
                loadIn();
                checkout(args);
                break;
            case "global-log":
                globalLog();
                break;
            case "status":
                status();
                break;
            case "find":
                loadIn();
                find(args[1]);
                break;
            case "branch":
                branch(args[1]);
                break;
            case "merge":
                loadIn();
                merge(args[1]);
                break;
            case "rm-branch":
                loadIn();
                branchRM(args[1]);
                break;
            case "reset":
                loadIn();
                reset(args[1]);
                break;
            default:
                throw new GitletException("No command with that name exists.");
            }
        } catch (GitletException e) {
            System.out.println(e.getMessage());
        }
    }

    /** INIT initializes a new gitlet directory.
     * INIT should only ever be called once. */
    public static void init() throws IOException {
        File gitletFile = new File(".gitlet");
        if (gitletFile.exists()) {
            throw new GitletException("A Gitlet version-control "
                    + "system already exists in the current directory.");
        } else {
            blobs = new File(".gitlet/blobs");
            commits = new File(".gitlet/commits");
            branches = new File(".gitlet/branches");
            staging = new File(".gitlet/staging");
            stagingRemove = new File(".gitlet/stagingRemove");
            gitInfo = new File(".gitlet/gitInfo");

            gitletFile.mkdir();
            blobs.mkdir();
            commits.mkdir();
            branches.mkdir();
            staging.mkdir();
            stagingRemove.mkdir();

            allBranches = new HashMap<String, String>();
            commitList = new LinkedList<String>();

            String initialDateTime = "Thu Jan 1 00:00:00 1970 -0800";
            Commit initialCommit = new Commit("initial commit",
                    new HashMap<>(), initialDateTime,
                    null, null);
            String initialCommitID = initialCommit.getCommitHashID();
            commitList.add(initialCommitID);

            File initialCommitFile = new File(commits + "/"
                    + initialCommit.getCommitHashID());
            initialCommitFile.createNewFile();
            Utils.writeObject(initialCommitFile, initialCommit);

            currentBranch = "master";
            allBranches.put(currentBranch, initialCommitID);
            info = new GitInfo("master", initialCommitID);
            gitInfo.createNewFile();

            Utils.writeObject(gitInfo, info);
            headPointer = initialCommitID;
        }
        loadOut();
    }

    /** LOADIN is a helper function that loads
     * everything on from disk when Gitlet starts up. */
    public static void loadIn() {
        blobs = new File(".gitlet/blobs");
        commits = new File(".gitlet/commits");
        branches = new File(".gitlet/branches");
        staging = new File(".gitlet/staging");
        stagingRemove = new File(".gitlet/stagingRemove");
        gitInfo = new File(".gitlet/gitInfo");
        info = readObject(gitInfo, GitInfo.class);

        currentBranch = info.getCurrentBranch();
        headPointer = info.getCurrentCommit();
        commitList = info.getCommitList();
        allBranches = info.getAllBranches();
    }

    /** LOADOUT is a helper function to save
     * everything to disk when Gitlet terminates. */
    public static void loadOut() {
        info.setCurrentBranch(currentBranch);
        info.setCurrentCommit(headPointer);
        info.setCommitList(commitList);
        info.setAllBranches(allBranches);
        writeObject(gitInfo, info);
    }

    /** ADD method. ADD adds a file to the staging area.
     * ADDNEWFILE represents the name of the file.
     * @param addNewFile */
    public static void add(String addNewFile) throws IOException {
        File addFile = new File(addNewFile);
        if (!addFile.exists()) {
            throw new GitletException("File does not exist.");
        }
        Blob newFile = new Blob(addFile);
        File stagingFile =
                new File(staging + "/" + newFile.getFileName());
        File removingArea = new
                File(stagingRemove + "/" + newFile.getFileName());
        if (removingArea.exists()) {
            removingArea.delete();
        } else {
            File path = Utils.join(".gitlet/staging", addNewFile);
            if (path.exists()) {
                Utils.writeContents(path, newFile.getFile());
            } else {
                File currentCommitFile =
                        new File(commits + "/" + headPointer);
                Commit currentCommit =
                        readObject(currentCommitFile, Commit.class);
                HashMap<String, Blob> lastBlobs = currentCommit.getAllBlobs();
                if (lastBlobs.containsKey(addNewFile)) {
                    Blob lastBlobCommit = (Blob) lastBlobs.get(addNewFile);
                    if (Arrays.equals(lastBlobCommit
                            .getFile(), newFile.getFile())) {
                        if (new File(staging + "/" + addNewFile).exists()) {
                            Utils.restrictedDelete(staging + "/" + addNewFile);
                        }
                    } else {
                        stagingFile.createNewFile();
                        Utils.writeContents(stagingFile, newFile.getFile());
                    }
                } else {
                    stagingFile.createNewFile();
                    Utils.writeContents(stagingFile, newFile.getFile());
                }
            }
        }
        loadOut();
    }

    /** RM unstages the file REMOVEDNEWFILE if it is
     * currently staged for addition.
     * If the file is tracked in the current commit, stage it for removal
     * and remove the file from the working directory.
     * Do not remove it unless it is tracked in the current commit.
     * @param removeNewFile */
    public static void rm(String removeNewFile) throws IOException {
        File fileExists = new File(removeNewFile);
        if (!fileExists.exists()) {
            File stagingRemoveFile =
                    new File(stagingRemove + "/" + removeNewFile);
            stagingRemoveFile.createNewFile();
            return;
        }
        File pathFile = new File(".gitlet/staging" + "/" + removeNewFile);
        boolean fileStaged = pathFile.exists();
        boolean fileTracked = false;
        File currentCommitFile = new File(commits + "/" + headPointer);
        Commit currentCommit = readObject(currentCommitFile, Commit.class);
        HashMap<String, Blob> blobMap = currentCommit.getAllBlobs();
        Blob fileRemove = null;
        for (Blob blob : blobMap.values()) {
            if (blob.getFileName().equals(removeNewFile)) {
                fileTracked = true;
                fileRemove = blob;
            }
        }
        if (fileStaged) {
            pathFile.delete();
        } else if (fileTracked) {
            if (fileStaged) {
                pathFile.delete();
            }
            File stagingRemoveFile = new File(stagingRemove
                    + "/" + fileRemove.getFileName());
            stagingRemoveFile.createNewFile();
            File path = new File(removeNewFile);
            Blob blobNew = new Blob(path);
            Utils.writeContents(stagingRemoveFile, blobNew.getFile());
            Utils.restrictedDelete(path);
        } else if (!fileTracked && !fileStaged) {
            throw new GitletException("No reason to remove the file.");
        }
        loadOut();
    }

    /** COMMIT method. COMMIT adds files to the COMMITS
     * File with the message COMMITMESSAGE.
     * Saves all the files from the staging area and parent commit.
     * Keeps the versions of files exactly as
     * they are, contents are not updated.
     * After a commit, the staging area is cleared.
     * @param commitMessage */
    public static void commit(String commitMessage) throws IOException {
        List<String> add = Utils.plainFilenamesIn(staging);
        List<String> remove = Utils.plainFilenamesIn(stagingRemove);
        if (commitMessage.isEmpty()) {
            throw new GitletException("Please enter a commit message.");
        } else if (add.size() == 0 && remove.size() == 0) {
            throw new GitletException("No changes added to the commit.");
        } else {
            File parentCommitFile =
                    new File(commits + "/" + headPointer);
            Commit parentCommit =
                    readObject(parentCommitFile, Commit.class);
            HashMap<String, Blob> parentBlobs =
                    parentCommit.getAllBlobs();

            for (String file : plainFilenamesIn(staging)) {
                File stagingFile = new File(staging + "/" + file);
                Blob blob = new Blob(stagingFile);
                parentBlobs.put(file, blob);
            }
            for (String file : plainFilenamesIn(stagingRemove)) {
                if (parentBlobs.keySet().contains(file)) {
                    parentBlobs.remove(file);
                }
            }
            Date commitDateTime = new Date();
            String pattern = "EEE MMM d HH:mm:ss yyyy Z";
            SimpleDateFormat date = new SimpleDateFormat(pattern);
            String dateTime = date.format(new Date());

            Commit newCommit = new Commit(commitMessage, parentBlobs,
                    dateTime, headPointer, null);
            String commitHash = newCommit.getCommitHashID();
            File commitFile = new File(commits + "/" + commitHash);
            byte[] commitSer = Utils.serialize(newCommit);
            commitFile.createNewFile();
            Utils.writeContents(commitFile, commitSer);
            LinkedList<String> commitTemp = new LinkedList<>();
            commitTemp.add(newCommit.getCommitHashID());
            commitTemp.addAll(commitList);
            commitList = commitTemp;
            headPointer = commitHash;
            allBranches.put(currentBranch, headPointer);
            for (File file: staging.listFiles()) {
                if (!file.isDirectory()) {
                    file.delete();
                }
            }
            for (File file: stagingRemove.listFiles()) {
                if (!file.isDirectory()) {
                    file.delete();
                }
            }
        }
        loadOut();
    }

    /** Method for LOG(). */
    public static void log() {
        loadIn();
        File headCommitFile = new File(commits + "/" + headPointer);
        Commit headCommit = readObject(headCommitFile, Commit.class);
        while (!headCommit.getCommitMessage().equals("initial commit")) {
            System.out.println("===");
            System.out.println("commit "
                    + headCommit.getCommitHashID());
            System.out.println("Date: "
                    + headCommit.getCommitDateAndTime());
            System.out.println(headCommit.getCommitMessage());
            System.out.println();
            headCommit = headCommit.getParent();
        }
        System.out.println("===");
        System.out.println("commit "
                + headCommit.getCommitHashID());
        System.out.println("Date: "
                + headCommit.getCommitDateAndTime());
        System.out.println(headCommit.getCommitMessage());
        System.out.println();
        loadOut();
    }

    /** Method for GLOBALLOG(). */
    public static void globalLog() {
        loadIn();
        for (String fileName : Utils.plainFilenamesIn(commits)) {
            File commitFile = new File(commits + "/" + fileName);
            Commit commit = readObject(commitFile, Commit.class);
            System.out.println("===");
            System.out.println("commit " + commit.getCommitHashID());
            System.out.println("Date: " + commit.getCommitDateAndTime());
            System.out.println(commit.getCommitMessage());
            System.out.println();
        }
        loadOut();
    }

    /** @param args */
    public static void checkout(String[] args) throws IOException {
        if (args[1].equals("--")) {
            String fileName = args[2];
            checkoutHelper(fileName);
        } else if (args.length == 2) {
            String branchName = args[1];
            if (!allBranches.containsKey(branchName)) {
                throw new GitletException("No such branch exists.");
            } else if (branchName.equals(currentBranch)) {
                throw new GitletException("No need to "
                        + "checkout the current branch.");
            }
            Commit currentCommit = readObject(Utils.
                    join(commits, headPointer), Commit.class);
            HashMap<String, Blob> blobMap = currentCommit.getAllBlobs();
            File branch = new File(commits + "/" + allBranches.get(branchName));
            Commit branchCommit = Utils.readObject(branch, Commit.class);
            HashMap<String, Blob> branchMap = branchCommit.getAllBlobs();
            File workingDirectory = new File(FileSystems.getDefault()
                    .getPath("").toAbsolutePath().toString());
            List<String> filesCWD = Utils.plainFilenamesIn(workingDirectory);
            for (String file : filesCWD) {
                if (!blobMap.containsKey(file)
                        && branchMap.containsKey(file)) {
                    throw new GitletException("There is an "
                            + "untracked file in the way; "
                            + "delete it, or add and commit it first.");
                }
            }
            String headCommit = allBranches.get(args[1]);
            File getCommitFile = new File(commits + "/" + headCommit);
            Commit commit = readObject(getCommitFile, Commit.class);
            for (String fileName : filesCWD) {
                File fileCWD = new File(fileName);
                fileCWD.delete();
            }
            for (Blob blob : commit.getAllBlobs().values()) {
                File newFile = new File(blob.getFileName());
                newFile.createNewFile();
                Utils.writeContents(newFile, blob.getFile());
            }
            currentBranch = args[1];
            headPointer = allBranches.get(currentBranch);
            for (File file: staging.listFiles()) {
                if (!file.isDirectory()) {
                    file.delete();
                }
            }
        } else if (args[2].equals("--")) {
            checkoutIDHelper(args);
        } else {
            throw new GitletException("Incorrect operands.");
        }
        loadOut();
    }

    public static void checkoutIDHelper(String[] args) {
        String commitID = args[1];
        List<String> allFiles = Utils.plainFilenamesIn(commits);
        commitID = getFullCommit(commitID);

        if (!allFiles.contains(commitID)) {
            throw new GitletException("No commit with that id exists.");
        }
        File commitFile = new File(commits + "/" + commitID);
        Commit newCommit = readObject(commitFile, Commit.class);
        HashMap<String, Blob> commitBlobs = newCommit.getAllBlobs();
        if (!commitBlobs.containsKey(args[3])) {
            throw new GitletException("File does "
                    + "not exist in that commit.");
        }
        String fileName = args[3];
        File getCommitFile = new File(commits + "/" + commitID);
        Commit commit = readObject(getCommitFile, Commit.class);
        HashMap<String, Blob> allBlobs = commit.getAllBlobs();
        for (String name : allBlobs.keySet()) {
            if (name.equals(fileName)) {
                File newFile = new File(fileName);
                Utils.writeContents(newFile, allBlobs.get(name).getFile());
            }
        }
    }

    /** Checkout Helper.
     * @param fileName */
    public static void checkoutHelper(String fileName) {
        File headCommitFile = new File(commits + "/" + headPointer);
        Commit headCommit = readObject(headCommitFile, Commit.class);
        HashMap<String, Blob> allBlobs = headCommit.getAllBlobs();
        ArrayList<String> allFileNames = new ArrayList<String>();
        for (String name : allBlobs.keySet()) {
            if (name.equals(fileName)) {
                File newFile = new File(fileName);
                allFileNames.add(fileName);
                Utils.writeContents(newFile, allBlobs.get(name).getFile());
            }
        }
        if (allFileNames.isEmpty()) {
            throw new GitletException("File does not exist in that commit.");
        }
    }

    /** Find Method.
     * @param commitMessage */
    public static void find(String commitMessage) {
        ArrayList<String> commitIDs = new ArrayList<>();
        for (String fileName : Utils.plainFilenamesIn(commits)) {
            File commitFile = new File(commits + "/" + fileName);
            Commit commit = readObject(commitFile, Commit.class);
            if (commit.getCommitMessage().equals(commitMessage)) {
                commitIDs.add(commit.getCommitHashID());
                System.out.println(commit.getCommitHashID());
            }
        }
        if (commitIDs.isEmpty()) {
            throw new GitletException("Found no commit with that message.");
        }
        loadOut();
    }

    /** Status Method.*/
    public static void status() {
        loadIn();
        System.out.println("=== Branches ===");
        System.out.println("*" + currentBranch);
        for (String branch : allBranches.keySet()) {
            if (!branch.equals(currentBranch)) {
                System.out.println(branch);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        for (String fileName : plainFilenamesIn(staging)) {
            System.out.println(fileName);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String fileName : plainFilenamesIn(stagingRemove)) {
            System.out.println(fileName);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        loadOut();
    }

    /** Branch method.
     * @param branchName */
    public static void branch(String branchName) {
        loadIn();
        if (allBranches.containsKey(branchName)) {
            throw new GitletException("A branch "
                    + "with that name already exists.");
        }
        allBranches.put(branchName, headPointer);
        loadOut();
    }

    /** Branch RM method.
     * @param branchName */
    public static void branchRM(String branchName) {
        if (!allBranches.containsKey(branchName)) {
            throw new GitletException("A branch "
                    + "with that name does not exist.");
        }
        if (branchName.equals(currentBranch)) {
            throw new GitletException("Cannot remove the current branch.");
        }
        allBranches.remove(branchName);
        loadOut();
    }

    /** Merge method.
     * @param branchName */
    public static void merge(String branchName) throws IOException {
        List<String> add = Utils.plainFilenamesIn(staging);
        List<String> remove = Utils.plainFilenamesIn(stagingRemove);
        if (add.size() != 0 || remove.size() != 0) {
            throw new GitletException("You have uncommitted changes.");
        } else if (!allBranches.containsKey(branchName)) {
            throw new GitletException("A branch with "
                    + "that name does not exist.");
        } else if (branchName.equals(currentBranch)) {
            throw new GitletException("Cannot merge a branch with itself.");
        }
        File current = new File(commits + "/" + headPointer);
        File given = new File(commits + "/" + allBranches.get(branchName));
        Commit currentPointer = Utils.readObject(current, Commit.class);
        Commit givenPointer = Utils.readObject(given, Commit.class);
        HashMap<String, Blob> blobMap = currentPointer.getAllBlobs();
        File workingDirectory =
                new File(FileSystems.getDefault().getPath("")
                        .toAbsolutePath().toString());
        List<String> filesCWD = Utils.plainFilenamesIn(workingDirectory);
        for (String file : filesCWD) {
            if (!blobMap.containsKey(file)) {
                throw new GitletException("There is an "
                        + "untracked file in the way; "
                        + "delete it, or add and commit it first.");
            }
        }
        String splitPt = Merge.splitPoint(givenPointer, currentPointer);
        File split = new File(commits + "/" + splitPt);
        Commit splitPoint = readObject(split, Commit.class);
        HashMap<String, Blob> splitPointMap = splitPoint.getAllBlobs();
        HashMap<String, Blob> currentBlobs =
                currentPointer.getAllBlobs();
        HashMap<String, Blob> givenBlobs =
                givenPointer.getAllBlobs();
        if (splitPoint.getCommitHashID()
                .equals(givenPointer.getCommitHashID())) {
            throw new GitletException("Given branch is "
                    + "an ancestor of the current branch.");
        }
        if (splitPoint.getCommitHashID()
                .equals(currentPointer.getCommitHashID())) {
            checkout(new String[]{"checkout", branchName});
            throw new GitletException("Current branch fast-forwarded.");
        }
        mergeHelper(givenBlobs, currentBlobs, splitPointMap,
                givenPointer, splitPoint, currentPointer);
        for (String name : splitPointMap.keySet()) {
            if (currentBlobs.containsKey(name)) {
                if (splitPointMap.get(name).getBlobID()
                        .equals(currentBlobs.get(name).getBlobID())
                        && !givenBlobs.containsKey(name)) {
                    rm(name);
                }
            }
        }
        mergeSecondHelper(currentBlobs, givenBlobs,
                currentPointer, givenPointer,
                splitPoint, branchName);
    }

    public static void mergeHelper(HashMap<String, Blob> givenBlobs,
                                   HashMap<String, Blob> currentBlobs,
                                   HashMap<String, Blob> splitPointMap,
                                   Commit givenPointer,
                                   Commit splitPoint,
                                   Commit currentPointer) throws IOException {
        for (String name: givenBlobs.keySet()) {
            if (currentBlobs.containsKey(name)
                    && givenBlobs.containsKey(name)) {
                if (Merge.modifiedFile(givenPointer, splitPoint, name)
                        && !Merge.modifiedFile(currentPointer,
                        splitPoint, name)) {
                    File fileNew = new File(name);
                    if (!fileNew.exists()) {
                        fileNew.createNewFile();
                        writeContents(fileNew,
                                givenBlobs.get(name).getFile());
                    } else {
                        writeContents(fileNew,
                                givenBlobs.get(name).getFile());
                    }
                    add(name);
                }
            }
            if (!splitPointMap.containsKey(name)) {
                headPointer = givenPointer.getCommitHashID();
                checkout(new String[]{"add", "--", name});
                headPointer = currentPointer.getCommitHashID();
                add(name);
            }
            if (Merge.inConflict(currentPointer,
                    givenPointer, splitPoint, name)) {
                System.out.println("Encountered a merge conflict.");
                if (currentBlobs.get(name) == null) {
                    Merge.mergeConflictCurrentNull(givenBlobs, name);
                } else if (givenBlobs.get(name) == null) {
                    Merge.mergeConflictGivenNull(currentBlobs, name);
                } else if (currentBlobs.get(name) != null
                        && givenBlobs.get(name) != null) {
                    Merge.mergeConflictBothNull(currentBlobs,
                            givenBlobs, name);
                }
            }
        }
    }

    public static void mergeSecondHelper(HashMap<String, Blob> currentBlobs,
                                         HashMap<String, Blob> givenBlobs,
                                         Commit currentPointer,
                                         Commit givenPointer,
                                         Commit splitPoint,
                                         String branchName) throws IOException {
        ArrayList<String> tempList = new ArrayList<String>();
        for (String string : currentBlobs.keySet()) {
            if (!givenBlobs.keySet().contains(string)) {
                tempList.add(string);
            }
        }
        for (String string : currentBlobs.keySet()) {
            if (!tempList.contains(string)
                    && !givenBlobs.keySet().contains(string)) {
                tempList.add(string);
            }
        }
        for (String name: tempList) {
            if (Merge.inConflict(currentPointer,
                    givenPointer, splitPoint, name)) {
                System.out.println("Encountered a merge conflict.");
                if (currentBlobs.get(name) == null) {
                    Merge.mergeConflictCurrentNull(givenBlobs, name);
                } else if (givenBlobs.get(name)
                        == null) {
                    Merge.mergeConflictGivenNull(currentBlobs, name);
                } else if (currentBlobs.get(name) != null
                        && givenBlobs.get(name) != null) {
                    Merge.mergeConflictBothNull(currentBlobs,
                            givenBlobs, name);
                }
            }
        }
        mergeCommit("Merged " + branchName
                + " into " + currentBranch + ".", allBranches.get(branchName));
        loadOut();
    }

    public static void mergeCommit(String commitMessage,
                                   String secondParent) throws IOException {
        List<String> add = Utils.plainFilenamesIn(staging);
        List<String> remove = Utils.plainFilenamesIn(stagingRemove);
        if (commitMessage.isEmpty()) {
            throw new GitletException("Please enter a commit message.");
        } else if (add.size() == 0 && remove.size() == 0) {
            throw new GitletException("No changes added to the commit.");
        } else {
            File parentCommitFile = new File(commits + "/" + headPointer);
            Commit parentCommit = readObject(parentCommitFile, Commit.class);
            HashMap<String, Blob> parentBlobs = parentCommit.getAllBlobs();
            for (String file : plainFilenamesIn(staging)) {
                File stagingFile = new File(staging + "/" + file);
                Blob blob = new Blob(stagingFile);
                parentBlobs.put(file, blob);
            }
            for (String file : plainFilenamesIn(stagingRemove)) {
                if (parentBlobs.keySet().contains(file)) {
                    parentBlobs.remove(file);
                }
            }
            Date commitDateTime = new Date();
            String pattern = "EEE MMM d HH:mm:ss yyyy Z";
            SimpleDateFormat date = new SimpleDateFormat(pattern);
            String dateTime = date.format(new Date());
            Commit newCommit = new Commit(commitMessage, parentBlobs,
                    dateTime, headPointer, secondParent);
            String commitHash = newCommit.getCommitHashID();
            File commitFile = new File(commits + "/" + commitHash);
            byte[] commitSer = Utils.serialize(newCommit);
            commitFile.createNewFile();
            Utils.writeContents(commitFile, commitSer);
            LinkedList<String> commitTemp = new LinkedList<>();
            commitTemp.add(newCommit.getCommitHashID());
            commitTemp.addAll(commitList);
            commitList = commitTemp;
            headPointer = commitHash;
            allBranches.put(currentBranch, headPointer);
            for (File file: staging.listFiles()) {
                if (!file.isDirectory()) {
                    file.delete();
                }
            }
            for (File file: stagingRemove.listFiles()) {
                if (!file.isDirectory()) {
                    file.delete();
                }
            }
        }
    }

    /** Checks out all the files tracked by the given commit.
     * Removes tracked files that are not present in that
     * commit. Also moves the current branch's head to that commit node.
     * See the intro for an example of
     * what happens to the head pointer after using reset. The [commit id]
     * may be abbreviated as for checkout.
     * The staging area is cleared. The command is essentially checkout of
     * an arbitrary commit that also
     * changes the current branch head.
     * @param commitID*/
    public static void reset(String commitID) throws IOException {
        commitID = getFullCommit(commitID);

        Boolean commitExists = false;
        for (String string : Utils.plainFilenamesIn(commits)) {
            if (string.equals(commitID)) {
                commitExists = true;
                break;
            }
        }

        if (!commitExists) {
            throw new GitletException("No commit with that id exists.");
        }

        File reset = new File(commits + "/" + commitID);
        File current = new File(commits + "/" + headPointer);
        Commit resetCommit = Utils.readObject(reset, Commit.class);
        Commit currentCommit = Utils.readObject(current, Commit.class);

        HashMap<String, Blob> resetBlobs = resetCommit.getAllBlobs();
        HashMap<String, Blob> currentBlobs = currentCommit.getAllBlobs();

        File workingDirectory = new File(FileSystems.getDefault()
                .getPath("").toAbsolutePath().toString());
        List<String> filesCWD = Utils.plainFilenamesIn(workingDirectory);

        for (String file : filesCWD) {
            if (!currentBlobs.containsKey(file)
                    && resetBlobs.containsKey(file)) {
                throw new GitletException(
                        "There is an untracked file in the way;"
                        + " delete it, or add and commit it first.");
            }
        }

        for (String string : resetBlobs.keySet()) {
            checkout(new String[]{"checkout", commitID, "--", string});
        }

        for (String string : filesCWD) {
            if (currentBlobs.containsKey(string)
                    && !resetBlobs.containsKey(string)) {
                File remove = new File(string);
                if (remove.exists()) {
                    remove.delete();
                }
            }
        }

        for (File file: staging.listFiles()) {
            if (!file.isDirectory()) {
                file.delete();
            }
        }

        headPointer = commitID;
        allBranches.put(currentBranch, headPointer);
        loadOut();
    }

    public static String getFullCommit(String commitID) {
        if (commitID.length() < UID_LENGTH) {
            List<String> allFiles = Utils.plainFilenamesIn(commits);

            for (String commit : allFiles) {
                if (commit.startsWith(commitID)) {
                    commitID = commit;
                }
            }
        }
        return commitID;
    }
}


