package gitlet;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

public class Merge extends Main implements Serializable {

    /** @return Any files modified in different ways in the
     * current and given branches are in conflict.
     * "Modified in different ways" can mean that the
     * contents of both are changed and different from other,
     * or the contents of one are changed and the other file
     * is deleted, or the file was absent at the split point
     * and has different contents in the given and current branches.
     * If the two commits are in conflict then INCONFLICT return true.
     * If the two commits are not in conflict then INCONFLICT returns false.
     * @param current
     * @param given
     * @param splitPoint
     * @param fileName */
    public static Boolean inConflict(Commit current, Commit given,
                                     Commit splitPoint, String fileName) {
        HashMap<String, Blob> currentBlobs = current.getAllBlobs();
        HashMap<String, Blob> givenBlobs = given.getAllBlobs();
        HashMap<String, Blob> splitBlobs = splitPoint.getAllBlobs();

        Blob currentFile = currentBlobs.get(fileName);
        Blob givenFile = givenBlobs.get(fileName);
        Blob splitPointFile = splitBlobs.get(fileName);

        if (givenFile == null
                && splitPointFile != null) {
            if (!currentFile.getBlobID()
                    .equals(splitPointFile.getBlobID())) {
                return true;
            }
        } else if (currentFile == null
                && splitPointFile != null) {
            if (!givenFile.getBlobID()
                    .equals(splitPointFile.getBlobID())) {
                return true;
            }
        } else if (currentFile != null
                && givenFile != null
                && splitPointFile != null) {
            if (!currentFile.getBlobID()
                    .equals(givenFile.getBlobID())
                    && !currentFile.getBlobID()
                    .equals(splitPointFile.getBlobID())
                    && !givenFile.getBlobID()
                    .equals(splitPointFile.getBlobID())) {
                return true;
            }
        } else if (currentFile != null && givenFile != null) {
            if (splitPointFile == null
                    && !currentFile.getBlobID()
                    .equals(givenFile.getBlobID())) {
                return true;
            }
        }
        return false;
    }

    /** @return SPLITPOINT is Helper function to determine a split point
     * when there are multiple split points.
     * HEADMERGEBRANCH represents the head of the given branch
     * and HEADCURRENTBRANCH represents the head of the current branch. */
    public static String splitPoint(Commit headMergeBranch,
                                    Commit headCurrentBranch) {
        HashMap<String, Integer> mergeBranch =
                createCommitTree(headMergeBranch,
                        new HashMap<String, Integer>(), -1);
        HashMap<String, Integer> currentBranch =
                createCommitTree(headCurrentBranch,
                        new HashMap<String, Integer>(), -1);

        ArrayList<String> potentialSplitPoints = new ArrayList<String>();
        for (String commitID : mergeBranch.keySet()) {
            if (currentBranch.containsKey(commitID)) {
                potentialSplitPoints.add(commitID);
            }
        }

        int minDistance = Integer.MAX_VALUE;
        String splitPoint = null;

        if (potentialSplitPoints.size() > 1) {
            for (String commit : potentialSplitPoints) {
                if (currentBranch.get(commit) < minDistance) {
                    minDistance = currentBranch.get(commit);
                    splitPoint = commit;
                }
            }
        } else {
            splitPoint = potentialSplitPoints.get(0);
        }
        return splitPoint;
    }

    /** @return CREATECOMMITTREE is a helper function of MERGE.
     * The method takes in a commit COMMIT and an empty hashmap DISTANCE MAP
     * and int DISTANCE is used for the recursive call. The purpose of
     * this helper function is to keep track of the distance of
     * each commit from the most recent commit in order to establish
     * a pathway of a branch. The method is recursively
     * called until the very first commit (commit when parent is null).
     * If a commit has two parent commits, then the function is called
     * on both parent commits.
     * @param commit
     * @param distance
     * @param distanceMap */
    private static HashMap<String, Integer> createCommitTree(
            Commit commit,
            HashMap<String, Integer> distanceMap,
            int distance) {
        while (commit.getParent() != null) {
            distance += 1;
            distanceMap.put(commit.getCommitHashID(), distance);
            if (commit.getSecondParent() != null) {
                Commit secondParent = commit.getSecondParent();
                createCommitTree(secondParent, distanceMap, distance);
            }
            commit = commit.getParent();
            createCommitTree(commit, distanceMap, distance);
        }
        distanceMap.put(commit.getCommitHashID(), distance + 1);
        return distanceMap;
    }

