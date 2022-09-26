package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;

public class GitInfo implements Serializable {

    /** CURRENTCOMMIT represents that SHA-1 Hash
     * ID of the current commit. */
    private String currentCommit;

    /** CURRENTBRANCH represents the name
     * of the current branch. */
    private String currentBranch;

    /** ALLBRANCHES is a HashMap of all the branches. */
    private HashMap<String, String> allBranches;

    /** LinkedList of commitIDs.
     * Each commit points to its parent commit*/
    private LinkedList<String> commitList;

    /** Files in the Staging Area that need
     * to be added when commit is called. */
    private HashMap<String, Blob> stagingAdd;

    /** Files in the Staging Area that need to be
     * removed when commit is called. */
    private HashMap<String, Blob> stagingRemove;

    /** Constructor for GitInfo.
     * @param branch
     * @param commit */
    public GitInfo(String branch, String commit) {
        currentBranch = branch;
        currentCommit = commit;
        allBranches = null;
        commitList = null;
        stagingAdd = null;
        stagingRemove = null;
    }

    /** Returns the current commit ID.*/
    public String getCurrentCommit() {
        return currentCommit;
    }

    /** Returns the current branch. */
    public String getCurrentBranch() {
        return currentBranch;
    }

    /** Returns the list of all commits.*/
    public LinkedList<String> getCommitList() {
        return commitList;
    }

    /** Returns HashMap of all branch names.*/
    public HashMap<String, String> getAllBranches() {
        return allBranches;
    }

    public void setCurrentCommit(String commitID) {
        currentCommit = commitID;
    }

    public void setCurrentBranch(String branchName) {
        currentBranch = branchName;
    }

    public void setCommitList(LinkedList<String> list) {
        commitList = list;
    }

    public void setAllBranches(HashMap<String, String> branches) {
        allBranches = branches;
    }

}
