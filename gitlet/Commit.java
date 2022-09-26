package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;



public class Commit implements Serializable {
    /** COMMITID represents the SHA1-Hash ID of the commit. */
    private String commitID;

    /** COMMITMESSAGE represents the commit message. */
    private String commitMessage;

    /** ALLBLOBS represents a HashMap containing the SHA1-Hash ID of a blob,
     * to the corresponding Blob object. These represents a screenshot of
     * all the blobs at the time of the commit. */
    private HashMap<String, Blob> allBlobs;

    /** COMMITPARENT represents the parent of the commit.*/
    private String commitParent;

    /** COMMITPARENTSECOND represents the second parent of the commit.*/
    private String commitSecondParent;

    /** COMMITDATETIME represents the date and time a commit was made.*/
    private String commitDateTime;


    /** Commit constructor.
     * @param commitText
     * @param blobs
     * @param dateTime
     * @param parent
     * @param secondParent */

    public Commit(String commitText,
                  HashMap<String, Blob> blobs,
                  String dateTime,
                  String parent,
                  String secondParent) {
        commitMessage = commitText;
        allBlobs = blobs;
        commitParent = parent;
        commitDateTime = dateTime;
        commitID = generateHashID();
        commitSecondParent = secondParent;
    }

    /** @return the SHA1-Hash ID for the current commit. */
    public String getCommitHashID() {
        return commitID;
    }

    /** @return Generates a SHA1-Hash ID for the current commit. */
    public String generateHashID() {
        return Utils.sha1(commitMessage, commitDateTime);
    }

    /** @return  text message of the current commit. */
    public String getCommitMessage() {
        return commitMessage;
    }

    /** @return  the date and time of the current commit. */
    public String getCommitDateAndTime() {
        return commitDateTime;
    }

    /** @return the parent of the current commit. */
    public Commit getParent() {
        if (commitParent == null) {
            return null;
        }
        File commits = new File(".gitlet/commits");
        File parent = new File(commits + "/" + commitParent);
        Commit parentFile = Utils.readObject(parent, Commit.class);
        return parentFile;
    }

    /** @return the second parent of the current commit.
     * Need to account for this due to cross merging. */
    public Commit getSecondParent() {
        if (commitSecondParent == null) {
            return null;
        }
        File commits = new File(".gitlet/commits");
        File parent = new File(commits + "/" + commitSecondParent);
        return Utils.readObject(parent, Commit.class);
    }

    /** @return a HashMap of the all the blobs in the commit. */
    public HashMap<String, Blob> getAllBlobs() {
        if (allBlobs == null) {
            return new HashMap<String, Blob>();
        }
        return allBlobs;
    }
}
