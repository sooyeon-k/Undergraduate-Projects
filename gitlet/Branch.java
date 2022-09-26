package gitlet;

import java.io.Serializable;

public class Branch implements Serializable {

    /** BRANCHHEAD. */
    private String branchHead;
    /** BRANCHHEAD. */
    private String branchName;

    public Branch() {
        branchHead = null;
        branchName = null;
    }

    public String getBranchName() {
        return branchName;
    }

    public String getBranchHead() {
        return branchHead;
    }

    public void setBranchName(String name) {
        branchName = name;
    }

    public void setBranchHead(String head) {
        branchHead = head;
    }
}
