package SVM;

public enum SvmType { 
    /// <summary>
    /// C-SVC.
    /// </summary>
    C_SVC(0), 
    /// <summary>
    /// nu-SVC.
    /// </summary>
    NU_SVC(1), 
    /// <summary>
    /// one-class SVM
    /// </summary>
    ONE_CLASS(2), 
    /// <summary>
    /// epsilon-SVR
    /// </summary>
    EPSILON_SVR(3), 
    /// <summary>
    /// nu-SVR
    /// </summary>
    NU_SVR(4);
    
    private final int value;
    private SvmType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
};