    /** @return MODIFIEDFILE returns true if the contents of FILENAME
     * in GIVEN is different from START. If they are the same,
     * MODIFIEDFILE returns false. This method is only called
     * when both commits for sure have the file. */
    public static Boolean modifiedFile(Commit start,
                                       Commit given, String fileName) {
        HashMap<String, Blob> startHash = start.getAllBlobs();
        HashMap<String, Blob> givenHash = given.getAllBlobs();

        Blob startBlob = startHash.get(fileName);
        Blob givenBlob = givenHash.get(fileName);

        if (startBlob.getFile() != givenBlob.getFile()) {
            return true;
        }
        return false;
    }

    /** @return Helper method.
     * @param first
     * @param second */
    public static byte[] concatenateByte(byte [] first, byte [] second) {
        byte[] concatenated = new byte[first.length + second.length];
        System.arraycopy(first, 0, concatenated, 0, first.length);
        System.arraycopy(second, 0, concatenated, first.length, second.length);
        return concatenated;
    }

    /** Helper method.
     * @param givenBlobs
     * @param name */
    public static void mergeConflictCurrentNull(HashMap<String,
            Blob> givenBlobs, String name) {
        byte[] givenFileContents = givenBlobs.get(name).getFile();
        byte[] head = "<<<<<<< HEAD\n".getBytes(StandardCharsets.UTF_8);
        byte[] middle = "=======\n".getBytes(StandardCharsets.UTF_8);
        byte[] last = ">>>>>>>\n".getBytes(StandardCharsets.UTF_8);

        byte[] content = Merge.concatenateByte(head, middle);
        content = Merge.concatenateByte(content, givenFileContents);
        content = Merge.concatenateByte(content, last);

        File conflicted = new File(name);
        Utils.writeContents(conflicted, content);
    }

    /** Helper method.
     * @param currentBlobs
     * @param name */
    public static void mergeConflictGivenNull(HashMap<String,
            Blob> currentBlobs, String name) {
        byte[] currentFileContents = currentBlobs.get(name).getFile();
        byte[] head = "<<<<<<< HEAD\n".getBytes(StandardCharsets.UTF_8);
        byte[] middle = "=======\n".getBytes(StandardCharsets.UTF_8);
        byte[] last = ">>>>>>>\n".getBytes(StandardCharsets.UTF_8);

        byte[] content = Merge.concatenateByte(head, currentFileContents);
        content = Merge.concatenateByte(content, middle);
        content = Merge.concatenateByte(content, last);

        File conflicted = new File(name);
        Utils.writeContents(conflicted, content);
    }

    /** Helper method.
     * @param currentBlobs
     * @param givenBlobs
     * @param name */
    public static void mergeConflictBothNull(HashMap<String, Blob> currentBlobs,
                                             HashMap<String, Blob> givenBlobs,
                                             String name) {
        byte[] currentFileContents = currentBlobs.get(name).getFile();
        byte[] givenFileContents = givenBlobs.get(name).getFile();
        byte[] head = "<<<<<<< HEAD\n".getBytes(StandardCharsets.UTF_8);
        byte[] middle = "=======\n".getBytes(StandardCharsets.UTF_8);
        byte[] last = ">>>>>>>\n".getBytes(StandardCharsets.UTF_8);

        byte[] content = Merge.concatenateByte(head, currentFileContents);
        content = Merge.concatenateByte(content, middle);
        content = Merge.concatenateByte(content, givenFileContents);
        content = Merge.concatenateByte(content, last);

        File conflicted = new File(name);
        Utils.writeContents(conflicted, content);
    }
}

